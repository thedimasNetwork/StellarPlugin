/*
 * This file is generated by jOOQ.
 */
package stellar.database.gen;


import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

import stellar.database.gen.tables.IpCached;
import stellar.database.gen.tables.PlayerEvents;
import stellar.database.gen.tables.Playtime;
import stellar.database.gen.tables.ServerEvents;
import stellar.database.gen.tables.Users;
import stellar.database.gen.tables.Warns;
import stellar.database.gen.tables.records.IpCachedRecord;
import stellar.database.gen.tables.records.PlayerEventsRecord;
import stellar.database.gen.tables.records.PlaytimeRecord;
import stellar.database.gen.tables.records.ServerEventsRecord;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.database.gen.tables.records.WarnsRecord;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * mindustry.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<IpCachedRecord> KEY_IP_CACHED_PRIMARY = Internal.createUniqueKey(IpCached.IP_CACHED, DSL.name("KEY_ip_cached_PRIMARY"), new TableField[] { IpCached.IP_CACHED.ID }, true);
    public static final UniqueKey<IpCachedRecord> KEY_IP_CACHED_SECONDARY = Internal.createUniqueKey(IpCached.IP_CACHED, DSL.name("KEY_ip_cached_SECONDARY"), new TableField[] { IpCached.IP_CACHED.IP }, true);
    public static final UniqueKey<PlayerEventsRecord> KEY_PLAYER_EVENTS_PRIMARY = Internal.createUniqueKey(PlayerEvents.PLAYER_EVENTS, DSL.name("KEY_player_events_PRIMARY"), new TableField[] { PlayerEvents.PLAYER_EVENTS.ID }, true);
    public static final UniqueKey<PlaytimeRecord> KEY_PLAYTIME_PRIMARY = Internal.createUniqueKey(Playtime.PLAYTIME, DSL.name("KEY_playtime_PRIMARY"), new TableField[] { Playtime.PLAYTIME.UUID }, true);
    public static final UniqueKey<ServerEventsRecord> KEY_SERVER_EVENTS_PRIMARY = Internal.createUniqueKey(ServerEvents.SERVER_EVENTS, DSL.name("KEY_server_events_PRIMARY"), new TableField[] { ServerEvents.SERVER_EVENTS.ID }, true);
    public static final UniqueKey<UsersRecord> KEY_USERS_PRIMARY = Internal.createUniqueKey(Users.USERS, DSL.name("KEY_users_PRIMARY"), new TableField[] { Users.USERS.UUID }, true);
    public static final UniqueKey<WarnsRecord> KEY_WARNS_PRIMARY = Internal.createUniqueKey(Warns.WARNS, DSL.name("KEY_warns_PRIMARY"), new TableField[] { Warns.WARNS.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<WarnsRecord, UsersRecord> ADMIN_UUID_FKEY = Internal.createForeignKey(Warns.WARNS, DSL.name("admin_uuid_fkey"), new TableField[] { Warns.WARNS.ADMIN_UUID }, Keys.KEY_USERS_PRIMARY, new TableField[] { Users.USERS.UUID }, true);
    public static final ForeignKey<WarnsRecord, UsersRecord> TARGET_UUID_FKEY = Internal.createForeignKey(Warns.WARNS, DSL.name("target_uuid_fkey"), new TableField[] { Warns.WARNS.TARGET_UUID }, Keys.KEY_USERS_PRIMARY, new TableField[] { Users.USERS.UUID }, true);
}
