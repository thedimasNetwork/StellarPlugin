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
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import stellar.Const;
import stellar.ThedimasPlugin;
import stellar.util.logger.DiscordLogger;

import static mindustry.Vars.world;

public class AdminCommands {
    public static void load(CommandHandler handler) {
        ThedimasPlugin plugin = (ThedimasPlugin) Vars.mods.getMod(Const.PLUGIN_NAME).main;
        
        handler.removeCommand("a");
        handler.<Player>register("a", "<text...>", "commands.admin.a.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                Log.info("@ попытался отправить сообщение админам", Strings.stripColors(player.name));
                return;
            }

            String message = args[0];
            Groups.player.each(Player::admin, otherPlayer -> {
                String msg = plugin.translateChat(player, otherPlayer, message);
                otherPlayer.sendMessage("<[scarlet]A[]>" + msg);
            });

            Log.info("<A>" + Const.CHAT_LOG_FORMAT, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
        });

        handler.<Player>register("admin", "commands.admin.admin.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
            } else {
                player.admin = !player.admin;
            }
        });

        handler.<Player>register("name", "<name...>", "commands.admin.name.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                player.name(plugin.admins.get(player.uuid()));
                String playerName = player.coloredName();
                ThedimasPlugin.bundled(player, "commands.admin.name.reset", playerName);
            } else {
                player.name(args[0]);
                String playerName = player.coloredName();
                ThedimasPlugin.bundled(player, "commands.admin.name.update", playerName);
            }
        });

        handler.<Player>register("tp", "<x> <y>", "commands.admin.tp.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }
            if (!Strings.canParseFloat(args[0]) || !Strings.canParseFloat(args[1])) {
                ThedimasPlugin.bundled(player, "commands.incorrect-format.number");
                return;
            }

            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);

            if (x > world.width() || x < 0 || y > world.height() || y < 0) {
                ThedimasPlugin.bundled(player, "commands.admin.tp.out-of-map");
                return;
            }

            Tile tile = world.tileWorld(x * 8, y * 8);
            if (!player.unit().isFlying() && (tile.solid() || tile.isDarkened())) {
                ThedimasPlugin.bundled(player, "commands.admin.tp.in-block");
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

        handler.<Player>register("spawn", "<unit> [count] [team]", "commands.admin.unit.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                ThedimasPlugin.bundled(player, "commands.admin.unit.notfound", Const.UNIT_LIST);
                return;
            }

            int count = args.length > 1 && Strings.canParseInt(args[1]) ? Strings.parseInt(args[1]) : 1;
            if (count > 24) {
                ThedimasPlugin.bundled(player, "commands.admin.unit.under-limit");
                return;
            } else if (count < 1) {
                ThedimasPlugin.bundled(player, "commands.admin.unit.negative-count");
                return;
            }

            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if (team == null) {
                ThedimasPlugin.bundled(player, "commands.admin.unit.team-notfound", Const.TEAM_LIST);
                return;
            }

            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }

            ThedimasPlugin.bundled(player, "commands.admin.unit.text", count, unit, team.color, team);
            DiscordLogger.info(String.format("%s заспавнил %d %s для команды %s", player.name, count, unit.name, team));
        });

        handler.<Player>register("team", "<team> [username...]", "commands.admin.team.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                ThedimasPlugin.bundled(player, "commands.admin.team.notfound", Const.TEAM_LIST);
                return;
            }

            if (args.length == 1) {
                player.team(team);
                ThedimasPlugin.bundled(player, "commands.admin.team.changed", team.color, team);
            } else {
                Player otherPlayer = ThedimasPlugin.findPlayer(args[1]);
                if (otherPlayer != null) {
                    otherPlayer.team(team);
                    ThedimasPlugin.bundled(otherPlayer, "commands.admin.team.updated", team.color, team);

                    String otherPlayerName = otherPlayer.coloredName();
                    ThedimasPlugin.bundled(player, "commands.admin.team.successful-updated", otherPlayer.name, team.color, team);
                } else {
                    ThedimasPlugin.bundled(player, "commands.admin.team.player-notfound");
                }
            }
        });

        handler.<Player>register("kill", "[username...]", "commands.admin.kill.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                player.unit().kill();
                ThedimasPlugin.bundled(player, "commands.admin.kill.suicide");

                Log.info("@ убил сам себя", Strings.stripColors(player.name));
                return;
            }

            Player otherPlayer = ThedimasPlugin.findPlayer(args[0]);
            if (otherPlayer != null) {
                otherPlayer.unit().kill();
                String otherPlayerName = otherPlayer.coloredName();
                ThedimasPlugin.bundled(player, "commands.admin.kill.kill-another", otherPlayerName);

                Log.info("@ убил @", Strings.stripColors(player.name), Strings.stripColors(otherPlayerName));
            } else {
                player.sendMessage("[scarlet]Игрока с таким ником нет на сервере");
            }
        });

        handler.<Player>register("killall", "[team]", "commands.admin.killall.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            if (args.length == 0) {
                Groups.unit.each(Unitc::kill);
                ThedimasPlugin.bundled(player, "commands.admin.killall.text");

                Log.info("@ убил всех...", Strings.stripColors(player.name));
                DiscordLogger.info(String.format("%s убил всех...", player.name));
            } else {
                Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
                if (team == null) {
                    ThedimasPlugin.bundled(player, "commands.admin.killall.team-notfound", Const.TEAM_LIST);
                    return;
                }

                Groups.unit.each(u -> u.team == team, Unitc::kill); // Надеюсь, оно работает
                ThedimasPlugin.bundled(player, "commands.admin.killall.text-teamed", team.color, team);

                Log.info("@ убил всех с команды @...", Strings.stripColors(player.name), team);
                DiscordLogger.info(String.format("%s убил всех с команды %s...", player.name, team));
            }
        });

        handler.<Player>register("core", "<small|medium|big>", "commands.admin.core.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            Block core;
            switch (args[0].toLowerCase()) {
                case "small" -> core = Blocks.coreShard;
                case "medium" -> core = Blocks.coreFoundation;
                case "big" -> core = Blocks.coreNucleus;
                default -> {
                    ThedimasPlugin.bundled("commands.admin.core.core-type-not-found");
                    return;
                }
            }

            Tile tile = player.tileOn();
            Call.constructFinish(tile, core, player.unit(), (byte) 0, player.team(), false);

            ThedimasPlugin.bundled(player, tile.block() == core ? "commands.admin.core.success" : "commands.admin.core.failed");

            Log.info("@ заспавнил ядро (@, @)", Strings.stripColors(player.name), tile.x, tile.y);
        });

        handler.<Player>register("pause", "commands.admin.pause.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }
            Vars.state.serverPaused = !Vars.state.serverPaused;
            Log.info("@ поставил игру на паузу", Strings.stripColors(player.name));
        });

        handler.<Player>register("end", "commands.admin.end.description", (args, player) -> {
            if (!plugin.admins.containsKey(player.uuid())) {
                ThedimasPlugin.bundled(player, "commands.access-denied");
                return;
            }

            Events.fire(new EventType.GameOverEvent(Team.crux));
            Log.info("@ сменил карту", Strings.stripColors(player.name));
            DiscordLogger.info(String.format("%s сменил карту", player.name));
        });
    }
}
