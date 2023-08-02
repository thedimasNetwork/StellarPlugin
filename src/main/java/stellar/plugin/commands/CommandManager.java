package stellar.plugin.commands;

import arc.func.Cons;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.CommandHandler;
import arc.util.Log;
import lombok.Getter;
import lombok.Setter;
import mindustry.gen.Player;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;
import stellar.plugin.util.Bundle;

import java.sql.SQLException;

@Getter
public class CommandManager {
    @Setter
    private CommandHandler clientHandler;
    private final Seq<String> commands = new Seq<>();

    public void registerPlayer(String name, String params, String description, Rank rank, CommandRunner runner) { // TODO: Command type
        if (clientHandler == null) {
            throw new IllegalArgumentException("ClientHandler is null!");
        }
        commands.add(name);

        clientHandler.<Player>register(name, params, description, (args, player) -> {
            Rank playerRank = Rank.player;
            Rank specialRank = Rank.player;
            try {
                playerRank = Rank.getRank(player);
                specialRank = Variables.specialRanks.get(player.uuid());
            } catch (SQLException e) {
                Log.err(e);
            }

            if (!Rank.max(playerRank, specialRank).gte(rank)) {
                Bundle.bundled(player, "commands.access-denied", rank.formatted(player));
                return;
            }

            runner.acceptPlayer(args, player);
        });
    }

    public void registerPlayer(String name, String description, Rank rank, CommandRunner runner) {
        registerPlayer(name, "", description, rank, runner);
    }

//    public void registerPlayer(String name, String params, String description, CommandRunner runner) {
//        registerPlayer(name, params, description, Rank.player, runner);
//    }
//
//    public void registerPlayer(String name, String description, CommandRunner runner) {
//        registerPlayer(name, "", description, Rank.player, runner);
//    }
}
