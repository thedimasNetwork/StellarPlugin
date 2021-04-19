package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

import static mindustry.Vars.netServer;

public class ThedimasPlugin extends Plugin {

    final String RULES_UK = new String("1. Не спамити/флуди в чат\n"
            + "2. Не ображати інших учасників сервера\n"
            + "3. Не бути гріфером\n"
            + "[scarlet]4. Не будувати NSFW (18+) схеми[]\n");

    final String WELCOME_UK = new String("[white]Привіт, подорожній!\n"
            + "Ласкаво просимо на мережу серверів від thedimas!\n"
            + "Jсь правила:\n"
            + "[accent]" + RULES_UK + "[white]\n"
            + "\nЯкщо ти їх забув, то можеш ввести комманду [accent]/rules[]\n"
            + "Докладні правила ти можеш знайти на нашому Discord сервері");

    final String RULES_RU = new String("1. Не спамить/флудить в чат\n"
            + "2. Не оскорблять других участников сервера\n"
            + "3. Не быть грифером\n"
            + "[scarlet]4. Не строить NSFW (18+) схемы[]\n");

    final String WELCOME_RU = new String("[white]Привет, путник!\n"
            + "Добро пожаловать на сеть серверов от thedimas!\n"
            + "Вот правила:\n"
            + "[accent]" + RULES_RU + "[white]\n"
            + "\nЕсли ты их забыл, то можешь ввести комманду [accent]/rules[]\n"
            + "Подробные правила ты можешь найти на нашем Discord сервере");

    final String RULES_EN = new String("1. Don't spam/flood in the chat\n"
            + "2. Don't insult another players\n"
            + "3. Don't grief\n"
            + "[scarlet]4. Don't build NSFW (18+) schemes[]\n");

    final String WELCOME_EN = new String("[white]Hi, traveller!\n"
            + "Welcome to thedimas' servers!\n"
            + "Here are the rules:\n"
            + "[accent]" + RULES_EN + "[]\n"
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
            if (event.player.locale.startsWith("uk")) {
                Call.infoMessage(event.player.con, WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, WELCOME_EN);
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
            if (event.player.locale.startsWith("uk")) {
                player.sendMessage(RULES_UK);
            } else if (event.player.locale.startsWith("ru")) {
                player.sendMessage(RULES_RU);
            } else {
                player.sendMessage(RULES_EN);
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
