package stellar.plugin.util.logger;

import stellar.plugin.bot.Colors;

import java.awt.*;

public enum LogLevel {

    debug("Debug", Colors.green),
    info("Info", Colors.blue),
    warn("Warn", Colors.yellow),
    err("Error", Colors.red);

    public final String name;
    public final Color color;

    LogLevel(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
