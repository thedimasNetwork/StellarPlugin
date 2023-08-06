package stellar.plugin.util.logger;

import stellar.plugin.bot.Colors;

import java.awt.*;

public enum LogLevel {

    debug("DEBUG", Colors.green),
    info("INFO", Colors.blue),
    warn("WARN", Colors.yellow),
    err("ERROR", Colors.red);

    public final String name;
    public final Color color;

    LogLevel(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
