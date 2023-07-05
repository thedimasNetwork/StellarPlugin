package stellar.bot;

import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.Strings;
import com.sun.management.OperatingSystemMXBean;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.net.Packets;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.BansRecord;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.util.Players;
import stellar.util.StringUtils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

import static stellar.Variables.config;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!(event.getChannel().getIdLong() == config.bot.channelId)) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        // \uE80D
        String name = event.getAuthor().getName();
        String message = event.getMessage().getContentStripped();
        String format = "<[blue]\uE80D[]> %s: %s";
        Call.sendMessage(String.format(format, name, message));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getChannel().getIdLong() != config.bot.channelId) {
            // сообщение отсылаться не будет, ибо таких ботов будет запущено ~10
            return;
        }
        Log.info("Got @ command from @", event.getName(), event.getMember().getUser().getName());
        switch (event.getName()) { // TODO: use command handler
            case "info" -> {
                String text = String.format("""
                        Карта: **%s** | Волна: **%s**
                        Игроков: **%s** | Юнитов: **%s**
                        TPS **%s** | ОЗУ **%sМБ**""", Vars.state.map.name(), Vars.state.wave, Groups.player.size(), Groups.unit.size(), Core.graphics.getFramesPerSecond(), Core.app.getJavaHeap() / 1024 / 1024);
                MessageEmbed embed = Util.embedBuilder("Статус сервера", text, Colors.blue, LocalDateTime.now());
                event.replyEmbeds(embed).queue();
            } case "players" -> {
                if (Groups.player.size() > 0) {
                    StringBuilder players = new StringBuilder();
                    Groups.player.each(p -> {
                        players.append(StringUtils.stripColorsAndGlyphs(p.name)).append("\n");
                    });
                    MessageEmbed embed = Util.embedBuilder("Игроки", players.toString(), Colors.blue, LocalDateTime.now());
                    event.replyEmbeds(embed).queue();
                } else {
                    MessageEmbed embed = Util.embedBuilder("Никого нет", Colors.blue);
                    event.replyEmbeds(embed).queue();
                }
            } case "host" -> {
                OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                File file = new File("/");

                long ramTotal = bean.getTotalMemorySize() / 1024 / 1024;
                long ramUsage = ramTotal - bean.getFreeMemorySize() / 1024 / 1024;
                int ramLoad = (int) (100.0 * ramUsage / ramTotal);
                long diskTotal = file.getTotalSpace() / 1024 / 1024 / 1024;
                long diskUsage = diskTotal - file.getUsableSpace() / 1024 / 1024 / 1024;
                int diskLoad = (int) (100.0 * diskUsage / diskTotal);

                DoubleSummaryStatistics s = new DoubleSummaryStatistics();
                for (int i = 0; i < 100; i++) {
                    s.accept(bean.getCpuLoad() * 100);
                }
                int cpuLoad = (int) s.getAverage();

                String text = String.format("""
                        ЦП: **%s%%**
                        ОЗУ: **%s/%s**МБ (**%s%%**)
                        Диск: **%s/%s**ГБ (**%s%%**)
                        """, cpuLoad, ramUsage, ramTotal, ramLoad, diskUsage, diskTotal, diskLoad);
                MessageEmbed embed = Util.embedBuilder("Нагрузка на хост", text, Colors.blue, LocalDateTime.now());
                event.replyEmbeds(embed).queue();
            } case "maps" -> {
                StringBuilder text = new StringBuilder();
                for (Map map : Vars.maps.customMaps()) {
                    text.append(map.name()).append("\n");
                }
                MessageEmbed embed = Util.embedBuilder("Карты на сервере", text.toString(), Colors.blue, LocalDateTime.now());
                event.replyEmbeds(embed).queue();
            } case "skipwave" -> {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }
                Vars.logic.skipWave();
                String text = String.format("Волна пропущена. Текущая волна %s на карте %s", Vars.state.wave, Vars.state.map.name());
                MessageEmbed embed = Util.embedBuilder(text, Colors.green);
                event.replyEmbeds(embed).queue();
            } case "gameover" -> {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }
                Events.fire(new EventType.GameOverEvent(Team.crux));
                String text = String.format("Игра окончена. Всего волн: %s на карте %s", Vars.state.wave, Vars.state.map.name());
                MessageEmbed embed = Util.embedBuilder(text, Colors.blue);
                event.replyEmbeds(embed).queue();
            } case "find" -> {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }

                String query = event.getOption("query").getAsString();
                try {
                    switch (event.getOption("type").getAsString()) {
                        case "uuid" -> {
                            UsersRecord record = Database.getPlayer(query);
                            MessageEmbed embed;
                            if (record == null) {
                                embed = Util.embedBuilder("Игрок не найден", Colors.red);
                                event.replyEmbeds(embed).queue();
                                return;
                            }
                            String message = String.format("""
                                    UUID: `%s`
                                    Имя: %s
                                    Айди: %s
                                    Последний айпи: %s
                                    Администратор: %s
                                    """, record.getUuid(), record.getName(), record.getId(), record.getIp(), record.getAdmin() == 1);
                            embed = Util.embedBuilder("Информация", message, Colors.purple);
                            event.replyEmbeds(embed).queue();
                        } case "name" -> {
                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < query.length(); i++) {
                                builder.append("%").append(query.charAt(i));
                            }
                            builder.append("%");

                            UsersRecord[] records = Database.getContext()
                                    .selectFrom(Tables.USERS)
                                    .where(Tables.USERS.NAME.likeIgnoreCase(builder.toString()))
                                    .limit(20)
                                    .fetchArray();

                            if (records.length == 0) {
                                MessageEmbed embed = Util.embedBuilder("Игрок не найден", Colors.red);
                                event.replyEmbeds(embed).queue();
                                return;
                            }

                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setTitle("Результат запроса")
                                    .setColor(Colors.purple);

                            for (UsersRecord record : Arrays.copyOfRange(records, 0, Math.min(records.length, 15))) {
                                String message = String.format("""
                                        UUID: `%s`
                                        Имя: %s
                                        Айди: %s
                                        Последний айпи: %s
                                        Администратор: %s
                                        """, record.getUuid(), record.getName(), record.getId(), record.getIp(), record.getAdmin() == 1);
                                embedBuilder.addField(StringUtils.stripColorsAndGlyphs(record.getName()), message, false);
                            }

                            if (records.length > 15) {
                                embedBuilder.setFooter("Найдено больше 15 ползователей. Пожалуйста задайте запрос конкретнее");
                            }

                            event.replyEmbeds(embedBuilder.build()).queue();
                        } case "id" -> {
                            if (!Strings.canParseInt("query")) {
                                MessageEmbed embed = Util.embedBuilder("Невалидный айди", Colors.red);
                                event.replyEmbeds(embed).queue();
                                return;
                            }

                            int id = Integer.parseInt(query);
                            UsersRecord record = Database.getPlayer(id);
                            MessageEmbed embed;
                            if (record == null) {
                                embed = Util.embedBuilder("Игрок не найден", Colors.red);
                            } else {
                                String message = String.format("""
                                        UUID: `%s`
                                        Имя: %s
                                        Айди: %s
                                        Последний айпи: %s
                                        Администратор: %s
                                        """, record.getUuid(), record.getName(), record.getId(), record.getIp(), record.getAdmin() == 1);
                                embed = Util.embedBuilder("Информация", message, Colors.purple);
                            }

                            event.replyEmbeds(embed).queue();
                        } case "ip" -> {
                            UsersRecord[] records = Database.getContext()
                                    .selectFrom(Tables.USERS)
                                    .where(Tables.USERS.IP.contains(query))
                                    .limit(20)
                                    .fetchArray();

                            if (records.length == 0) {
                                MessageEmbed embed = Util.embedBuilder("Ничего не найдено", Colors.red);
                                event.replyEmbeds(embed).queue();
                                return;
                            }

                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setTitle("Результат запроса")
                                    .setColor(Colors.purple);

                            for (UsersRecord record : Arrays.copyOfRange(records, 0, Math.min(records.length, 15))) {
                                String message = String.format("""
                                        UUID: `%s`
                                        Имя: %s
                                        Айди: %s
                                        Последний айпи: %s
                                        Администратор: %s
                                        """, record.getUuid(), record.getName(), record.getId(), record.getIp(), record.getAdmin() == 1);
                                embedBuilder.addField(StringUtils.stripColorsAndGlyphs(record.getName()), message, false);
                            }

                            if (records.length > 15) { // TODO: pages
                                embedBuilder.setFooter("Найдено больше 15 ползователей. Пожалуйста задайте запрос конкретнее");
                            }

                            event.replyEmbeds(embedBuilder.build()).queue();
                        }
                    }
                } catch (SQLException e) {
                    Log.err(e);
                    event.replyEmbeds(Util.embedBuilder("Возникла ошибка", Colors.red)).setEphemeral(true).queue();
                }
            } case "ban" -> {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }

                String query = event.getOption("query").getAsString();
                String reason = event.getOption("reason").getAsString();
                int period = event.getOption("period") == null ? -1 : event.getOption("period").getAsInt();
                Log.debug("@ - @", event.getOption("type").getAsString(), query);
                try {
                    switch (event.getOption("type").getAsString()) {
                        case "uuid" -> {
                            Player player = Players.getPlayer(query);
                            UsersRecord record = Database.getPlayer(query);
                            if (player != null) {
                                BansRecord bansRecord = Database.getContext().newRecord(Tables.BANS);
                                bansRecord.setAdmin(event.getUser().getName());
                                bansRecord.setTarget(query);
                                bansRecord.setCreated(LocalDateTime.now());
                                if (period > -1) { bansRecord.setUntil(LocalDateTime.now().plusDays(period)); }
                                bansRecord.setReason(reason);
                                bansRecord.store();

                                String message = """
                                **Админ**: %admin% (%aid%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                """.replace("%admin%", event.getUser().getName()).replace("%aid%", event.getUser().getId())
                                        .replace("%target%", player.name).replace("%tid%", record.getId().toString())
                                        .replace("%reason%", reason);
                                if (period > -1) {
                                    message += "**Срок**: <t:%timestamp%:f>".replace("%timestamp%", (System.currentTimeMillis() / 1000 + period * (24 * 60 * 60)) + "");
                                } else {
                                    message += "**Срок**: Перманентный";
                                }
                                MessageEmbed embed = Util.embedBuilder("Бан (через Discord)", message, Colors.red);
                                Bot.sendEmbed(config.bot.bansId, embed);
                                player.kick(Packets.KickReason.banned);
                            } else {
                                if (record == null) {
                                    MessageEmbed embed = Util.embedBuilder("Игрок не найден", Colors.red);
                                    event.replyEmbeds(embed).setEphemeral(true).queue();
                                    return;
                                }

                                BansRecord bansRecord = Database.getContext().newRecord(Tables.BANS);
                                bansRecord.setAdmin(event.getUser().getName());
                                bansRecord.setTarget(query);
                                bansRecord.setCreated(LocalDateTime.now());
                                if (period > -1) { bansRecord.setUntil(LocalDateTime.now().plusDays(period)); }
                                bansRecord.setReason(reason);
                                bansRecord.store();

                                String message = """
                                **Админ**: <@%aid%> (%admin%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                """.replace("%aid%", event.getUser().getId()).replace("%admin%", event.getUser().getName())
                                        .replace("%target%", record.getName()).replace("%tid%", record.getId().toString())
                                        .replace("%reason%", reason);
                                if (period > -1) {
                                    message += "**Срок**: <t:%timestamp%:f>".replace("%timestamp%", (System.currentTimeMillis() / 1000 + period * (24 * 60 * 60)) + "");
                                } else {
                                    message += "**Срок**: Перманентный";
                                }
                                MessageEmbed embed = Util.embedBuilder("Бан", message, Colors.red);
                                Bot.sendEmbed(config.bot.bansId, embed);
                            }
                            MessageEmbed embed = Util.embedBuilder("Игрок забанен", Colors.green);
                            event.replyEmbeds(embed).setEphemeral(true).queue();
                        }
                        case "name" -> {
                            Player player = Players.findPlayer(query);
                            if (player == null) {
                                MessageEmbed embed = Util.embedBuilder("Игрок не найден", Colors.red);
                                event.replyEmbeds(embed).setEphemeral(true).queue();
                                return;
                            }

                            UsersRecord record = Database.getPlayer(player.uuid());
                            BansRecord bansRecord = Database.getContext().newRecord(Tables.BANS);
                            bansRecord.setAdmin(event.getUser().getName());
                            bansRecord.setTarget(player.uuid());
                            bansRecord.setCreated(LocalDateTime.now());
                            if (period > -1) { bansRecord.setUntil(LocalDateTime.now().plusDays(period)); }
                            bansRecord.setReason(reason);
                            bansRecord.store();

                            String message = """
                                **Админ**: <@%aid%> (%admin%)
                                **Нарушитель**: %target% (%tid%)
                                **Причина**: %reason%
                                """.replace("%aid%", event.getUser().getId()).replace("%admin%", event.getUser().getName())
                                    .replace("%target%", player.name).replace("%tid%", record.getId().toString())
                                    .replace("%reason%", reason);
                            if (period > -1) {
                                message += "**Срок**: <t:%timestamp%:f>".replace("%timestamp%", (System.currentTimeMillis() / 1000 + period * (24 * 60 * 60)) + "");
                            } else {
                                message += "**Срок**: Перманентный";
                            }

                            MessageEmbed embed = Util.embedBuilder("Игрок забанен", Colors.green);
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            MessageEmbed banEmbed = Util.embedBuilder("Бан (через Discord)", message, Colors.red);
                            Bot.sendEmbed(config.bot.bansId, banEmbed);
                            player.kick(Packets.KickReason.banned);
                        }
                        case "id" -> {

                        }
                    }
                } catch (SQLException e) {
                    Log.err(e);
                    event.replyEmbeds(Util.embedBuilder("Возникла ошибка", Colors.red)).setEphemeral(true).queue();
                }
            }
        }
    }
}
