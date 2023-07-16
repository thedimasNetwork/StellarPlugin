package stellar.plugin.menus;

import mindustry.gen.Player;

public interface MenuRunner {
    void accept(int menuId, int option, Player player);
}
