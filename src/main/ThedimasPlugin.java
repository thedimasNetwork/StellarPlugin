package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.Log;

public class ThedimasPlugin extends Plugin{

    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launced!")
        Events.on(EventType.PlayerJoin.class, event -> { // called when player join
            Call.infoMessage(event.player.con, "Привет, путник!");
        });
    }
}
