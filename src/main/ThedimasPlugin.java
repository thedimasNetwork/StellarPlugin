package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

import static mindustry.Vars.netServer;

public class ThedimasPlugin extends Plugin{
    String rules_ru = new String("1. Не спамить/флудить в чат\n"
                            + "2. Не оскорблять других участников сервера\n"
                            + "3. Не быть грифером\n"
                            + "[scarlet]4. Не строить NSFW (18+) схемы[]\n");
    
    String welcome_ru = new String("[white]Привет, путник!\n"
                                 + "Добро пожаловать на сеть серверов от thedimas!\n"
                                 + "Вот правила:\n"
                                 + "[accent]" + rules_ru + "[white]\n"
                                 + "\nЕсли ты их забыл, то можешь ввести комманду [accent]/rules[]\n"
                                 + "Подробные правла ты можешь на нашем Discord сервере");

    String rules_en = new String("1. Don't spam/flood in the chat\n"
                               + "2. Don't insult another players\n"
                               + "3. Don't grief\n"
                               + "[scarlet]4. Don't build NSFW (18+) schemes[]\n");

    String welcome_en = new String("[white]Hi, traveller!\n"
                                 + "Welcome to thedimas' servers!\n"
                                 + "Here are the rules:\n"
                                 + "[accent]" + rules_en + "[]\n"
                                 + "\nIf you forgot them, you can type [accent]/rules[] command\n"
                                 + "Detailed rules you can get in our Discord server");
            
    //called when game initializes
    @Override
    public void init() {
        Log.info("thedimasPlugin launched!");
        Events.on(EventType.PlayerJoin.class, event -> { // called when player join
            Log.info(event.player.name + " has joined the server");
            Log.info("\tLocale: " + event.player.locale);
            Log.info("\tIP: " + event.player.con.address);
            Call.sendMessage("[lime]+ [accent]" + event.player.name + "[lime] присоединился");
            if (event.player.locale.startsWith("ru") || event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, welcome_ru);
            } else {
                Call.infoMessage(event.player.con, welcome_en);
            }
            netServer.admins.addChatFilter((player, text) -> null);
        });
        Events.on(EventType.PlayerLeave.class, event -> { // called when player leave
            Call.sendMessage("[scarlet]- [accent]" + event.player.name + "[scarlet] вышел");
            Log.info(event.player.name + " has disconnected from the server");

        });
        Events.on(EventType.PlayerChatEvent.class, event -> {
            if (event.player.admin()) {
                Call.sendMessage("<\uE82C> " + event.player.name + "[white]: " + event.message);
            } else {
                Call.sendMessage("<\uE872> " + event.player.name + "[white]: " + event.message);
            }
        });
    }
    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("rules", "Посмотреть список правил.", (args, player) -> {
            if(player.locale.startsWith("ru") || player.locale.startsWith("uk")) {
                player.sendMessage(rules_ru);
            } else {
                player.sendMessage(rules_en);
            } 
        });
        handler.<Player>register("hub", "Подключиться к Хабу.", (args, player) -> {
            Call.connect(player.con, "95.217.226.152", 26160);
        });
        handler.<Player>register("discord", "Получить ссылку на Discord cервер.", (args, player) -> {
            player.sendMessage("https://discord.gg/RkbFYXFU9E");
        });
    }
}
