package types;

import lombok.Getter;
import lombok.Setter;
import mindustry.net.Packets;
import stellar.database.gen.tables.records.UsersRecord;

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
}
