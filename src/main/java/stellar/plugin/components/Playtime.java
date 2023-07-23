package stellar.plugin.components;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import stellar.plugin.Const;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.plugin.util.logger.DiscordLogger;

import static stellar.plugin.Variables.interval;
import static stellar.plugin.util.NetUtils.updateBackground;

public class Playtime {
    public static void load() {
        Events.run(EventType.Trigger.update, () -> {
            if (interval.get(1, 3600)) { // 1 minute
                Log.debug(Const.serverFieldName);
                if (Const.playtimeField == null) {
                    Log.err("Server @ does not exist in the Database!", Const.serverFieldName);
                    DiscordLogger.err("Сервер " + Const.serverFieldName + " не существует в базе данных!");
                    return;
                }
                for (Player p : Groups.player) {
                    try {
                        long computed = Database.getPlaytime(p.uuid(), Const.playtimeField) + 60;
                        updateBackground(Database.getContext() // TODO: Database.updatePlaytime
                                .update(Tables.playtime)
                                .set(Const.playtimeField, computed)
                                .where(Tables.playtime.uuid.eq(p.uuid())));
                    } catch (Throwable t) {
                        Log.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                        DiscordLogger.err("Failed to update playtime for player '" + p.uuid() + "'", t);
                    }
                }
            }
        });
    }
}
