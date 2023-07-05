package stellar.database;

import arc.util.Log;
import arc.util.Nullable;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mindustry.gen.Player;
import org.jooq.*;
import org.jooq.impl.DSL;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.BansRecord;
import stellar.database.gen.tables.records.UsersRecord;

import java.sql.*;
import java.time.LocalDateTime;

import static stellar.Variables.config;

public class Database {
    private static Connection connection;
    private static DSLContext context;

    private static String getConnectionUrl() {
        return "jdbc:mysql://" + config.database.ip + ":" + config.database.port + "/" + config.database.name + "?autoReconnect=true";
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(getConnectionUrl(), config.database.user, config.database.password);
            } catch (Throwable t) {
                Log.err(t);
            }
        } else if (connection.isClosed()) {
            connection = DriverManager.getConnection(getConnectionUrl(), config.database.user, config.database.password);
        }

        try { // checking if the connection is alive; may be not closed but timeout
            PreparedStatement ps = connection.prepareStatement("SELECT 1");
            ps.executeQuery();
        } catch (CommunicationsException e) {
            Log.warn("Database connection died due to inactivity");
            connection = DriverManager.getConnection(getConnectionUrl(), config.database.user, config.database.password);
        }

        return connection;
    }

    public static DSLContext getContext() throws SQLException {
        if (context == null) {
            context = DSL.using(getConnection(), SQLDialect.MYSQL);
        }
        return context;
    }

    @Nullable
    public static UsersRecord getPlayer(String uuid) throws SQLException {
        return Database.getContext()
                .selectFrom(Tables.USERS)
                .where(Tables.USERS.UUID.eq(uuid))
                .fetchOne();
    }

    @Nullable
    public static UsersRecord getPlayer(int id) throws SQLException {
        return Database.getContext()
                .selectFrom(Tables.USERS)
                .where(Tables.USERS.ID.eq(id))
                .fetchOne();
    }

    public static boolean playerExists(Player player) throws SQLException {
        return Database.getContext().fetchExists(Tables.USERS, Tables.USERS.UUID.eq(player.uuid()));
    }

    public static BansRecord latestBan(String uuid) throws SQLException {
        return Database.getContext()
                .selectFrom(Tables.BANS)
                .where(Tables.BANS.TARGET.eq(uuid))
                .orderBy(Tables.BANS.ID.desc())
                .limit(1)
                .fetchOne();
    }

    public static boolean isBanned(String uuid) throws  SQLException {
        BansRecord record = latestBan(uuid);
        if (record == null) {
            return false;
        }

        if (record.getActive() != 1) {
            return false;
        }

        if (record.getUntil() == null) {
            return true;
        }

        return !record.getUntil().isBefore(LocalDateTime.now());
    }
}
