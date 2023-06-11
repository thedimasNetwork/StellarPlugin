package types;

import lombok.Getter;
import lombok.Setter;
import mindustry.net.Packets;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.BansRecord;
import stellar.database.gen.tables.records.UsersRecord;

import java.sql.SQLException;
import java.time.LocalDateTime;

@Getter
public class AdminActionEntry {
    private final UsersRecord admin;
    private final UsersRecord target;
    private final Packets.AdminAction action;

    @Setter
    private String reason;

    @Setter
    private int until;

    public AdminActionEntry(UsersRecord admin, UsersRecord target, Packets.AdminAction action) {
        this.admin = admin;
        this.target = target;
        this.action = action;
        this.until = 0;
    }

    public void storeRecord() throws SQLException {
        BansRecord record = Database.getContext().newRecord(Tables.BANS);
        record.setAdmin(this.admin.getUuid());
        record.setTarget(this.target.getUuid());
        record.setCreated(LocalDateTime.now());
        if (until > -1) { record.setUntil(LocalDateTime.now().plusDays(until)); }
        record.setReason(reason);
        record.store();
    }
}
