package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

import java.io.IOException;

import static mindustry.Vars.netServer;

public class ThedimasPlugin extends Plugin {

    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");
        Events.on(EventType.PlayerJoin.class, event -> { // called when player join
            Log.info(event.player.name + " has joined the server");
            Log.info("\tLocale: " + event.player.locale);
            Log.info("\tIP: " + event.player.con.address);
            Call.sendMessage("[lime]+ [accent]" + event.player.name + "[lime] присоединился");
            if (event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, Const.WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, Const.WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, Const.WELCOME_EN);
            }
            netServer.admins.addChatFilter((player, text) -> null);
        });
        Events.on(EventType.PlayerLeave.class, event -> { // called when player leave
            Call.sendMessage("[scarlet]- [accent]" + event.player.name + "[scarlet] вышел");
            Log.info(event.player.name + " has disconnected from the server");
        });
        Events.on(EventType.PlayerChatEvent.class, event -> {
            Log.info("%0: %1 | %2".replace("%0", event.player.name)
                    .replace("%1", event.message)
                    .replace("%2", event.player.locale));
            String prefix = event.player.admin() ? "\uE82C" : "\uE872";
            String type = event.message.split("\\s")[0];
            Groups.player.each(player -> {
                String translated = event.message;
                try {
                    translated = Translator.translate(event.message, player.locale, "auto");
                } catch (IOException e) {
                    Log.info(e.getMessage());
                } finally {
                    String msg = Const.FORMAT.replace("%0", prefix)
                            .replace("%1", event.player.name)
                            .replace("%2", translated);
                    if (type.equals("/t")) {
                        if (player.team() == event.player.team()) {
                            player.sendMessage(msg);
                        }
                    } else if (type.equals("/a")) {
                        if (player.admin()) {
                            player.sendMessage(msg);
                        }
                    } else {
                        player.sendMessage(msg);
                    }
                }
            });
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("rules", "Посмотреть список правил.", (args, player) -> {
            if (player.locale.startsWith("uk")) {
                player.sendMessage(Const.RULES_UK);
            } else if (player.locale.startsWith("ru")) {
                player.sendMessage(Const.RULES_RU);
            } else {
                player.sendMessage(Const.RULES_EN);
            }
        });
        handler.<Player>register("hub", "Подключиться к Хабу.", (args, player) -> Call.connect(player.con, "95.217.226.152", 26160));
        handler.<Player>register("discord", "Получить ссылку на Discord cервер.", (args, player) -> player.sendMessage("https://discord.gg/RkbFYXFU9E"));
        handler.<Player>register("whisper", "<player> <text...>", "Нашептать текст другому игроку.", (args, player) -> {
            //find player by name
            Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
            //give error message with scarlet-colored text if player isn't found
            if (other == null) {
                player.sendMessage("[scarlet]No player by that name found!");
                return;
            }
            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            if (other.locale.startsWith("uk")) {
                other.sendMessage("[lightgray](шепіт) " + player.name + ":[] " + args[1]);
            } else if (other.locale.startsWith("ru")) {
                other.sendMessage("[lightgray](шепот) " + player.name + ":[] " + args[1]);
            } else {
                other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1]);
            }
        });
    }
}
