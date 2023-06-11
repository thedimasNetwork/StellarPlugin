/*
 * This file is generated by jOOQ.
 */
package stellar.database.gen;


import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import stellar.database.gen.tables.Bans;
import stellar.database.gen.tables.IpCached;
import stellar.database.gen.tables.PlayerEvents;
import stellar.database.gen.tables.Playtime;
import stellar.database.gen.tables.ServerEvents;
import stellar.database.gen.tables.Users;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Mindustry extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>mindustry</code>
     */
    public static final Mindustry MINDUSTRY = new Mindustry();

    /**
     * The table <code>mindustry.bans</code>.
     */
    public final Bans BANS = Bans.BANS;

    /**
     * The table <code>mindustry.ip_cached</code>.
     */
    public final IpCached IP_CACHED = IpCached.IP_CACHED;

    /**
     * The table <code>mindustry.player_events</code>.
     */
    public final PlayerEvents PLAYER_EVENTS = PlayerEvents.PLAYER_EVENTS;

    /**
     * The table <code>mindustry.playtime</code>.
     */
    public final Playtime PLAYTIME = Playtime.PLAYTIME;

    /**
     * The table <code>mindustry.server_events</code>.
     */
    public final ServerEvents SERVER_EVENTS = ServerEvents.SERVER_EVENTS;

    /**
     * The table <code>mindustry.users</code>.
     */
    public final Users USERS = Users.USERS;

    /**
     * No further instances allowed
     */
    private Mindustry() {
        super("mindustry", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Bans.BANS,
            IpCached.IP_CACHED,
            PlayerEvents.PLAYER_EVENTS,
            Playtime.PLAYTIME,
            ServerEvents.SERVER_EVENTS,
            Users.USERS
        );
    }
}
