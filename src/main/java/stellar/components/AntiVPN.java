package stellar.components;

import arc.Events;
import arc.net.DcReason;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.game.EventType;
import mindustry.net.Packets;
import stellar.Variables;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.IpCachedRecord;
import stellar.util.NetUtils;
import stellar.util.Players;

import java.sql.SQLException;

public class AntiVPN { // also includes anti ddos from gh actions servers
    public static void load() {
        Http.get("https://api.github.com/meta", res -> {
            Jval json = Jval.read(res.getResultAsString());
            json.get("actions").asArray().each(subnet -> {
                if (subnet.asString().contains(":")) {
                    return; // skipping IPv6
                }
                Variables.blacklistedSubnets.add(subnet.asString());
            });
            Log.info("Fetched @ Github Actions subnets", Variables.blacklistedSubnets.size);
        }, err -> {
            Log.err("Failed to fetch Github Actions subnets", err);
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            if (Players.isBot(event.player)) {
                event.player.kick(Packets.KickReason.typeMismatch);
                return;
            }

            boolean exists = false;

            try {
                exists = Database.getContext()
                        .selectFrom(Tables.IP_CACHED)
                        .where(Tables.IP_CACHED.IP.eq(event.player.ip()))
                        .fetch()
                        .size() > 0;
            } catch (SQLException e) {
                Log.err(e);
            }
            if (exists) {
                try {
                    IpCachedRecord record = Database.getContext()
                            .selectFrom(Tables.IP_CACHED)
                            .where(Tables.IP_CACHED.IP.eq(event.player.ip()))
                            .fetchOne();
                    if (record.getProxy() == 1 || record.getProxy() == 1) {
                        event.player.kick("No VPN is allowed");
                    }
                } catch (SQLException e) {
                    Log.err(e);
                }
            } else {
                Log.debug("Trying to get info...");
                Http.get("http://proxycheck.io/v2/" + event.player.ip() + "?vpn=3&risk=2&key=" + Variables.config.pcToken, res -> { // TODO: caching
                    String resp = res.getResultAsString();
                    Log.debug(resp);
                    Jval json = Jval.read(resp);


                    if (json.getString("status").equals("denied") || json.getString("status").equals("error")) {
                        Log.err(res.getResultAsString());
                        return;
                    }

                    Jval data = json.get(event.player.ip());

                    IpCachedRecord record = Database.getContext().newRecord(Tables.IP_CACHED);
                    record.setIp(event.player.ip());
                    record.setProxy((byte) (data.getString("proxy").equals("yes") ? 1 : 0));
                    record.setVpn((byte) (data.getString("vpn").equals("yes") ? 1 : 0));
                    record.setType(data.getString("type"));
                    record.setRisk((short) data.getInt("risk", 0));
                    record.store();

                    if (!(data.getString("proxy").equals("no") && data.getString("vpn").equals("no"))) {
                        event.player.kick("No VPN is allowed"); // TODO: bundles
                    } // maybe do check on type of ip, only allow Residential/Wireless
                }, Log::err);
            }
        });
    }
}
