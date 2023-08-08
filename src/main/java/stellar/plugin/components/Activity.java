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
import stellar.database.DatabaseAsync;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.StatsRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.util.Bundle;
import stellar.plugin.util.logger.DiscordLogger;

import static stellar.plugin.Variables.ranks;

public class Activity {
    public static void load() {
        Timer.schedule(() -> {
            if (Const.playtimeField == null) {
                Log.err("Server @ does not exist in the Database!", Const.serverFieldName);
                DiscordLogger.err("Сервер " + Const.serverFieldName + " не существует в базе данных!");
                return;
            }

            ObjectMap<String, Rank> oldRanks = ranks.copy();
            ranks.clear();

            for (Player p : Groups.player) {
                DatabaseAsync.getContextAsync().thenComposeAsync(context -> context.update(Tables.playtime)
                        .set(Const.playtimeField, Const.playtimeField.plus(60))
                        .where(Tables.playtime.uuid.eq(p.uuid()))
                        .executeAsync()
                ).thenComposeAsync(ignored ->
                        DatabaseAsync.getContextAsync()
                ).thenAcceptAsync(context -> {
                    ObjectMap<String, Integer> stats = Variables.statsData.get(p.uuid(), new ObjectMap<>());
                    UpdateSetMoreStep<StatsRecord> query = (UpdateSetMoreStep<StatsRecord>) context.update(Tables.stats);
                    stats.each((stat, value) -> {
                        Field<Integer> field = (Field<Integer>) Tables.stats.field(stat);
                        if (field == null) {
                            Log.err("Field @ is null. UUID: @", stat, p.uuid());
                            return;
                        }

                        if (value == 0) {
                            return;
                        }

                        query.set(field, field.plus(value));
                        stats.put(stat, 0);
                    });
                    query.where(Tables.stats.uuid.eq(p.uuid()));
                }).thenComposeAsync(ignored ->
                        Rank.getRankAsync(p)
                ).thenAcceptAsync(newRank -> {
                    Rank oldRank = oldRanks.get(p.uuid());
                    ranks.put(p.uuid(), newRank);
                    if (newRank != null && oldRank != null && newRank != oldRank) {
                        Call.warningToast(p.con, Iconc.chartBar, Bundle.format("events.new-rank", Bundle.findLocale(p.locale()), newRank.formatted(p)));
                    }

                });
            }
        }, 0, 60);
    }
}
