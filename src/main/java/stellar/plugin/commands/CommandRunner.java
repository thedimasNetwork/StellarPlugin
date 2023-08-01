package stellar.plugin.commands;

import mindustry.gen.Player;

public interface CommandRunner {
    void acceptPlayer(String[] args, Player parameter);
}
