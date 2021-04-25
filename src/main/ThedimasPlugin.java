package main;

import arc.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.mod.*;
import arc.util.*;

import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static mindustry.Vars.netServer;
import static mindustry.Vars.player;

public class ThedimasPlugin extends Plugin {

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
                Call.infoMessage(event.player.con, Const.WELCOME_UK);
            } else if (event.player.locale.startsWith("ru")) {
                Call.infoMessage(event.player.con, Const.WELCOME_RU);
            } else {
                Call.infoMessage(event.player.con, Const.WELCOME_EN);
            }
            netServer.admins.addChatFilter((player, text) -> null);
        });
        Events.on(EventType.PlayerLeave.class, event -> { // called when player leave
            Call.sendMessage("[scarlet]- [accent]" + event.player.name + "[scarlet] вышел");
            Log.info(event.player.name + " has disconnected from the server");

        });
        Events.on(EventType.PlayerChatEvent.class, event -> {
            Log.info("%0: %1 | %2".replace("%0", event.player.name)
                    .replace("%1", event.message)
                    .replace("%2", event.player.locale));
            String prefix = event.player.admin() ? "\uE82C" : "\uE872";
            Groups.player.each(player -> {
                String translated = event.message;
                try {
                    translated = translate(event.message, player.locale, "auto");
                } catch (IOException e) {
                    Log.info(e.getMessage());
                } finally {
                    String msg = Const.FORMAT.replace("%0", prefix)
                            .replace("%1", event.player.name)
                            .replace("%2", translated);
                    player.sendMessage(msg);
                }
            });
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("rules", "Посмотреть список правил.", (args, player) -> {
            if (player.locale.startsWith("uk")) {
                player.sendMessage(Const.RULES_UK);
            } else if (player.locale.startsWith("ru")) {
                player.sendMessage(Const.RULES_RU);
            } else {
                player.sendMessage(Const.RULES_EN);
            }
        });
        handler.<Player>register("hub", "Подключиться к Хабу.", (args, player) -> {
            Call.connect(player.con, "95.217.226.152", 26160);
        });
        handler.<Player>register("discord", "Получить ссылку на Discord cервер.", (args, player) -> {
            player.sendMessage("https://discord.gg/RkbFYXFU9E");
        });
        //register a whisper command which can be used to send other players messages
        handler.<Player>register("whisper", "<player> <text...>", "Whisper text to another player.", (args, player) -> {
            //find player by name
            Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
            //give error message with scarlet-colored text if player isn't found
            if(other == null){
                player.sendMessage("[scarlet]No player by that name found!");
                return;
            }
            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            if (other.locale.startsWith("uk")) {
                other.sendMessage("[lightgray](шепіт) " + player.name + ":[] " + args[1]);
            } else if (other.locale.startsWith("ru")) {
                other.sendMessage("[lightgray](шепот) " + player.name + ":[] " + args[1]);
            } else {
                other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1]);
            }
        });
    }

    private static String translate(String text, String langTo, String langFrom) throws IOException {
        String urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t&ie=UTF-8&oe=UTF-8" +
                "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&tl=" + langTo +
                "&sl=" + langFrom; // use "&sl=auto" for automatic translations
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
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
}
