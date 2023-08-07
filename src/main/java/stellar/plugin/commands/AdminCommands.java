package stellar.plugin.commands;

import arc.Events;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import rhino.WrappedException;
import stellar.database.Database;
import stellar.database.enums.MessageType;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.bot.Bot;
import stellar.plugin.components.Rank;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.Players;
import stellar.plugin.util.Translator;
import stellar.plugin.util.logger.DiscordLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mindustry.Vars.mods;
import static mindustry.Vars.world;
import static stellar.plugin.Variables.commandManager;

public class AdminCommands {

    public static void load(CommandHandler commandHandler) {
        commandHandler.removeCommand("a");
        commandManager.registerPlayer("a", "<text...>", "commands.admin.a.description", Rank.admin, (args, player) -> {
            String message = args[0];
            Groups.player.each(Player::admin, otherPlayer -> {
                new Thread(() -> {
                    String msg = Translator.translateChat(player, otherPlayer, message);
                    otherPlayer.sendMessage("<[scarlet]A[]> " + msg);
                }).start();
            });

            Log.info("<A>" + Const.chatLogFormat, Strings.stripColors(player.name), Strings.stripColors(message), player.locale);
            new Thread(() -> {
                try {
                    Database.createMessage(Const.serverFieldName, player.uuid(), null, MessageType.admin, message, player.locale());
                } catch (SQLException e) {
                    Log.err(e);
                }
            }).start();
        });

        commandManager.registerPlayer("admin", "commands.admin.admin.description", Rank.admin, (args, player) -> {
            player.admin = !player.admin;
        });

        commandManager.registerPlayer("name", "[name...]", "commands.admin.name.description", Rank.admin, (args, player) -> {
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

        commandManager.registerPlayer("tp", "<x> <y>", "commands.admin.tp.description", Rank.admin, (args, player) -> {
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

        commandManager.registerPlayer("spawn", "<unit> [count] [team]", "commands.admin.unit.description", Rank.admin, (args, player) -> {
            UnitType unit = Vars.content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if (unit == null) {
                Bundle.bundled(player, "commands.admin.unit.notfound", Const.unitList);
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
                Bundle.bundled(player, "commands.admin.unit.team-notfound", Const.teamList);
                return;
            }

            for (int i = 0; count > i; i++) {
                unit.spawn(team, player.x, player.y);
            }

            Bundle.bundled(player, "commands.admin.unit.text", count, unit, team.color, team);
            DiscordLogger.info(String.format("%s заспавнил %d %s для команды %s", player.name, count, unit.name, team));
        });

        commandManager.registerPlayer("team", "<team> [username...]", "commands.admin.team.description", Rank.admin, (args, player) -> {
            Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
            if (team == null) {
                Bundle.bundled(player, "commands.admin.team.notfound", Const.teamList);
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
                    Bundle.bundled(player, "commands.admin.team.successful-updated", otherPlayer.name, team.color, team);
                } else {
                    Bundle.bundled(player, "commands.player-notfound");
                }
            }
        });

        commandManager.registerPlayer("kill", "[username...]", "commands.admin.kill.description", Rank.admin, (args, player) -> {
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

        commandManager.registerPlayer("killall", "[team]", "commands.admin.killall.description", Rank.admin, (args, player) -> {
            if (args.length == 0) {
                Groups.unit.each(Unitc::kill);
                Bundle.bundled(player, "commands.admin.killall.text");

                Log.info("@ убил всех...", Strings.stripColors(player.name));
                DiscordLogger.info(String.format("%s убил всех...", player.name));
            } else {
                Team team = Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[0]));
                if (team == null) {
                    Bundle.bundled(player, "commands.admin.killall.team-notfound", Const.teamList);
                    return;
                }

                Groups.unit.each(u -> u.team == team, Unitc::kill); // Надеюсь, оно работает
                Bundle.bundled(player, "commands.admin.killall.text-teamed", team.color, team);

                Log.info("@ убил всех с команды @...", Strings.stripColors(player.name), team);
                DiscordLogger.info(String.format("%s убил всех с команды %s...", player.name, team));
            }
        });

        commandManager.registerPlayer("core", "<planet> <small|medium|big>", "commands.admin.core.description", Rank.admin, (args, player) -> {
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

        commandManager.registerPlayer("end", "commands.admin.end.description", Rank.admin, (args, player) -> {
            Events.fire(new EventType.GameOverEvent(Team.crux));
            Log.info("@ сменил карту", Strings.stripColors(player.name));
            DiscordLogger.info(String.format("%s сменил карту", player.name));
        });

        commandManager.registerPlayer("js", "<code...>", "JS eval", Rank.console, (args, player) -> {
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

        commandManager.registerPlayer("effect", "<effect> <x> <y> [rotation] [color]", "call effect", Rank.admin, (args, player) -> {
            Effect effect;

            try {
                effect = Reflect.get(Fx.class.getField(args[0]));
            } catch (NoSuchFieldException error) {
                Bundle.bundled(player, "commands.admin.effect.notfound", args[0].toLowerCase());
                return;
            }

            if (!Strings.canParseFloat(args[1]) || !Strings.canParseFloat(args[2])) {
                Bundle.bundled(player, "commands.incorrect-format.number");
                return;
            }

            float x = Strings.parseFloat(args[1]) * 8;
            float y = Strings.parseFloat(args[2]) * 8;

            if (args.length == 3) {
                Call.effect(effect, x, y, 0, Color.white);
                Bundle.bundled(player, "commands.admin.effect.success");
            }

            if (!Strings.canParseFloat(args[3])) {
                Bundle.bundled(player, "commands.incorrect-format.number");
                return;
            }

            Float rotation = Strings.parseFloat(args[3]);

            if (args.length == 4) {
                Call.effect(effect, x, y, rotation, Color.white);
                Bundle.bundled(player, "commands.admin.effect.success");
            }

            Color color;

            try {
                color = Color.valueOf(args[4]);
            } catch (WrappedException error) {
                Bundle.bundled(player, "commands.admin.effect.string");
                return;
            }

            Call.effect(effect, x, y, rotation, color);
        });

        commandHandler.removeCommand("kick");
        commandManager.registerPlayer("kick", "<name...>", "Выгнать игрока", Rank.admin, (args, player) -> {
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

            player.sendMessage(String.format("[lime]Игрок %s выгнан[]", args[0]));
            Log.info("@ (@) has kicked @ (@)", player.name(), player.uuid(), found.name(), found.uuid());
            Bot.sendMessage(String.format("%s выгнал игрока %s", player.name(), found.name()));
        }); // TODO: использовать бандлы

        commandManager.registerPlayer("execute", "<name> <command> [args...]", "Execute command as a player", Rank.console, (args, player) -> {
            Player target = Players.findPlayer(args[0]);
            if (target == null) {
                Bundle.bundled("commands.player-notfound");
                return;
            }

            String command = "/" + args[1];
            if (args.length > 2) {
                command += " " + args[2];
            }
            commandHandler.handleMessage(command, target);
        });

        commandManager.registerPlayer("kills", "<selector...>", "Kill entities by specified selector.", Rank.console, (args, player) -> {
            if (args[0].length() < 2 || !args[0].startsWith("@")) {
                player.sendMessage("[scarlet]Invalid selector![]");
                return;
            }
            String selector = args[0].substring(0, 2);
            Seq<Entityc> entities = new Seq<>();

            switch (selector) {
                case "@p" -> {
                    entities.add(Units.closest(player.team(), player.x(), player.y(), unit -> unit.isPlayer() && !unit.equals(player.unit())));
                }
                case "@r" -> {
                    Seq<Player> players = new Seq<>();
                    Groups.player.copy(players);
                    entities.add(players.random());
                }
                case "@e" -> Groups.all.each(entities::add);
                case "@a" -> Groups.player.each(entities::add);
                case "@u" -> Groups.unit.each(entities::add);
                case "@s" -> entities.add(player);
                default -> {
                    player.sendMessage("[scarlet]Invalid selector![]");
                    return;
                }
            }
            entities = entities.filter(Objects::nonNull);

            if (args[0].length() > 2) {
                Pattern argumentsPattern = Pattern.compile("\\[(.*?)\\]");
                Pattern comparatorPattern = Pattern.compile("(>|>=|<|<=|=|!=|~|!~)");

                Matcher argumentsMatcher = argumentsPattern.matcher(args[0]);
                if (!argumentsMatcher.find()) {
                    player.sendMessage("[scarlet]Invalid arguments![]");
                    return;
                }

                Log.info(argumentsMatcher.group(1));
                for (String argument : argumentsMatcher.group(1).split(",")) {
                    String[] split = argument.split(comparatorPattern.pattern());

                    if (split.length != 2) {
                        player.sendMessage(String.format("[scarlet]Invalid key %s![]", split[0]));
                        return;
                    }

                    Matcher comparatorMatcher = comparatorPattern.matcher(argument);
                    if (!comparatorMatcher.find()) {
                        player.sendMessage(String.format("[scarlet]Invalid comparator in parameter %s![]", argument));
                        return;
                    }

                    String fieldName = split[0], value = split[1], comparator = comparatorMatcher.group();

                    entities.filter(entity -> {
                        try {
                            Object obj = entity;
                            Field field = null;
                            Method method = null;
                            for (String part : fieldName.split("\\.")) {
                                if (field != null) {
                                    obj = field.get(obj);
                                } else if (method != null) {
                                    obj = method.invoke(obj);
                                }
                                if (part.endsWith("()")) {
                                    method = obj.getClass().getMethod(part.replace("()", ""));
                                    field = null;
                                } else {
                                    field = obj.getClass().getField(part);
                                    method = null;
                                }
                                Log.debug("@: @ | @", obj, field, part);
                            }
                            Log.debug(obj.getClass().getName());
                            return Players.fieldCompare(field, obj, value, comparator);
                        } catch (NoSuchMethodException | NoSuchFieldException e) {
                            Log.debug("-- DOES NOT EXIST --");
                            Log.debug(entity);
                            Log.debug("-------");
                            return false;
                        } catch (IllegalAccessException e) {
                            Log.debug("-- ILLEGAL! --");
                            Log.debug(entity);
                            Log.debug("-------");
                            return false;
                        } catch (InvocationTargetException e) {
                            Log.debug("-- WRONG TARGET! --");
                            Log.debug(entity);
                            Log.debug("-------");
                            return false;
                        }
                    });
                    player.sendMessage(String.format("Key: [accent]%s[], value: [accent]%s[], comparator: [accent]%s[]", fieldName, value, comparator));
                }
            }

            if (entities.size <= 0) {
                player.sendMessage("[scarlet]No entities found[]");
                return;
            }
            entities.each(entity -> {
                try {
                    if (entity instanceof Player p) {
                        p.unit().kill();
                    } else {
                        entity.getClass().getMethod("kill").invoke(entity);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    player.sendMessage(String.format("Unable to kill %s (#%d)", entity.getClass().getName(), entity.id()));
                    Log.err(e);
                }
            });
            player.sendMessage(String.format("Killed %d entities", entities.size));
        });
    }
}
