package stellar.plugin.util.commands;

import arc.util.CommandHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import mindustry.gen.Player;
import stellar.plugin.Variables;
import stellar.plugin.components.Rank;

@Getter
@AllArgsConstructor
public class Command {
    private final String name, paramText, description;
    private final Rank rank;
    private final CommandRunner runner;

    public boolean isAllowed(Player player) {
        return Rank.max(Variables.ranks.get(player.uuid(), Rank.player), Variables.specialRanks.get(player.uuid()))
                .gte(this.rank);
    }

    public static Command fromArc(CommandHandler.Command command, Rank rank) {
        return new Command(command.text, command.paramText, command.description, rank, (a, p) -> {});
    }

    public static Command fromArc(CommandHandler.Command command) {
        return fromArc(command, Rank.player);
    }
}
