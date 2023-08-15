package stellar.plugin.util.commands;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Nullable;
import lombok.Getter;
import lombok.Setter;
import mindustry.gen.Player;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;
import thedimas.util.Bundle;
import stellar.plugin.util.logger.DiscordLogger;

import java.util.concurrent.CompletableFuture;

public class CommandManager {
    private final ObjectMap<String, Command> commands = new ObjectMap<>();
    private final Seq<Command> orderedCommands = new Seq<>();

    @Getter
    @Setter
    private CommandHandler clientHandler;

    public void registerPlayer(String name, String params, String description, Rank rank, CommandRunner runner) {
        if (clientHandler == null) {
            throw new IllegalArgumentException("ClientHandler is null!");
        }

        Command cmd = new Command(name, params, description, rank, runner);
        commands.put(name, cmd);
        orderedCommands.add(cmd);
        clientHandler.<Player>register(name, params, description, (args, player) -> {
            Rank playerRank = Variables.ranks.get(player.uuid(), Rank.player);
            Rank specialRank = Variables.specialRanks.get(player.uuid(), Rank.player);

            if (!Rank.max(playerRank, specialRank).gte(rank)) {
                Bundle.bundled(player, "commands.access-denied", rank.formatted(player));
                return;
            }

            CompletableFuture.runAsync(() ->
                    runner.acceptPlayer(args, player)
            ).exceptionally(t -> {
                Log.err("Failed to accept command @ from @", name, player.plainName());
                Log.err(t);
                DiscordLogger.err(String.format("Failed to accept command %s from %s", name, player.plainName()), t);
                return null;
            });
        });
    }

    public void registerPlayer(String name, String description, Rank rank, CommandRunner runner) {
        registerPlayer(name, "", description, rank, runner);
    }

    public void registerPlayer(String name, String params, String description, CommandRunner runner) {
        registerPlayer(name, params, description, Rank.player, runner);
    }

    public void registerPlayer(String name, String description, CommandRunner runner) {
        registerPlayer(name, "", description, Rank.player, runner);
    }

    public Seq<Command> getCommandList() {
        return this.orderedCommands.copy();
    }

    @Nullable
    public Command getCommand(String name) {
        if (commands.containsKey(name)) {
            return commands.get(name);
        } else {
            return Command.fromArc(clientHandler.getCommandList().find(c -> c.text.equals(name)));
        }
    }
}
