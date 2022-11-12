package stellar.components;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import stellar.Const;
import stellar.database.DBHandler;
import stellar.database.tables.Tables;
import stellar.util.logger.DiscordLogger;

import static stellar.Variables.interval;

public class Playtime {
    public static void load() {
        // region обновление плейтайма
        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(1, 3600)) { // 1 минута
                if (Tables.playtime.getFields().containsKey(Const.SERVER_COLUMN_NAME)) {
                    for (Player p : Groups.player) {
                        try {
                            Long time = DBHandler.get(p.uuid(), Tables.playtime.getFields().get(Const.SERVER_COLUMN_NAME), Tables.playtime);
                            if (time == null) {
                                Log.err("Player '" + p.uuid() + "' doesn't exists");
                                DiscordLogger.err("Player '" + p.uuid() + "' doesn't exists");
                            }
                            long computed = (time != null ? time : 0) + 60;
                            DBHandler.update(p.uuid(), Tables.playtime.getFields().get(Const.SERVER_COLUMN_NAME), Tables.playtime, computed);
                        } catch (Throwable t) {
                            Log.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                            DiscordLogger.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                        }
                    }
                } else {
                    Log.err("Сервер @ не существует в базе данных!", Const.SERVER_COLUMN_NAME);
                    DiscordLogger.err("Сервер " + Const.SERVER_COLUMN_NAME + " не существует в базе данных!");
                }
            }
        });
        // endregion
    }
}
