package stellar.plugin.components;

import arc.Events;
import arc.graphics.Color;
import arc.util.Timer;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.type.Item;

import static mindustry.Vars.content;
import static mindustry.Vars.state;

public class LaunchPad {
    public static void load() {
        Events.on(EventType.PlayEvent.class, event -> state.rules.revealedBlocks.add(Blocks.launchPad));

        Timer.schedule(() -> Groups.build.each(b -> b.block instanceof mindustry.world.blocks.campaign.LaunchPad, building -> {
            if (building.items.total() == 100 && building.power.status > 0.95) {
                Building core = building.closestCore();
                if (core == null) {
                    return;
                }

                for (int i = 0; i < building.items.length(); i++) {
                    Item item = content.items().get(i);
                    if (building.items.has(item) && !core.acceptItem(building, item)) {
                        return;
                    }
                }

                Call.effect(Fx.coreLaunchConstruct, building.tile.drawx(), building.tile.drawy(), 0, Color.white);
                building.items.each((item, amount) -> core.items.add(item, amount));
                Call.clearItems(building);
            }
        }), 0, 0.1F);
    }
}
