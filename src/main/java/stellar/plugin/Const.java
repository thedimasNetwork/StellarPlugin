package stellar.plugin;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Structs;
import mindustry.Vars;
import org.jooq.Field;
import stellar.database.enums.PlayerStatus;
import stellar.database.gen.Tables;
import thedimas.util.Bundle;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

import static mindustry.Vars.mods;

public class Const {

    public static final String pluginFolder = "config/mods/" + mods.list().find(l -> l.main instanceof ThedimasPlugin).meta.name + "/";
    public static final String pluginVersion = mods.list().find(l -> l.main instanceof ThedimasPlugin).meta.version;

    public static final String joinLogFormat = "@ has joined the server | UUID: @ | IP: @ | Locale: @";
    public static final String chatLogFormat = "@: @ | @";

    public static final String chatFormat = "{0}[white]: {1}";
    public static final String chatFormatDetailed = chatFormat + " [gray]({2}[gray])";

    public static final float votesRatio = 0.6f;

    public static final float listPageSize = 6f;

    public static final String boolValues = "1, on, yes, true, вкл, да";

    public static final String[] pirates = {"valve", "igruhaorg", "tuttop", "freetp.org", "freetp"};
    public static final Seq<String> usefulCommands = Seq.with("help", "rtv", "stats", "ranks");

    public static final DateTimeFormatter dateFormatterFull = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"); // 04.12.2003 23:59
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"); // 04.12.2003

    public static final int maxIdenticalIPs = 3;
    public static final int winSurvivalWaves = 50;
    public static final long votekickChannel = 1270136943690121296L;
    // языки, для которых у нас есть перевод
    public static final Locale[] supportedLocales;
    public static final String teamList = "[white][yellow]sharded[], [red]crux[], [purple]malis[], [green]green[], [blue]blue[], [gray]derelict[]";
    public static final String unitList = """
            [accent]Serpulo:
            [white]Ground:
            [accent]dagger [](\uF800), [accent]mace [](\uF7FF), [accent]fortress [](\uF7FE), [accent]scepter [](\uF7DB), [accent]reign [](\uF7DA)
            [purple]crawler [](\uF7FA), [purple]atrax [](\uF7F9), [purple]spiroct [](\uF7F8), [purple]arkyid [](\uF7F7), [purple]toxopid [](\uF7DE)
            [lime]nova [](\uF7FD), [lime]pulsar [](\uF7FC), [lime]quasar [](\uF7FB), [lime]vela [](\uF7C1), [lime]corvus [](\uF7C0)
            [white]Air:
            [accent]flare [](\uF7F6), [accent]horizon [](\uF7F5), [accent]zenith [](\uF7F4), [accent]antumbra [](\uF7F3), [accent]eclipse [](\uF7F2)
            [lime]mono [](\uF7F1), [lime]poly [](\uF7F0), [lime]mega [](\uF7EF), [lime]quad [](\uF7C3), [lime]oct [](\uF7C2)
            [white]Naval:
            [accent]risso [](\uF7E7), [accent]minke [](\uF7ED), [accent]bryde [](\uF7EC), [accent]sei [](\uF7C4), [accent]omura [](\uF7C6)
            [white]Core:
            [accent]alpha [](\uF7EB), [accent]beta [](\uF7EA), [accent]gamma [](\uF7E9)[]
            [accent]Erekir:
            [white]Ground:
            [#f69466]stell [](\uF6B5), [#f69466]locus [](\uF6B3), [#f69466]precept [](\uF69C), [#f69466]vanquish [](\uF6F4), [#f69466]conquer [](\uF6CF)
            [#c7caeb]merui [](\uF69E), [#c7caeb]cleroi [](\uF6B1), [#c7caeb]anthicus [](\u63142), [#c7caeb]tecta [](\uF699), [#c7caeb]collaris [](\uF698)
            [white]Air:
            [#c8779bd]elude [](\uF697), [#c8779bd]avert [](\uF6B2), [#c8779bd]obviate [](\uF6A3), [#c8779bd]quell [](\uF6EC), [#c8779bd]disrupt [](\uF6CE)
            [white]Core:
            [accent]evoke [](\uF735), [accent]incite [](\uF724), [accent]emanate [](\uF719) 
            """;

    public static final StringMap translatorLocales = StringMap.of( // Top 25 locales by popularity on the server
            "ru", "Русский",
            "en", "English",
            "es", "Español",
            "zh", "简体中文",
            "uk_UA", "Українська",
            "vi", "Tiếng Việt",
            "pt", "Português",
            "th", "ไทย",
            "in", "Indonesian",
            "fr", "Français",
            "pl", "Polski",
            "tr", "Türkçe",
            "de", "Deutsch",
            "ko", "한국어",
            "it", "Italiano",
            "cs", "Čeština",
            "ja", "日本語",
            "ar", "العربية",
            "hu", "Magyar",
            "ro", "Română",
            "nl", "Nederlands",
            "fa", "فارسی",
            "fil", "Filipino",
            "sk", "Slovenčina",
            "ms", "Malay"
    );

    public static final ObjectMap<PlayerStatus, String> statusNames = ObjectMap.of(
            PlayerStatus.basic, "Игрок :bust_in_silhouette:",
            PlayerStatus.admin, "Админ :hammer:",
            PlayerStatus.console, "Консоль :wrench:",
            PlayerStatus.owner, "Владелец :crown:"
    );

    static {
        Fi[] files = Vars.mods.list().find(mod -> mod.main instanceof ThedimasPlugin).root.child("bundles").list();
        supportedLocales = new Locale[files.length + 1];
        supportedLocales[supportedLocales.length - 1] = new Locale("router"); // router

        for (int i = 0; i < files.length; i++) {
            String code = files[i].nameWithoutExtension();
            supportedLocales[i] = Bundle.parseLocale(code.substring("bundle_".length()));
        }

        Log.debug("Loaded locales: @", Arrays.toString(supportedLocales));
    }

    public static Locale defaultLocale() {
        return Structs.find(supportedLocales, l -> l.toString().equals("en"));
    }
}
