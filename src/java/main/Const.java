package main;

import arc.struct.StringMap;

public class Const {
    public static final String JOIN_LOG_FORMAT = "{0} has joined the server\tLocale: {1}\tIP: {2}";
    public static final String CHAT_LOG_FORMAT = "{0}: {1} | {2}";
    public static final String CHAT_FORMAT = "<{0}> {1}[white]: {2}";

    public static final int HISTORY_PAGE_SIZE = 6;

    public static final String BOOL_VALUES = "1, on, yes, true, вкл, да";

    public static final String TEAM_LIST = "[white][yellow]sharded[], [red]crux[], [green]green[], [purple]purple[], [blue]blue[], [gray]derelict[]";

    public static final String UNIT_LIST
            = "[white]Ground:\n"
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

    public static final String SERVER_LIST
            = "[]Hub\n"
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
            + "MS:GO\n"
            + "Zone capture";

    public static final StringMap SERVER_ADDRESS = StringMap.of(
            "hub", "play.thedimas.pp.ua:6567",
            "survival", "play.thedimas.pp.ua:6501",
            "attack", "play.thedimas.pp.ua:6502",
            "sandbox", "play.thedimas.pp.ua:6503",
            "pvp", "play.thedimas.pp.ua:6504",
            "annexation", "play.thedimas.pp.ua:6505",
            "anarchy", "play.thedimas.pp.ua:6506",
            "campaign maps", "play.thedimas.pp.ua:6507",
            "ms:go", "play.thedimas.pp.ua:6508",
            "hex pvp", "play.thedimas.pp.ua:6509",
            "castle wars", "play.thedimas.pp.ua:6510",
            "crawler arena", "play.thedimas.pp.ua:6511",
            "zone capture", "play.thedimas.pp.ua:6512");

    public static final String RULES_UK
            = "1. Не спамити/флудити в чат\n"
            + "2. Не ображати інших учасників сервера\n"
            + "3. Не бути гріфером\n"
            + "[scarlet]4. Не будувати NSFW (18+) схеми[]\n";

    public static final String WELCOME_UK
            = "[white]Привіт, подорожній!\n"
            + "Ласкаво просимо на мережу серверів від thedimas!\n"
            + "Ось правила:\n"
            + "[accent]" + RULES_UK + "[white]\n"
            + "\nЯкщо ти їх забув, то можеш ввести комманду [accent]/rules[]\n"
            + "Докладні правила ти можеш знайти на нашому Discord сервері";

    public static final String RULES_RU
            = "1. Не спамить/флудить в чат\n"
            + "2. Не оскорблять других участников сервера\n"
            + "3. Не быть грифером\n"
            + "[scarlet]4. Не строить NSFW (18+) схемы[]\n";

    public static final String WELCOME_RU
            = "[white]Привет, путник!\n"
            + "Добро пожаловать на сеть серверов от thedimas!\n"
            + "Вот правила:\n"
            + "[accent]" + RULES_RU + "[white]\n"
            + "\nЕсли ты их забыл, то можешь ввести комманду [accent]/rules[]\n"
            + "Подробные правила ты можешь найти на нашем Discord сервере";

    public static final String RULES_EN
            = "1. Don't spam/flood in the chat\n"
            + "2. Don't insult another players\n"
            + "3. Don't grief\n"
            + "[scarlet]4. Don't build NSFW (18+) schemes[]\n";

    public static final String WELCOME_EN
            = "[white]Hi, traveller!\n"
            + "Welcome to thedimas' servers!\n"
            + "Here are the rules:\n"
            + "[accent]" + RULES_EN + "[]\n"
            + "\nIf you forgot them, you can type [accent]/rules[] command\n"
            + "Detailed rules you can get in our Discord server";
}
