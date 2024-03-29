package stellar.plugin;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.net.Administration;
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

    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"); // 04.12.2003 23:09

    public static final int maxIdenticalIPs = 3;
    public static final int winSurvivalWaves = 50;
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
    public static final String serverList = """
            []Hub
            PvP
            Sandbox
            Survival
            Attack
            Hex_PvP
            Erekir_Hexed
            Campaign_maps
            Anarchy
            Castle_wars
            Crawler_arena
            MS_GO
            Zone_capture
            """;
    public static final StringMap serverAddress = StringMap.of(
            "hub", "play.thedimas.pp.ua:6567",
            "survival", "play.thedimas.pp.ua:6501",
            "attack", "play.thedimas.pp.ua:6502",
            "sandbox", "play.thedimas.pp.ua:6503",
            "pvp", "play.thedimas.pp.ua:6504",
            "erekir_hexed", "play.thedimas.pp.ua:6505",
            "anarchy", "play.thedimas.pp.ua:6506",
            "campaign_maps", "play.thedimas.pp.ua:6507",
            "ms_go", "play.thedimas.pp.ua:6508",
            "hex_pvp", "play.thedimas.pp.ua:6509",
            "castle_wars", "play.thedimas.pp.ua:6510",
            "crawler_arena", "play.thedimas.pp.ua:6511",
            "zone_capture", "play.thedimas.pp.ua:6512",
            "test", "play.thedimas.pp.ua:6666"
    );
    public static final StringMap serverNames = StringMap.of(
            "hub", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE810[#a6e22e]HUB[#e6bd74]\uE810[]",
            "survival", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE86B[#a6e22e]Survival[#e6bd74]\uE86B[]",
            "attack", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE86E[#a6e22e]Attack[#e6bd74]\uE86E[]",
            "sandbox", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE87C[#a6e22e]Sandbox[#e6bd74]\uE87C[]",
            "pvp", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE861[#a6e22e]PvP[#e6bd74]\uE861[]",
            "erekir_hexed", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE861[#a6e22e]Erekir Hex PvP[#e6bd74]\uE861[]",
            "anarchy", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE876[#a6e22e]Anarchy[#e6bd74]\uE876[]",
            "campaign_maps", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE873[#a6e22e]Campaign maps[#e6bd74]\uE873[]",
            "ms_go", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uF018[#a6e22e]MS:GO[#e6bd74]\uF018[]",
            "hex_pvp", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE861[#a6e22e]Hex PvP[#e6bd74]\uE861[]",
            "castle_wars", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE807[#a6e22e]Castle Wars[#e6bd74]\uE807[]",
            "crawler_arena", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE871[#a6e22e]Crawler Arena[#e6bd74]\uE871[]",
            "zone_capture", "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE853[#a6e22e]Zone Capture[#e6bd74]\uE853[]"
    );

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

    public static final String serverFieldName = Const.serverAddress.findKey("play.thedimas.pp.ua:" + Administration.Config.port.num(), false);
    public static final Field<Long> playtimeField = (Field<Long>) Tables.playtime.field(Const.serverFieldName);

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
