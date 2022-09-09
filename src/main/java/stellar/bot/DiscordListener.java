package stellar.bot;

import arc.Core;
import arc.math.Mathf;
import arc.util.Log;
import com.sun.management.OperatingSystemMXBean;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.maps.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import stellar.ThedimasPlugin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;

import static stellar.Variables.config;
import static mindustry.Vars.*;

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
        switch (event.getName()) {
            case "info" -> {
                String text = String.format("""
                    Карта: **%s** | Волна: **%s**
                    Игроков: **%s** | Юнитов: **%s**
                    TPS **%s** | ОЗУ **%sМБ**""", state.map.name(), state.wave, Groups.player.size(), Groups.unit.size(), Core.graphics.getFramesPerSecond(), Core.app.getJavaHeap() / 1024 / 1024);
                MessageEmbed embed = new EmbedBuilder()
                        .addField("Статус сервера", text, false)
                        .setColor(Colors.blue)
                        .setTimestamp(LocalDateTime.now())
                        .build();
                event.replyEmbeds(embed).queue();
            }
            case "players" -> {
                if (Groups.player.size() > 0) {
                    StringBuilder players = new StringBuilder();
                    Groups.player.each(p -> {
                        players.append(ThedimasPlugin.stripColorsAndGlyphs(p.name)).append("\n");
                    });
                    MessageEmbed embed = new EmbedBuilder()
                            .addField("Игроки", players.toString(), false)
                            .setColor(Colors.blue)
                            .setTimestamp(LocalDateTime.now())
                            .build();
                    event.replyEmbeds(embed).queue();
                }
                else {
                    MessageEmbed embed = new EmbedBuilder()
                            .setDescription("Никого нет")
                            .setColor(Colors.blue)
                            .setTimestamp(LocalDateTime.now())
                            .build();
                    event.replyEmbeds(embed).queue();
                }
            }
            case "host" -> {
                OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                File file = new File("/");

                long ramTotal = bean.getTotalMemorySize() / 1024 / 1024;
                long ramUsage = ramTotal - bean.getFreeMemorySize() / 1024 / 1024;
                int ramLoad = (int)(100.0 * ramUsage / ramTotal);
                long diskTotal = file.getTotalSpace() / 1024 / 1024 / 1024;
                long diskUsage = diskTotal - file.getUsableSpace() / 1024 / 1024 / 1024;
                int diskLoad = (int)(100.0 * diskUsage / diskTotal);

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
                MessageEmbed embed = new EmbedBuilder()
                        .addField("Нагрузка на сервер", text, false)
                        .setColor(Colors.blue)
                        .setTimestamp(LocalDateTime.now())
                        .build();
                event.replyEmbeds(embed).queue();
            }
            case "maps" -> {
                StringBuilder text = new StringBuilder();
                for (Map map : maps.customMaps()) {
                    text.append(map.name()).append("\n");
                }
                MessageEmbed embed = new EmbedBuilder()
                        .addField("Карты на сервере", text.toString(), false)
                        .setColor(Colors.blue)
                        .setTimestamp(LocalDateTime.now())
                        .build();
                event.replyEmbeds(embed).queue();
            }
        }
    }
}
