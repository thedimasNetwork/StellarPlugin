package stellar.plugin.components;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import org.jooq.Field;
import org.jooq.Record1;
import stellar.plugin.Const;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.PlaytimeRecord;
import stellar.plugin.util.logger.DiscordLogger;

import static stellar.plugin.Variables.interval;
import static stellar.plugin.util.NetUtils.updateBackground;

public class Playtime {
    public static void load() {
        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(1, 3600)) { // 1 minute
                Log.debug(Const.SERVER_COLUMN_NAME);
                if (Const.PLAYTIME_FIELD == null) {
                    Log.err("Server @ does not exist in the Database!", Const.SERVER_COLUMN_NAME);
                    DiscordLogger.err("Сервер " + Const.SERVER_COLUMN_NAME + " не существует в базе данных!");
                    return;
                }
                for (Player p : Groups.player) {
                    try {
                        long computed = Database.getPlaytime(p.uuid(), Const.PLAYTIME_FIELD) + 60;
                        updateBackground(Database.getContext()
                                .update(Tables.PLAYTIME)
                                .set(Const.PLAYTIME_FIELD, computed)
                                .where(Tables.PLAYTIME.UUID.eq(p.uuid())));
                    } catch (Throwable t) {
                        Log.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                        DiscordLogger.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                    }
                }
            }
        });
    }
}
