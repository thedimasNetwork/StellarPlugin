package stellar.plugin.types;

import lombok.Getter;
import lombok.Setter;
import mindustry.net.Packets;
import stellar.database.Database;
import stellar.database.gen.tables.records.UsersRecord;

import java.sql.SQLException;

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

    public void storeRecord() throws SQLException {
        Database.ban(this.getAdmin().getUuid(), this.getTarget().getUuid(), getPeriod(), getReason());
    }
}
