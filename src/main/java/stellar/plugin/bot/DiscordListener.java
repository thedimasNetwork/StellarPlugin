package stellar.plugin.bot;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
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
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jooq.Record1;
import stellar.plugin.database.Database;
import stellar.plugin.database.gen.Tables;
import stellar.plugin.database.gen.tables.records.UsersRecord;
import stellar.plugin.util.Players;
import stellar.plugin.util.StringUtils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;

import static stellar.plugin.Variables.config;

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
            }
            case "players" -> {
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
            }
            case "host" -> {
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
            }
            case "maps" -> {
                StringBuilder text = new StringBuilder();
                for (Map map : Vars.maps.customMaps()) {
                    text.append(map.name()).append("\n");
                }
                MessageEmbed embed = Util.embedBuilder("Карты на сервере", text.toString(), Colors.blue, LocalDateTime.now());
                event.replyEmbeds(embed).queue();
            }
            case "skipwave" -> {
                if (!Util.isMindustryAdmin(event.getMember())) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }
                Vars.logic.skipWave();
                String text = String.format("Волна пропущена. Текущая волна %s на карте %s", Vars.state.wave, Vars.state.map.name());
                MessageEmbed embed = Util.embedBuilder(text, Colors.green);
                event.replyEmbeds(embed).queue();
            }
            case "gameover" -> {
                if (!Util.isMindustryAdmin(event.getMember())) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }
                Events.fire(new EventType.GameOverEvent(Team.crux));
                String text = String.format("Игра окончена. Всего волн: %s на карте %s", Vars.state.wave, Vars.state.map.name());
                MessageEmbed embed = Util.embedBuilder(text, Colors.blue);
                event.replyEmbeds(embed).queue();
            }
            case "find" -> {
                if (!Util.isMindustryAdmin(event.getMember())) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }

                String query = event.getOption("query").getAsString();
                try {
                    Seq<UsersRecord> records = new Seq<>();
                    Integer count = 0;
                    switch (event.getOption("type").getAsString()) {
                        case "uuid" -> {
                            UsersRecord record = Database.getPlayer(query);
                            records = record != null ? Seq.with(record) : new Seq<>();
                            count = records.size;
                        }
                        case "name" -> {
                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < query.length(); i++) {
                                builder.append("%").append(query.charAt(i));
                            }
                            builder.append("%");
                            records = Seq.with(Database.getContext()
                                    .selectFrom(Tables.USERS)
                                    .where(Tables.USERS.NAME.likeIgnoreCase(builder.toString()))
                                    .limit(15)
                                    .fetchArray());
                            Record1<Integer> record1 = Database.getContext()
                                    .selectCount()
                                    .from(Tables.USERS)
                                    .where(Tables.USERS.NAME.likeIgnoreCase(builder.toString()))
                                    .limit(15)
                                    .fetchOne();
                            count = record1 == null ? 0 : record1.value1();
                        }
                        case "id" -> {
                            if (!Strings.canParseInt(query)) {
                                MessageEmbed embed = Util.embedBuilder("Невалидный айди", Colors.red);
                                event.replyEmbeds(embed).setEphemeral(true).queue();
                                return;
                            }
                            UsersRecord record = Database.getPlayer(Strings.parseInt(query));
                            records = record != null ? Seq.with(record) : new Seq<>();
                            count = records.size;
                        }
                        case "ip" -> {
                            records = Seq.with(Database.getContext()
                                    .selectFrom(Tables.USERS)
                                    .where(Tables.USERS.IP.contains(query))
                                    .limit(15)
                                    .fetchArray());
                            Record1<Integer> record1 = Database.getContext()
                                    .selectCount()
                                    .from(Tables.USERS)
                                    .where(Tables.USERS.IP.contains(query))
                                    .limit(15)
                                    .fetchOne();
                            count = record1 == null ? 0 : record1.value1();

                        }
                    }

                    if (count == 0) {
                        MessageEmbed embed = Util.embedBuilder("Ничего не найдено", Colors.red);
                        event.replyEmbeds(embed).setEphemeral(true).queue();
                        return;
                    }

                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle(String.format("Найдено %s записей", count))
                            .setFooter(count > 15 ? "Показано только 15 записей. Задайте запрос конкретнее" : null) // TODO: pages
                            .setColor(Colors.blue);

                    records.each(record -> {
                        String banned = "???";
                        try {
                            banned = StringUtils.fancyBool(Database.isBanned(record.getUuid()));
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                        String message = String.format("""
                                    UUID: `%s`
                                    Имя: %s
                                    Айди: %s
                                    Последний айпи: %s
                                    Администратор: %s
                                    Забанен: %s
                                    """, record.getUuid(), record.getName(), record.getId(), record.getIp(), StringUtils.fancyBool(record.getAdmin() == 1), banned);
                        embedBuilder.addField(Strings.stripColors("**" + record.getName()) + "**", message, false);
                    });
                    event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                } catch (SQLException e) {
                    Log.err(e);
                    event.replyEmbeds(Util.embedBuilder("Возникла ошибка", Colors.red)).setEphemeral(true).queue();
                }
            }
            case "ban" -> {
                if (!Util.isMindustryAdmin(event.getMember())) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }

                String query = event.getOption("query").getAsString();
                String reason = event.getOption("reason").getAsString();
                int period = event.getOption("period") == null ? -1 : event.getOption("period").getAsInt();
                Log.debug("@ - @", event.getOption("type").getAsString(), query);
                try {
                    UsersRecord record = null;
                    Player player = null;
                    switch (event.getOption("type").getAsString()) {
                        case "uuid" -> {
                            record = Database.getPlayer(query);
                            player = Players.getPlayer(query);
                        }
                        case "name" -> {
                            player = Players.findPlayer(query);
                            record = player != null ? Database.getPlayer(player.uuid()) : null;
                        }
                        case "id" -> {
                            if (!Strings.canParseInt(query)) {
                                MessageEmbed embed = Util.embedBuilder("Невалидный айди", Colors.red);
                                event.replyEmbeds(embed).setEphemeral(true).queue();
                                return;
                            }
                            record = Database.getPlayer(Strings.parseInt(query));
                        }
                    }

                    if (record == null) {
                        MessageEmbed embed = Util.embedBuilder("Игрок не найден", Colors.red);
                        event.replyEmbeds(embed).setEphemeral(true).queue();
                        return;
                    }

                    if (Database.isBanned(record.getUuid())) {
                        MessageEmbed embed = Util.embedBuilder("Игрок уже забанен", Colors.red);
                        event.replyEmbeds(embed).setEphemeral(true).queue();
                        return;
                    }

                    if (player != null) {
                        player.kick(Packets.KickReason.banned);
                    }

                    Database.ban(event.getUser().getName(), record.getUuid(), period, reason);
                    MessageEmbed embed = Util.embedBuilder("Игрок забанен", Colors.green);
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    String message = """
                            **Админ**: <@%aid%> (%admin%)
                            **Нарушитель**: %target% (%tid%)
                            **Причина**: %reason%
                            """.replace("%aid%", event.getUser().getId()).replace("%admin%", event.getUser().getName())
                            .replace("%target%", Strings.stripColors(record.getName())).replace("%tid%", record.getId().toString())
                            .replace("%reason%", reason);
                    if (period > -1) {
                        message += "**Срок**: <t:%timestamp%:f>".replace("%timestamp%", (System.currentTimeMillis() / 1000 + period * (24 * 60 * 60)) + "");
                    } else {
                        message += "**Срок**: Перманентный";
                    }
                    MessageEmbed banEmbed = Util.embedBuilder("Бан (через Discord)", message, Colors.red, LocalDateTime.now());
                    Bot.sendEmbed(config.bot.bansId, banEmbed);
                } catch (IllegalArgumentException | SQLException e) {
                    Log.err(e);
                    event.replyEmbeds(Util.embedBuilder("Возникла ошибка", Colors.red)).setEphemeral(true).queue();
                }
            }
            case "unban" -> {
                if (!Util.isMindustryAdmin(event.getMember())) {
                    MessageEmbed embed = Util.embedBuilder("В доступе отказано", Colors.red);
                    event.replyEmbeds(embed).queue();
                    return;
                }

                String query = event.getOption("query").getAsString();
                try {
                    UsersRecord record = null;
                    switch (event.getOption("type").getAsString()) {
                        case "id" -> {
                            if (!Strings.canParseInt(query)) {
                                MessageEmbed embed = Util.embedBuilder("Невалидный айди", Colors.red);
                                event.replyEmbeds(embed).setEphemeral(true).queue();
                                return;
                            }
                            record = Database.getPlayer(Strings.parseInt(query));
                        }
                        case "uuid" -> {
                            record = Database.getPlayer(query);
                        }
                    }

                    if (record == null) {
                        MessageEmbed embed = Util.embedBuilder("Игрок не найден", Colors.red);
                        event.replyEmbeds(embed).setEphemeral(true).queue();
                        return;
                    }

                    if (!Database.isBanned(record.getUuid())) {
                        MessageEmbed embed = Util.embedBuilder("Игрок не забанен", Colors.red);
                        event.replyEmbeds(embed).setEphemeral(true).queue();
                        return;
                    }


                    Database.unban(record.getUuid());
                    MessageEmbed embed = Util.embedBuilder("Игрок разбанен", Colors.green);
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    String message = """
                            **Админ**: <@%aid%> (%admin%)
                            **Нарушитель**: %target% (%tid%)
                            """.replace("%aid%", event.getUser().getId()).replace("%admin%", event.getUser().getName())
                            .replace("%target%", Strings.stripColors(record.getName())).replace("%tid%", record.getId().toString());
                    MessageEmbed unbanEmbed = Util.embedBuilder("Разбан (через Discord)", message, Colors.green, LocalDateTime.now());
                    Bot.sendEmbed(config.bot.bansId, unbanEmbed);
                } catch (IllegalArgumentException | SQLException e) {
                    Log.err(e);
                    event.replyEmbeds(Util.embedBuilder("Возникла ошибка", Colors.red)).setEphemeral(true).queue();
                }
            }
        }
    }
}
