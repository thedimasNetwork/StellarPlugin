package stellar.util;

import arc.util.serialization.Jval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Translator {
    public static String translate(String text, String langTo, String langFrom) throws IOException {
        String urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t&ie=UTF-8&oe=UTF-8" +
                "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&tl=" + langTo +
                "&sl=" + langFrom; // use "&sl=auto" for automatic translations
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return Jval.read(response.toString()).get("sentences").asArray()
                .firstOpt().getString("trans");
    }
}
