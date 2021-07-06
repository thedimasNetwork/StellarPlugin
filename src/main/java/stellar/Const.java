package stellar;

import arc.files.Fi;
import arc.struct.StringMap;
import arc.util.*;
import mindustry.Vars;

import java.util.*;

public class Const {

    public static final String JOIN_LOG_FORMAT = "@ has joined the server | locale: @ | IP: @";
    public static final String CHAT_LOG_FORMAT = "@: @ | @";

    public static final String CHAT_FORMAT = "<{0}> {1}[white]: {2}";
    public static final String CHAT_FORMAT_DETAILED = CHAT_FORMAT + " [gray]({3}[gray])";

    public static final float VOTES_RATIO = 0.6f;

    public static final float LIST_PAGE_SIZE = 6f;

    public static final String BOOL_VALUES = "1, on, yes, true, вкл, да";

    public static final StringMap SERVER_NAMES = StringMap.of(
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE810[#a6e22e]HUB[#e6bd74]\uE810[]", "hub",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE86B[#a6e22e]Survival[#e6bd74]\uE86B[]", "survival",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE86E[#a6e22e]Attack[#e6bd74]\uE86E[]", "attack",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE87C[#a6e22e]Sandbox[#e6bd74]\uE87C[]", "sandbox",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE861[#a6e22e]PvP[#e6bd74]\uE861[]", "pvp",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE861[#a6e22e]Annexation[#e6bd74]\uE861[]", "annexation",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE876[#a6e22e]Anarchy[#e6bd74]\uE876[]", "anarchy",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE873[#a6e22e]Campaign maps[#e6bd74]\uE873[]", "campaign_maps",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uF018[#a6e22e]MS:GO[#e6bd74]\uF018[]", "ms:go",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE861[#a6e22e]Hex PvP[#e6bd74]\uE861[]", "hex_pvp",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE807[#a6e22e]Castle Wars[#e6bd74]\uE807[]", "castle_wars",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE871[#a6e22e]Crawler Arena[#e6bd74]\uE871[]", "crawler_arena",
            "[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]\uE853[#a6e22e]Zone Capture[#e6bd74]\uE853[]", "zone_capture");

    // для отложенной инициализации
    // это нужно из-за того, что Vars.locale инициализируется _не очень вовремя_
    // (из-за асинхронного запроса)
    public static class LocaleListHolder {
        public static final String LOCALE_LIST;

        static {
            StringBuilder tmp = new StringBuilder();

            for (int i = 0; i < Vars.locales.length; i++) {
                tmp.append(Vars.locales[i].toString());
                if (i != Vars.locales.length - 1) {
                    tmp.append(", ");
                }
                if (i % 6 == 0) {
                    tmp.append("\n");
                }
            }

            LOCALE_LIST = tmp.toString();
        }
    }

    // языки для которых есть перевод
    public static final Locale[] supportedLocales;

    public static Locale defaultLocale() {
        return Structs.find(supportedLocales, l -> l.toString().equals("en"));
    }

    static {
        Fi[] files = Vars.mods.list().find(mod -> mod.main instanceof ThedimasPlugin).root.child("bundles").list();
        supportedLocales = new Locale[files.length + 1];
        supportedLocales[supportedLocales.length - 1] = new Locale("router"); // router

        for (int i = 0; i < files.length; i++) {
            String code = files[i].nameWithoutExtension();
            supportedLocales[i] = ThedimasPlugin.parseLocale(code.substring("bundle_".length()));
        }

        Log.debug("Loaded locales: @", Arrays.toString(supportedLocales));
    }

    public static final String TEAM_LIST = "[white][yellow]sharded[], [red]crux[], [green]green[], [purple]purple[], [blue]blue[], [gray]derelict[]";

    public static final String UNIT_LIST = """
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
            [accent]alpha [](\uF7EB), [accent]beta [](\uF7EA), [accent]gamma [](\uF7E9)[]""";

    public static final String SERVER_LIST = """
            []Hub
            PvP
            Sandbox
            Survival
            Attack
            Hex_PvP
            Annexation
            Campaign_maps
            Anarchy
            Castle_wars
            Crawler_arena
            MS:GO
            Zone_capture
            """;

    public static final StringMap SERVER_ADDRESS = StringMap.of(
            "hub",              "v7.thedimas.pp.ua:6567",
            "survival",         "v7.thedimas.pp.ua:6501",
            "attack",           "v7.thedimas.pp.ua:6502",
            "sandbox",          "v7.thedimas.pp.ua:6503",
            "pvp",              "v7.thedimas.pp.ua:6504",
            "annexation",       "v7.thedimas.pp.ua:6505",
            "anarchy",          "v7.thedimas.pp.ua:6506",
            "campaign_maps",    "v7.thedimas.pp.ua:6507",
            "ms:go",            "v7.thedimas.pp.ua:6508",
            "hex_pvp",          "v7.thedimas.pp.ua:6509",
            "castle_wars",      "v7.thedimas.pp.ua:6510",
            "crawler_arena",    "v7.thedimas.pp.ua:6511",
            "zone_capture",     "v7.thedimas.pp.ua:6512");
}
