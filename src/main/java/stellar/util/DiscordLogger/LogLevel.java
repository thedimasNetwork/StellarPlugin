package stellar.util.DiscordLogger;

import java.awt.*;

public enum LogLevel {

    INFO("<:info:871833048956678164> INFO", new Color(0x0028CD)),
    WARN("<:warn:871831558376530041> WARN", new Color(0xE07400)),
    ERR("<:error:871832017237573632> ERROR", new Color(0xE50000)),
    DEBUG("<:debug:871833418319691777> DEBUG", new Color(0x01D90B));

    public final String name;
    public final Color color;

    LogLevel(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
