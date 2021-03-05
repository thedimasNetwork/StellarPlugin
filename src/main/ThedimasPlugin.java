package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

public class ThedimasPlugin extends Plugin{
    String rules = new String("1. Не спамить/флудить в чат\n"
                            + "2. Не оскорблять других участников сервера\n"
                            + "3. Не быть грифером\n"
                            + "[scarlet]4. Не строить NSFW (18+) схемы[]\n\n"
                            + "[accent]1 нарушение = 1 варн + кик\n"
                            + "3 варна = бан\n"
                            + "NFSW = бан[]\n");
    
    String welcome = new String("Привет, путник!\n"
                              + "Добро пожаловать на сеть серверов от thedimas!\n"
                              + "Вот правила и наказания:\n"
                              + rules + "\n"
                              + "\nЕсли ты их забыл, то можешь ввести комманду [accent]/rules");
    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");
        Events.on(EventType.PlayerJoin.class, event -> { // called when player join
            Call.infoMessage(event.player.con, welcome);
        });
    }
    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("rules", "Get list of rules.", (args, player) -> {
            player.sendMessage(rules);
        });
    }
}
