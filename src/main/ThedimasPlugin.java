package main;

import arc.*;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;
import mindustry.type.UnitType;

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
            String originalMsg;
            if (type.equals("/t") || type.equals("/a")) {
                originalMsg = event.message.substring(event.message.indexOf(" ") + 1);
            } else {
                originalMsg = event.message;
            }
            Groups.player.each(player -> {
                String translated = originalMsg;
                try {
                    translated = Translator.translate(originalMsg, player.locale, "auto");
                } catch (IOException e) {
                    Log.info(e.getMessage());
                } finally {
                    String msg = Const.FORMAT.replace("%0", prefix)
                            .replace("%1", event.player.name)
                            .replace("%2", translated);
                    if (type.equals("/t")) {
                        if (player.team() == event.player.team()) {
                            String color = player.team().color.toString();
                            player.sendMessage("[" + color.substring(0, color.length() - 2) + "]" + "<T>[]" + msg);
                        }
                    } else if (type.equals("/a")) {
                        if (player.admin()) {
                            player.sendMessage("[scarlet]<A>[]" + msg);
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
        handler.<Player>register("spawn", "<unit> <count> <team>", "Заспавнить юнитов", (args, player) -> {
            if (!player.admin) {
                player.sendMessage("[red]Только админы могут использовать эту команду!");
                return;
            }
            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                player.sendMessage("[red]Юнит не найден! Доступные юниты:\n\n" + Const.UNIT_LIST + "\n");
                return;
            }
            int count;
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("[red]Неверный формат числа!");
                return;
            }
            if (count > 24) {
                player.sendMessage("[red]Нельзя заспавнить больше 24 юнитов!");
                return;
            }
            Team team;
            switch (args[2]) {
                case "sharded":
                    team = Team.sharded;
                    break;
                case "blue":
                    team = Team.blue;
                    break;
                case "crux":
                    team = Team.crux;
                    break;
                case "derelict":
                    team = Team.derelict;
                    break;
                case "green":
                    team = Team.green;
                    break;
                case "purple":
                    team = Team.purple;
                    break;
                default:
                    player.sendMessage("[red]Неверная команда. Возможные варианты:\n\n" + Const.TEAM_LIST + "\n");
                    return;
            }
            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }
            player.sendMessage("[green]Ты заспавнил " + "[accent]" + count + " " + unit + " " + "[green]для команды" + " " + "[accent]" + team);
        });
        handler.<Player>register("team", "<team>", "Изменить команду", (args, player) -> {
            if (!player.admin()) {
                player.sendMessage("[red]Только админы могут использовать эту команду![]");
                return;
            }
            Team team;
            switch (args[0]) {
                case "sharded":
                    team = Team.sharded;
                    break;
                case "blue":
                    team = Team.blue;
                    break;
                case "crux":
                    team = Team.crux;
                    break;
                case "derelict":
                    team = Team.derelict;
                    break;
                case "green":
                    team = Team.green;
                    break;
                case "purple":
                    team = Team.purple;
                    break;
                default:
                    player.sendMessage("[red]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                    return;
            }
            player.team(team);
            player.sendMessage("Команда изменена. Новая команда - " + team);
        });
    }
}
