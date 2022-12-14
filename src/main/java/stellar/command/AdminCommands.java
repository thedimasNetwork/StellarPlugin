package stellar.command;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Packets;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import stellar.Const;
import stellar.Variables;
import stellar.bot.Bot;
import stellar.database.DBHandler;
import stellar.database.entries.PlayerEntry;
import stellar.database.entries.PlayerEventEntry;
import stellar.database.enums.PlayerEventTypes;
import stellar.database.tables.Tables;
import stellar.util.Bundle;
import stellar.util.Players;
import stellar.util.Translator;
import stellar.util.logger.DiscordLogger;

import java.sql.SQLException;

import static mindustry.Vars.mods;
import static mindustry.Vars.world;

public class AdminCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("a");
        commandHandler.<Player>register("a", "<text...>", "commands.admin.a.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                Log.info("@ попытался отправить сообщение админам", Strings.stripColors(player.name));
                return;
            }

            String message = args[0];
            Groups.player.each(Player::admin, otherPlayer -> {
                String msg = Translator.translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
            });

            Log.info("<A>" + Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
        });

        commandHandler.<Player>register("admin", "commands.admin.admin.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
            } else {
                player.admin = !player.admin;
            }
        });

        commandHandler.<Player>register("name", "<name...>", "commands.admin.name.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                player.name(Variables.admins.get(player.uuid()));
                String playerName = player.coloredName();
                Bundle.bundled(player, "commands.admin.name.reset", playerName);
            } else {
                player.name(args[0]);
                String playerName = player.coloredName();
                Bundle.bundled(player, "commands.admin.name.update", playerName);
            }
        });

        commandHandler.<Player>register("tp", "<x> <y>", "commands.admin.tp.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }
            if (!Strings.canParseFloat(args[0]) || !Strings.canParseFloat(args[1])) {
                Bundle.bundled(player, "commands.incorrect-format.number");
                return;
            }

            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);

            if (x > world.width() || x < 0 || y > world.height() || y < 0) {
                Bundle.bundled(player, "commands.admin.tp.out-of-map");
                return;
            }

            Tile tile = world.tileWorld(x * 8, y * 8);
            if (!player.unit().isFlying() && (tile.solid() || tile.isDarkened())) {
                Bundle.bundled(player, "commands.admin.tp.in-block");
                return;
            }

            Unit oldUnit = player.unit();
            UnitType type = player.unit().type;
            float health = oldUnit.health;
            float ammo = oldUnit.ammo;
            boolean spawnedByCore = oldUnit.spawnedByCore;

            player.unit().kill();
            player.unit(type.spawn(player.team(), x * 8, y * 8));
            player.unit().health = health;
            player.unit().ammo = ammo;
            player.unit().spawnedByCore = spawnedByCore;

            player.snapSync();
        });

        commandHandler.<Player>register("spawn", "<unit> [count] [team]", "commands.admin.unit.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                Bundle.bundled(player, "commands.admin.unit.notfound", Const.UNIT_LIST);
                return;
            }

            int count = args.length > 1 && Strings.canParseInt(args[1]) ? Strings.parseInt(args[1]) : 1;
            if (count > 24) {
                Bundle.bundled(player, "commands.admin.unit.under-limit");
                return;
            } else if (count < 1) {
                Bundle.bundled(player, "commands.admin.unit.negative-count");
                return;
            }

            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if (team == null) {
                Bundle.bundled(player, "commands.admin.unit.team-notfound", Const.TEAM_LIST);
                return;
            }

            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }

            Bundle.bundled(player, "commands.admin.unit.text", count, unit, team.color, team);
            DiscordLogger.info(String.format("%s заспавнил %d %s для команды %s", player.name, count, unit.name, team));
        });

        commandHandler.<Player>register("team", "<team> [username...]", "commands.admin.team.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                Bundle.bundled(player, "commands.admin.team.notfound", Const.TEAM_LIST);
                return;
            }

            if (args.length == 1) {
                player.team(team);
                Bundle.bundled(player, "commands.admin.team.changed", team.color, team);
            } else {
                Player otherPlayer = Players.findPlayer(args[1]);
                if (otherPlayer != null) {
                    otherPlayer.team(team);
                    Bundle.bundled(otherPlayer, "commands.admin.team.updated", team.color, team);

                    String otherPlayerName = otherPlayer.coloredName(); // ???
                    Bundle.bundled(player, "commands.admin.team.successful-updated", otherPlayer.name, team.color, team);
                } else {
                    Bundle.bundled(player, "commands.admin.team.player-notfound");
                }
            }
        });

        commandHandler.<Player>register("kill", "[username...]", "commands.admin.kill.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                player.unit().kill();
                Bundle.bundled(player, "commands.admin.kill.suicide");

                Log.info("@ убил сам себя", Strings.stripColors(player.name));
                return;
            }

            Player otherPlayer = Players.findPlayer(args[0]);
            if (otherPlayer != null) {
                otherPlayer.unit().kill();
                String otherPlayerName = otherPlayer.coloredName();
                Bundle.bundled(player, "commands.admin.kill.kill-another", otherPlayerName);

                Log.info("@ убил @", Strings.stripColors(player.name), Strings.stripColors(otherPlayerName));
            } else {
                player.sendMessage("[scarlet]Игрока с таким ником нет на сервере");
            }
        });

        commandHandler.<Player>register("killall", "[team]", "commands.admin.killall.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                Groups.unit.each(Unitc::kill);
                Bundle.bundled(player, "commands.admin.killall.text");

                Log.info("@ убил всех...", Strings.stripColors(player.name));
                DiscordLogger.info(String.format("%s убил всех...", player.name));
            } else {
                Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
                if (team == null) {
                    Bundle.bundled(player, "commands.admin.killall.team-notfound", Const.TEAM_LIST);
                    return;
                }

                Groups.unit.each(u -> u.team == team, Unitc::kill); // Надеюсь, оно работает
                Bundle.bundled(player, "commands.admin.killall.text-teamed", team.color, team);

                Log.info("@ убил всех с команды @...", Strings.stripColors(player.name), team);
                DiscordLogger.info(String.format("%s убил всех с команды %s...", player.name, team));
            }
        });

        commandHandler.<Player>register("core", "<small|medium|big>", "commands.admin.core.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Block core;
            switch (args[0].toLowerCase()) {
                case "small" -> core = Blocks.coreShard;
                case "medium" -> core = Blocks.coreFoundation;
                case "big" -> core = Blocks.coreNucleus;
                default -> {
                    Bundle.bundled("commands.admin.core.core-type-not-found");
                    return;
                }
            }

            Tile tile = player.tileOn();
            Call.constructFinish(tile, core, player.unit(), (byte) 0, player.team(), false);

            Bundle.bundled(player, tile.block() == core ? "commands.admin.core.success" : "commands.admin.core.failed");

            Log.info("@ заспавнил ядро (@, @)", Strings.stripColors(player.name), tile.x, tile.y);
        });

        commandHandler.<Player>register("end", "commands.admin.end.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Events.fire(new EventType.GameOverEvent(Team.crux));
            Log.info("@ сменил карту", Strings.stripColors(player.name));
            DiscordLogger.info(String.format("%s сменил карту", player.name));
        });

        commandHandler.<Player>register("js", "<code...>", "JS eval", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }
            if (!Variables.jsallowed.containsKey(player.uuid())) {
                Bundle.bundled(player, "В доступе к этой команде отказано. Для получения обратись к высшей администрации");
                return;
            }
            String output = mods.getScripts().runConsole(args[0]);
            boolean error = false;
            try {
                String errorName = output.substring(0, output.indexOf(' ') - 1);
                Class.forName("org.mozilla.javascript." + errorName);
            } catch (Exception ignored) {
                error = true;
            }
            player.sendMessage("> " + (error ? "[#ff341c]" + output : output));
        }); // TODO: использовать бандлы

        commandHandler.removeCommand("ban");
        commandHandler.<Player>register("ban", "<name...>", "Забанить игрока", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Player found = Players.findPlayer(args[0]);
            if (found == null) {
                player.sendMessage(String.format("[red]Игрок с ником %s не найден[]", args[0]));
                return;
            }
            String uuid = found.uuid();

            if (Variables.admins.containsValue(uuid)) {
                player.sendMessage("[red]Игрок администратор[]");
                return;
            }

            try {
                DBHandler.update(args[0], Tables.users.getBanned(), Tables.users, true);
                PlayerEventEntry entry = PlayerEventEntry.builder()
                        .server(Const.SERVER_COLUMN_NAME)
                        .timestamp((int) (System.currentTimeMillis() / 1000))
                        .type(PlayerEventTypes.BAN)
                        .name(found.name())
                        .uuid(found.uuid())
                        .ip(found.ip())
                        .build();
                try {
                    DBHandler.save(entry, Tables.playerEvents);
                } catch (SQLException e) {
                    Log.err(e);
                }
                found.kick(Packets.KickReason.banned);
                player.sendMessage(String.format("[lime]Игрок %s забанен[]", args[0]));
                Log.info("@ (@) has banned @ (@)", player.name(), player.uuid(), found.name(), found.uuid());
                Bot.sendMessage(String.format("%s забанил игрока %s", player.name(), found.name()));
            } catch (SQLException e) {
                Log.err("Failed to ban uuid for player '@'", uuid);
                Log.err(e);
                DiscordLogger.err("Failed to ban uuid for player '" + uuid + "'", e);
                player.sendMessage(String.format("[red]Не могу забанить игрока с ником %s[]", args[0]));
            }
        }); // TODO: использовать бандлы

        commandHandler.removeCommand("unban");
        commandHandler.<Player>register("unban", "<uuid>", "Разбанить игрока", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            if (args[0].contains("'") || args[0].contains("\"")) {
                player.sendMessage(String.format("[red]Невалидный айди![]", args[0]));
                return;
            }

            try {
                if (!DBHandler.userExist(args[0])) {
                    player.sendMessage(String.format("[red]Игрок с айди %s не найден[]", args[0]));
                    return;
                }

                DBHandler.update(args[0], Tables.users.getBanned(), Tables.users, false);
                PlayerEntry data = DBHandler.get(args[0], Tables.users, PlayerEntry.class);
                player.sendMessage(String.format("[lime]Игрок с айди %s разбанен[]", args[0]));
                Log.info("@ (@) has unbanned @ (@)", player.name(), player.uuid(), data.getName(), args[0]);
                Bot.sendMessage(String.format("%s разбанил игрока %s", player.name(), data.getName()));
            } catch (SQLException e) {
                Log.err("Failed to unban uuid for player '@'", args[0]);
                Log.err(e);
                DiscordLogger.err("Failed to ban uuid for player '" + args[0] + "'", e);
                player.sendMessage(String.format("[red]Не могу разбанить игрока с айди %s[]", args[0]));
            }
        }); // TODO: использовать бандлы

        commandHandler.removeCommand("kick");
        commandHandler.<Player>register("kick", "<name...>", "Выгнать игрока", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Player found = Players.findPlayer(args[0]);
            if (found == null) {
                player.sendMessage(String.format("[red]Игрок с ником %s не найден[]", args[0]));
                return;
            }

            String uuid = found.uuid();
            if (Variables.admins.containsValue(uuid)) {
                player.sendMessage("[red]Игрок администратор[]");
                return;
            }

            found.kick(Packets.KickReason.kick);
            PlayerEventEntry entry = PlayerEventEntry.builder()
                    .server(Const.SERVER_COLUMN_NAME)
                    .timestamp((int) (System.currentTimeMillis() / 1000))
                    .type(PlayerEventTypes.KICK)
                    .name(found.name())
                    .uuid(found.uuid())
                    .ip(found.ip())
                    .build();
            try {
                DBHandler.save(entry, Tables.playerEvents);
            } catch (SQLException e) {
                Log.err(e);
            }
            player.sendMessage(String.format("[lime]Игрок %s выгнан[]", args[0]));
            Log.info("@ (@) has kicked @ (@)", player.name(), player.uuid(), found.name(), found.uuid());
            Bot.sendMessage(String.format("%s выгнал игрока %s", player.name(), found.name()));
        }); // TODO: использовать бандлы
    }
}
