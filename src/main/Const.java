package main;

import mindustry.game.Team;

import java.util.HashMap;
import java.util.Map;

public class Const {
    public static final String FORMAT = "<%0> %1[white]: %2";

    public static final String SERVER_LIST = "[]Hub\n"
            + "PvP\n"
            + "Sandbox\n"
            + "Survival\n"
            + "Attack\n"
            + "Hex PvP\n"
            + "Annexation\n"
            + "Campaign maps\n"
            + "Anarchy\n"
            + "Castle wars\n"
            + "Crawler arena\n"
            + "MS:GO";

    public static final String UNIT_LIST = "[white]Ground:\n"
            + "[accent]dagger [](\uF800), [accent]mace [](\uF7FF), [accent]fortress [](\uF7FE), [accent]scepter [](\uF7DB), [accent]reign [](\uF7DA)\n"
            + "[purple]crawler [](\uF7FA), [purple]atrax [](\uF7F9), [purple]spiroct [](\uF7F8), [purple]arkyid [](\uF7F7), [purple]toxopid [](\uF7DE)\n"
            + "[lime]nova [](\uF7FD), [lime]pulsar [](\uF7FC), [lime]quasar [](\uF7FB), [lime]vela [](\uF7C1), [lime]corvus [](\uF7C0)\n"
            + "[white]Air:\n"
            + "[accent]flare [](\uF7F6), [accent]horizon [](\uF7F5), [accent]zenith [](\uF7F4), [accent]antumbra [](\uF7F3), [accent]eclipse [](\uF7F2)\n"
            + "[lime]mono [](\uF7F1), [lime]poly [](\uF7F0), [lime]mega [](\uF7EF), [lime]quad [](\uF7C3), [lime]oct [](\uF7C2)\n"
            + "[white]Naval:\n"
            + "[accent]risso [](\uF7E7), [accent]minke [](\uF7ED), [accent]bryde [](\uF7EC), [accent]sei [](\uF7C4), [accent]omura [](\uF7C6)\n"
            + "[white]Core:\n"
            + "[accent]alpha [](\uF7EB), [accent]beta [](\uF7EA), [accent]gamma [](\uF7E9)[]";

    public static final String TEAM_LIST = "[yellow]sharded\n"
            + "[red]crux\n"
            + "[green]green\n"
            + "[purple]purple\n"
            + "[blue]blue\n"
            + "[gray]derelict[]";

    public static final Map<String, String> SERVER_ADDRESS = new HashMap<>();

    public static final Map<String, Team> TEAM_MAP = new HashMap<>();

    static {
        SERVER_ADDRESS.put("hub", "95.217.226.152:26160");
        SERVER_ADDRESS.put("pvp", "178.170.47.34:20566");
        SERVER_ADDRESS.put("sandbox", "178.170.47.34:20594");
        SERVER_ADDRESS.put("survival", "178.170.47.34:20745");
        SERVER_ADDRESS.put("attack", "178.170.47.34:20752");
        SERVER_ADDRESS.put("hex pvp", "178.170.47.34:20636");
        SERVER_ADDRESS.put("annexation", "178.170.47.34:20664");
        SERVER_ADDRESS.put("campaign maps", "178.170.47.34:20981");
        SERVER_ADDRESS.put("anarchy", "95.217.226.152:26233");
        SERVER_ADDRESS.put("castle wars", "95.217.226.152:26194");
        SERVER_ADDRESS.put("crawler arena", "95.217.226.152:26004");
        SERVER_ADDRESS.put("ms:go", "95.217.226.152:26021");

        TEAM_MAP.put("sharded", Team.sharded);
        TEAM_MAP.put("crux", Team.crux);
        TEAM_MAP.put("green", Team.green);
        TEAM_MAP.put("purple", Team.purple);
        TEAM_MAP.put("blue", Team.blue);
        TEAM_MAP.put("derelict", Team.derelict);
    }

    public static final String RULES_UK = "1. Не спамити/флудити в чат\n"
            + "2. Не ображати інших учасників сервера\n"
            + "3. Не бути гріфером\n"
            + "[scarlet]4. Не будувати NSFW (18+) схеми[]\n";

    public static final String WELCOME_UK = "[white]Привіт, подорожній!\n"
            + "Ласкаво просимо на мережу серверів від thedimas!\n"
            + "Ось правила:\n"
            + "[accent]" + RULES_UK + "[white]\n"
            + "\nЯкщо ти їх забув, то можеш ввести комманду [accent]/rules[]\n"
            + "Докладні правила ти можеш знайти на нашому Discord сервері";

    public static final String RULES_RU = "1. Не спамить/флудить в чат\n"
            + "2. Не оскорблять других участников сервера\n"
            + "3. Не быть грифером\n"
            + "[scarlet]4. Не строить NSFW (18+) схемы[]\n";

    public static final String WELCOME_RU = "[white]Привет, путник!\n"
            + "Добро пожаловать на сеть серверов от thedimas!\n"
            + "Вот правила:\n"
            + "[accent]" + RULES_RU + "[white]\n"
            + "\nЕсли ты их забыл, то можешь ввести комманду [accent]/rules[]\n"
            + "Подробные правила ты можешь найти на нашем Discord сервере";

    public static final String RULES_EN = "1. Don't spam/flood in the chat\n"
            + "2. Don't insult another players\n"
            + "3. Don't grief\n"
            + "[scarlet]4. Don't build NSFW (18+) schemes[]\n";

    public static final String WELCOME_EN = "[white]Hi, traveller!\n"
            + "Welcome to thedimas' servers!\n"
            + "Here are the rules:\n"
            + "[accent]" + RULES_EN + "[]\n"
            + "\nIf you forgot them, you can type [accent]/rules[] command\n"
            + "Detailed rules you can get in our Discord server";
}
