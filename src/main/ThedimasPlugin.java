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
            String prefix = event.player.admin() ? "\uE82C" : "\uE872";
            Groups.player.each(player -> {
                String translated = event.message;
                if (!event.message.startsWith("/")) {
                    try {
                        if (!player.locale.equals(event.player.locale())) {
                            translated = Translator.translate(event.message, player.locale, "auto");
                        }
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String msg = Const.FORMAT.replace("%0", prefix)
                                                 .replace("%1", event.player.name)
                                                 .replace("%2", translated);
                        player.sendMessage(msg);
                    }
                }
            });
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("a");
        handler.removeCommand("t");
        handler.<Player>register("a", "<text...>", "Отправить сообщение администрации.", (args, player) -> {
            String message = args[0];
            if (player.admin()) {
                Groups.player.each(otherPlayer -> {
                    if (otherPlayer.admin()) {
                        String translated = message;
                        try {
                            translated = Translator.translate(message, otherPlayer.locale, "auto");
                        } catch (IOException e) {
                            Log.err(e.getMessage());
                        } finally {
                            String prefix = player.admin() ? "\uE82C" : "\uE872";
                            String msg = Const.FORMAT.replace("%0", prefix)
                                                     .replace("%1", player.name)
                                                     .replace("%2", translated);
                            otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
                        }
                    }
                });
            } else {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
            }
        });
        handler.<Player>register("t", "<text...>", "Отправить сообщение команде.", (args, player) -> {
            String message = args[0];
            Groups.player.each(player1 -> {
                if (player1.team() == player.team()) {
                    String translated = message;
                    try {
                        translated = Translator.translate(message, player1.locale, "auto");
                    } catch (IOException e) {
                        Log.err(e.getMessage());
                    } finally {
                        String prefix = player.admin() ? "\uE82C" : "\uE872";
                        String msg = Const.FORMAT.replace("%0", prefix)
                                                 .replace("%1", player.name)
                                                 .replace("%2", translated);
                        player1.sendMessage("<[#" + player.team().color.toString().substring(0, 6) + "]T[]>" + msg);
                    }
                }
            });
        });
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
        handler.<Player>register("connect", "[name]", "Подключиться к другому серверу.", (args, player) -> {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                player.sendMessage("[sky]Список доступных серверов" + Const.SERVER_LIST);
                return;
            }
            if (!Const.SERVER_ADDRESS.containsKey(args[0].toLowerCase())) {
                player.sendMessage("[scarlet]Такого сервера не существует. Доступные сервера:\n" + Const.SERVER_LIST);
                return;
            }
            String address = Const.SERVER_ADDRESS.get(args[0].toLowerCase());
            String ip = address.split(":")[0];
            int port = Integer.parseInt(address.split(":")[1]);
            Call.connect(player.con, ip, port);
        });
        handler.<Player>register("discord", "Получить ссылку на Discord сервер.", (args, player) -> player.sendMessage("https://discord.gg/RkbFYXFU9E"));
        handler.<Player>register("spawn", "<Юнит> [Количество] [Команда]", "Заспавнить юнитов", (args, player) -> {
            if (!player.admin) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду!");
                return;
            }
            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                player.sendMessage("[scarlet]Юнит не найден! Доступные юниты:\n\n" + Const.UNIT_LIST + "\n");
                return;
            }
            int count = (args.length > 1 && Strings.canParseInt(args[1])) ? Strings.parseInt(args[1]) : 1;
            if (count > 24) {
                player.sendMessage("[scarlet]Нельзя заспавнить больше 24 юнитов!");
                return;
            }
            if (count < 1) {
                player.sendMessage("[scarlet]Нельзя заспавнить меньше 1 юнита!");
                return;
            }
            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if (team == null) {
                player.sendMessage("[scarlet]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                return;
            }
            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }
            player.sendMessage("[green]Ты заспавнил " + "[accent]" + count + " " + unit + " " + "[green]для команды " + "[#" + team.color.toString().substring(0, 6) + "]" + team);
        });
        handler.<Player>register("team", "<team>", "Изменить команду", (args, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]Только админы могут использовать эту команду![]");
                return;
            }
            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                player.sendMessage("[scarlet]Неверная команда. Возможные варианты:\n" + Const.TEAM_LIST);
                return;
            }
            player.team(team);
            player.sendMessage("Команда изменена. Новая команда - [#" + team.color.toString().substring(0, 6) + "]" + team);
        });
    }
}
