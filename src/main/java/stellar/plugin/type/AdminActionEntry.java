package stellar.plugin.type;

import arc.util.Log;
import lombok.Getter;
import lombok.Setter;
import mindustry.net.Packets;
import stellar.database.DatabaseAsync;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.util.logger.DiscordLogger;

@Getter
public class AdminActionEntry {
    private final UsersRecord admin;
    private final UsersRecord target;
    private final Packets.AdminAction action;

    @Setter
    private String reason;

    @Setter
    private int period;

    public AdminActionEntry(UsersRecord admin, UsersRecord target, Packets.AdminAction action) {
        this.admin = admin;
        this.target = target;
        this.action = action;
        this.period = 0;
    }

    public void storeRecord() {
        DatabaseAsync.banAsync(
                this.getAdmin().getUuid(), this.getTarget().getUuid(), getPeriod(), getReason()
        ).exceptionally(t -> {
            Log.err(t);
            DiscordLogger.err(t);
            return null;
        });
    }
}
