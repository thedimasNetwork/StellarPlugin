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

public class Playtime {
    public static void load() {
        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(1, 3600)) { // 1 minute
                Field<Long> field = (Field<Long>) Tables.PLAYTIME.field(Const.SERVER_COLUMN_NAME);
                Log.debug(Const.SERVER_COLUMN_NAME);
                if (field == null) {
                    Log.err("Server @ does not exist in the Database!", Const.SERVER_COLUMN_NAME);
                    DiscordLogger.err("Сервер " + Const.SERVER_COLUMN_NAME + " не существует в базе данных!");
                    return;
                }
                for (Player p : Groups.player) {
                    try {
                        Record1<Long> timeFetch = Database.getContext()
                                .select(field)
                                .from(Tables.PLAYTIME)
                                .where(Tables.PLAYTIME.UUID.eq(p.uuid()))
                                .fetchOne();
                        long time = 0;
                        if (timeFetch == null) {
                            Log.warn("Player '" + p.uuid() + "' doesn't exists");
                            DiscordLogger.warn("Player '" + p.uuid() + "' doesn't exists");
                            PlaytimeRecord playtimeRecord = Database.getContext().newRecord(Tables.PLAYTIME);
                            playtimeRecord.setUuid(p.uuid());
                            playtimeRecord.store();
                        } else {
                            time = timeFetch.value1();
                        }
                        long computed = time + 60;
                        Log.debug("@| @: @-@", field.getName(), p.name(), time, computed);
                        Database.getContext()
                                .update(Tables.PLAYTIME)
                                .set(field, computed)
                                .where(Tables.PLAYTIME.UUID.eq(p.uuid()))
                                .execute();
                    } catch (Throwable t) {
                        Log.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                        DiscordLogger.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                    }
                }
            }
        });
    }
}