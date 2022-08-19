package stellar.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Translator {
    public static String translate(String text, String langTo, String langFrom) throws IOException {
        // Второй вариант переводчика но запрос парсить сложнее
        // * Url:  https://translate.googleapis.com/translate_a/single?client=gtx&sl=ru_RU&tl=en_US&dt=t&q=Привет
        // * Resp: [[["Hi","Привет",null,null,10]],null,"ru",null,null,null,null,[]]
        String urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t" +
                "&tl=" + langTo +
                "&sl=" + langFrom + // use "&sl=auto" for automatic translations
                "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

                URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.substring(2, response.length() - 2);
    }
}
