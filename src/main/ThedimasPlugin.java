package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

import java.io.*;
import java.net.*;
import org.json.*;


import static mindustry.Vars.netServer;
import static mindustry.Vars.player;

public class ThedimasPlugin extends Plugin {
    private final String FORMAT = "<%0> %1[white]: %2";

    final String RULES_UK = new String("1. Не спамити/флудити в чат\n"
            + "2. Не ображати інших учасників сервера\n"
            + "3. Не бути гріфером\n"
            + "[scarlet]4. Не будувати NSFW (18+) схеми[]\n");

    final String WELCOME_UK = new String("[white]Привіт, подорожній!\n"
            + "Ласкаво просимо на мережу серверів від thedimas!\n"
            + "Ось правила:\n"
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

    private static String translate(String text, String langTo, String langFrom) throws IOException {
        String urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t&ie=UTF-8&oe=UTF-8" +
                "&q=" + URLEncoder.encode(text, "UTF-8") +
                "&tl=" + langTo +
                "&sl=" + langFrom; // use "&sl=auto" for automatic translations
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(),  "UTF-8"));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONObject json = new JSONObject(response.toString());
        JSONArray sentences = json.getJSONArray("sentences");
        JSONObject transl = sentences.getJSONObject(0);
        return transl.getString("trans");
    }
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
            String prefix = event.player.admin() ? "\uE82C" : "\uE872";
            Groups.player.each(player -> {
                String translated;
                try {
                    translated = translate(event.message, player.locale, "auto");
                } catch (IOException e) {
                    e.printStackTrace();
                    translated = event.message;
                }
                String msg = FORMAT.replace("%0", prefix)
                                   .replace("%1", event.player.name)
                                   .replace("%2", translated);
                player.sendMessage(msg);
            });
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("rules", "Посмотреть список правил.", (args, player) -> {
            if (player.locale.startsWith("uk")) {
                player.sendMessage(RULES_UK);
            } else if (player.locale.startsWith("ru")) {
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
