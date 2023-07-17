package stellar.plugin.menus;

import arc.math.Mathf;
import arc.struct.IntMap;
import mindustry.gen.Call;
import mindustry.gen.Player;

/* === TODO ===
 * Text inputs
 * Follow-up menu
 * Timeout for runners
 */

public class MenuHandler {
    private static final IntMap<MenuRunner> runners = new IntMap<>();
    public static int send(Player player, String title, String message, String[][] buttons, MenuRunner runner) {
        int menuId = Mathf.random(Integer.MAX_VALUE - 1);
        while (runners.containsKey(menuId)) {
            menuId = Mathf.random(Integer.MAX_VALUE - 1);
        }
        runners.put(menuId, runner);
        Call.menu(player.con(), menuId, title, message, buttons);
        return menuId;
    }

    public static void handle(int menuId, int option, Player player) {
        MenuRunner runner = runners.get(menuId);
        if (runner == null) {
            return;
        }
        runner.accept(menuId, option, player);
        runners.remove(menuId);
    }
}
