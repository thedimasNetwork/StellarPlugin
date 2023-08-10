package stellar.plugin.util.commands;

import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import lombok.Getter;
import lombok.Setter;
import mindustry.gen.Player;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.logger.DiscordLogger;

import java.util.concurrent.CompletableFuture;

@Getter
public class CommandManager {
    private final Seq<String> commands = new Seq<>();
    @Setter
    private CommandHandler clientHandler;

    public void registerPlayer(String name, String params, String description, Rank rank, CommandRunner runner) { // TODO: Command type
        if (clientHandler == null) {
            throw new IllegalArgumentException("ClientHandler is null!");
        }
        commands.add(name);

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
}
