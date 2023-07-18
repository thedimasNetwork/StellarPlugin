package stellar.plugin.command;

import arc.Events;
import arc.graphics.Color;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Packets;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import rhino.WrappedException;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.bot.Bot;
import stellar.database.Database;
import stellar.database.enums.PlayerEventTypes;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.PlayerEventsRecord;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.Translator;
import stellar.plugin.util.logger.DiscordLogger;

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
                new Thread(() -> {
                    String msg = Translator.translateChat(player, otherPlayer, message);
                    otherPlayer.sendMessage("<[scarlet]A[]> " + msg);
                }).start();
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

            float x = Strings.parseFloat(args[0]) * 8;
            float y = Strings.parseFloat(args[1]) * 8;

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
                    Bundle.bundled(player, "commands.player-notfound");
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

        commandHandler.<Player>register("core", "<planet> <small|medium|big>", "commands.admin.core.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Block core;
            String planet = args[0].toLowerCase();

            if (planet.equals("serpulo")) {
                switch (args[1].toLowerCase()) {
                    case "small" -> core = Blocks.coreShard;
                    case "medium" -> core = Blocks.coreFoundation;
                    case "big" -> core = Blocks.coreNucleus;
                    default -> {
                        Bundle.bundled(player, "commands.admin.core.core-type-not-found");
                        return;
                    }
                }
            } else if (planet.equals("erekir")) {
                switch (args[1].toLowerCase()) {
                    case "small" -> core = Blocks.coreBastion;
                    case "medium" -> core = Blocks.coreCitadel;
                    case "big" -> core = Blocks.coreAcropolis;
                    default -> {
                        Bundle.bundled(player, "commands.admin.core.core-type-not-found");
                        return;
                    }
                }
            } else {
                Bundle.bundled(player, "commands.admin.core.planet-type-not-found");
                return;
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
                Bundle.bundled(player, "commands.admin.access-denied");
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
        });

        commandHandler.<Player>register("effect", "<effect> <x> <y> [rotation] [color]", "call effect", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            String field = args[0].toLowerCase();
            Effect effect;

            try{
                effect = Reflect.get(Fx.class.getField(field));
            } catch (NoSuchFieldException error){
                Bundle.bundled(player, "commands.admin.effect.notfound", args[0].toLowerCase());
                return;
            }

            if (!Strings.canParseFloat(args[1]) || !Strings.canParseFloat(args[2])) {
                Bundle.bundled(player, "commands.incorrect-format.number");
                return;
            }

            float x = Strings.parseFloat(args[1]) * 8;
            float y = Strings.parseFloat(args[2]) * 8;

            if(args.length == 3){
                Call.effect(effect, x, y, 0, Color.white);
                Bundle.bundled(player, "commands.admin.effect.success");
            }

            if(!Strings.canParseFloat(args[3])){
                Bundle.bundled(player, "commands.incorrect-format.number");
                return;
            }

            Float rotation = Strings.parseFloat(args[3]);

            if(args.length == 4){
                Call.effect(effect, x, y, rotation, Color.white);
                Bundle.bundled(player, "commands.admin.effect.success");
            }

            Color color;

            try{
                color = Color.valueOf(args[4]);
            } catch (WrappedException error) {
                Bundle.bundled(player, "commands.admin.effect.string");
                return;
            }
            
            Call.effect(effect, x, y, rotation, color);
        });

        /*
        commandHandler.removeCommand("ban");
        commandHandler.<Player>register("ban", "<name...>", "commands.admin.ban.description", (args, player) -> {
            if (!Variables.admins.containsKey(player.uuid())) {
                Bundle.bundled(player, "commands.access-denied");
                return;
            }

            Player found = Players.findPlayer(args[0]);
            if (found == null) {
                Bundle.bundled(player, "commands.player-notfound");
                return;
            }
            String uuid = found.uuid();

            if (Variables.admins.containsValue(uuid, false)) {
                Bundle.bundled(player, "commands.admin.ban.player-is-admin");
                return;
            }

            try {
                Database.getContext()
                        .update(Tables.USERS)
                        .set(Tables.USERS.BANNED, (byte) 1)
                        .where(Tables.USERS.UUID.eq(args[0]))
                        .execute();

                PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(PlayerEventTypes.BAN.name());
                record.setName(found.name());
                record.setUuid(found.uuid());
                record.setIp(found.ip());
                record.store();

                found.kick(Packets.KickReason.banned);
                Bundle.bundled(player, "commands.admin.ban.banned", args[0]);
                Log.info("@ (@) has banned @ (@)", player.name(), player.uuid(), found.name(), found.uuid());
                Bot.sendMessage(String.format("%s забанил игрока %s", player.name(), found.name()));
            } catch (SQLException e) {
                Log.err("Failed to ban uuid for player '@'", uuid);
                Log.err(e);
                DiscordLogger.err("Failed to ban uuid for player '" + uuid + "'", e);
                Bundle.bundled(player, "commands.admin.ban.failed", args[0]);
            }
        });

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
                boolean exists = Database.getContext()
                        .selectFrom(Tables.USERS)
                        .where(Tables.USERS.UUID.eq(args[0]))
                        .fetch()
                        .size() > 0;

                if (!exists) {
                    player.sendMessage(String.format("[red]Игрок с айди %s не найден[]", args[0]));
                    return;
                }

                Database.getContext()
                        .update(Tables.USERS)
                        .set(Tables.USERS.BANNED, (byte) 0)
                        .where(Tables.USERS.UUID.eq(args[0]))
                        .execute();

                UsersRecord data = Database.getContext()
                        .selectFrom(Tables.USERS)
                        .where(Tables.USERS.UUID.eq(args[0]))
                        .fetchOne();

                player.sendMessage(String.format("[lime]Игрок с айди %s разбанен[]", args[0]));
                Log.info("@ (@) has unbanned @ (@)", player.name(), player.uuid(), data.getName(), args[0]);
                Bot.sendMessage(String.format("%s разбанил игрока %s", player.name(), data.getName()));
            } catch (SQLException e) {
                Log.err("Failed to unban uuid for player '@'", args[0]);
                Log.err(e);
                DiscordLogger.err("Failed to unban uuid for player '" + args[0] + "'", e);
                player.sendMessage(String.format("[red]Не могу разбанить игрока с айди %s[]", args[0]));
            }
        }); // TODO: использовать бандлы
        */

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
            if (Variables.admins.containsValue(uuid, false)) {
                player.sendMessage("[red]Игрок администратор[]");
                return;
            }

            try {
                found.kick(Packets.KickReason.kick);
                PlayerEventsRecord record = Database.getContext().newRecord(Tables.PLAYER_EVENTS);
                record.setServer(Const.SERVER_COLUMN_NAME);
                record.setTimestamp(System.currentTimeMillis() / 1000);
                record.setType(PlayerEventTypes.KICK.name());
                record.setName(found.name());
                record.setUuid(found.uuid());
                record.setIp(found.ip());
                record.store();
            } catch (SQLException e) {
                Log.err(e);
            }
            player.sendMessage(String.format("[lime]Игрок %s выгнан[]", args[0]));
            Log.info("@ (@) has kicked @ (@)", player.name(), player.uuid(), found.name(), found.uuid());
            Bot.sendMessage(String.format("%s выгнал игрока %s", player.name(), found.name()));
        }); // TODO: использовать бандлы
    }
}
