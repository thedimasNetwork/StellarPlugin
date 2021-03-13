package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

public class ThedimasPlugin extends Plugin{
    String rules_ru = new String("1. Не спамить/флудить в чат\n"
                            + "2. Не оскорблять других участников сервера\n"
                            + "3. Не быть грифером\n"
                            + "[scarlet]4. Не строить NSFW (18+) схемы[]\n\n"
                            + "[accent]1 нарушение = 1 варн + кик\n"
                            + "3 варна = бан\n"
                            + "NFSW = бан[]");
    
    String welcome_ru = new String("Привет, путник!\n"
                                 + "Добро пожаловать на сеть серверов от thedimas!\n"
                                 + "Вот правила и наказания:\n"
                                 + rules_ru + "\n"
                                 + "\nЕсли ты их забыл, то можешь ввести комманду [accent]/rules");

    String rules_en = new String("1. Don't spam/flood in the chat\n"
                               + "2. Don't insult another players\n"
                               + "3. Don't grief\n"
                               + "[scarlet]4. Don't build NSFW (18+) schemes[]\n\n"
                               + "[accent]1 breach = 1 warn + kick\n"
                               + "3 warns = ban\n"
                               + "NFSW = ban[]");

    String welcome_en = new String("Hi, traveller!\n"
                                 + "Welcome back to thedimas' servers!\n"
                                 + "Here are the rules and punishments:\n"
                                 + rules_en + "\n"
                                 + "\nIf you forgot them, you can type [accent]/rules[] command");
    
    String mods_ip = new String("95.217.226.152:26233");
    
    String mods_ru = new String("[accent]Нужные моды:\n"
                              + "[green]Braindustry\n"
                              + "[green]BetaMindy\n"
                              + "[green]ReVision\n\n"
                              + "[accent]IP сервера[]: [cyan]" + mods_ip);

    String mods_en = new String("[accent]Necessary mods:\n"
                              + "[green]Braindustry\n"
                              + "[green]BetaMindy\n"
                              + "[green]ReVision\n\n"
                              + "[accent]Server IP[]: [cyan]" + mods_ip);
            
    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");
        Events.on(EventType.PlayerJoin.class, event -> { // called when player join
            Log.info(event.player.name + " has joined the server");
            Log.info("\tLocale: " + event.player.locale);
            Log.info("\tIP: " + event.player.con.address);
            if(event.player.locale.startsWith("ru") || event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, welcome_ru);
            } else {
                Call.infoMessage(event.player.con, welcome_en);
            } 
        });
    }
    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("rules", "Get list of rules.", (args, player) -> {
            if(player.locale.startsWith("ru") || player.locale.startsWith("uk")) {
                player.sendMessage(rules_ru);
            } else {
                player.sendMessage(rules_en);
            } 
        });
        handler.<Player>register("hub", "Connect to HUB.", (args, player) -> {
            Call.connect(player.con, "95.217.226.152", 26160);
        });
        handler.<Player>register("mods", "Get list of mods.", (args, player) -> {
            if(player.locale.startsWith("ru") || player.locale.startsWith("uk")) {
                player.sendMessage(mods_ru);
            } else {
                player.sendMessage(mods_en);
            } 
        });
    }
}
