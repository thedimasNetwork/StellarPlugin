package stellar.plugin.util.commands;

import mindustry.gen.Player;

public interface CommandRunner {
    void acceptPlayer(String[] args, Player player);
}
