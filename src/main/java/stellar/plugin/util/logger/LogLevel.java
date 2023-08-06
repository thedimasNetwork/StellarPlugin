package stellar.plugin.util.logger;

import stellar.plugin.bot.Colors;

import java.awt.*;

public enum LogLevel {

    INFO("INFO", Colors.blue),
    WARN("WARN", Colors.yellow),
    ERR("ERROR", Colors.red),
    DEBUG("DEBUG", Colors.green);

    public final String name;
    public final Color color;

    LogLevel(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
