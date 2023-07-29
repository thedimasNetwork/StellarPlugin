package stellar.plugin.components;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Timer;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import org.jooq.Field;
import org.jooq.UpdateSetMoreStep;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.StatsRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.logger.DiscordLogger;

import java.sql.SQLException;

import static stellar.plugin.Variables.ranks;
import static stellar.plugin.util.NetUtils.updateBackground;

public class Activity {
    public static void load() {
        Timer.schedule(() -> {
            new Thread(() -> {
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

                Variables.statsData.each((uuid, stats) -> {
                    try {
                        UpdateSetMoreStep<StatsRecord> query = (UpdateSetMoreStep<StatsRecord>) Database.getContext()
                                .update(Tables.stats); // omg... looks bad
                        stats.each((name, value) -> {
                            Field<Integer> field = (Field<Integer>) Tables.stats.field(name);
                            if (field == null) {
                                Log.err("Field @ is null. UUID: @", name, uuid);
                                return;
                            }

                            if (value == 0) {
                                return;
                            }

                            query.set(field, field.plus(value));
                            stats.put(name, 0);
                        });
                        query.where(Tables.stats.uuid.eq(uuid))
                                .execute(); // don't run in the background as rank updating should be on fresh data

                    } catch (SQLException e) {
                        Log.err(e);
                    }

                    if (!Groups.player.contains(p -> p.uuid().equals(uuid))) {
                        Variables.statsData.remove(uuid);
                    }
                });

                ObjectMap<String, Rank> oldRanks = ranks.copy();
                ranks.clear();
                Groups.player.each(player -> {
                    Rank oldRank = oldRanks.get(player.uuid());
                    Rank newRank = oldRank;

                    try {
                        newRank = Rank.getRank(player);
                    } catch (SQLException e) {
                        Log.err(e);
                        ranks.put(player.uuid(), oldRanks.get(player.uuid()));
                    }

                    if (newRank != null && oldRank != null && newRank != oldRank) {
                        Call.warningToast(player.con, Iconc.chartBar, Bundle.format("events.new-rank", Bundle.findLocale(player.locale()), newRank.formatted(player)));
                    }
                });
            }).start();
        }, 0, 60);
    }
}
