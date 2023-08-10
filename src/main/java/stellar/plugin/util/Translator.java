package stellar.plugin.util;

import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.gen.Player;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import stellar.database.Database;
import stellar.database.DatabaseAsync;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.util.logger.DiscordLogger;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

public class Translator { // TODO: normal async logic
    private static final JsonReader jsonReader = new JsonReader();

    public static String translate(String text, String langTo, String langFrom) {
        /*
         * Второй вариант переводчика. Ответ парсить сложнее
         * Url:  https://translate.googleapis.com/translate_a/single?client=gtx&sl=ru_RU&tl=en_US&dt=t&q=Привет
         * Resp: [[["Hi","Привет",null,null,10]],null,"ru",null,null,null,null,[]]
         */

        HttpUrl url = HttpUrl.parse("https://clients5.google.com/translate_a/t").newBuilder()
                .addQueryParameter("client", "dict-chrome-ex")
                .addQueryParameter("dt", "t")
                .addQueryParameter("sl", langFrom) // NOTE: use "auto" for automatic translations
                .addQueryParameter("tl", langTo)
                .addQueryParameter("q", text)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = Variables.httpClient.newCall(request).execute()) {
            JsonValue json = jsonReader.parse(response.body().string());
            return json.get(0).get(0).asString();
        } catch (Throwable t) {
            Log.err("Failed to translate", t);
            return text;
        }
    }

    public static String translateRaw(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = Database.getPlayer(player.uuid()).getTranslator();
        } catch (Throwable t) {
            Log.err(t);
        }

        String translated = message;
        if (!otherPlayer.locale.equals(player.locale()) && !locale.equals("off")) {
            String targetLocale = locale.equals("auto") || locale.equals("double") ? otherPlayer.locale : locale;
            translated = translate(message, targetLocale.split("#")[0], "auto");
        }

        return translated;
    }

    public static String translateChat(Player player, Player otherPlayer, String message) {
        String locale = otherPlayer.locale;
        try {
            locale = Database.getPlayer(otherPlayer.uuid()).getTranslator();
        } catch (Throwable t) {
            Log.err(t);
        }

        return formatChat(player, translateRaw(player, otherPlayer, message), message, locale.equals("double"));
    }

    public static CompletableFuture<String> translateAsync(String text, String langTo, String langFrom) {
        return CompletableFuture.supplyAsync(() -> translate(text, langTo, langFrom));
    }

    public static CompletableFuture<String> translateRawAsync(Player player, Player otherPlayer, String message) {
        return DatabaseAsync.getPlayerAsync(otherPlayer.uuid()).thenComposeAsync(record -> {
            String locale = record.getTranslator();
            if (!otherPlayer.locale.equals(player.locale()) && !locale.equals("off")) {
                String targetLocale = locale.equals("auto") || locale.equals("double") ? otherPlayer.locale : locale;
                return translateAsync(
                        message, targetLocale.split("#")[0], "auto"
                ).exceptionally(t -> {
                    Log.err(t);
                    DiscordLogger.err(t);
                    return null;
                });
            }
            return CompletableFuture.supplyAsync(() -> message);
        }).exceptionally(t -> {
            Log.err(t);
            DiscordLogger.err(t);
            return null;
        });
    }

    public static CompletableFuture<String> translateChatAsync(Player player, Player otherPlayer, String message) {
        return DatabaseAsync.getPlayerAsync(
                otherPlayer.uuid()
        ).thenCombineAsync(translateRawAsync(player, otherPlayer, message), (record, translated) ->
            formatChat(player, translated, message, record.getTranslator().equals("double"))
        ).exceptionally(t -> {
            Log.err(t);
            DiscordLogger.err(t);
            return null;
        });
    }

    public static String formatChat(Player player, String translated, String message, boolean detailed) {
        return MessageFormat.format(detailed ? Const.chatFormatDetailed : Const.chatFormat, Players.prefixName(player), translated, message);
    }

}
