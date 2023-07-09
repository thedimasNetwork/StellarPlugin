package stellar.plugin.components;

import arc.Events;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.NetConnection;
import mindustry.net.Packets;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.database.Database;
import stellar.database.gen.Tables;
import stellar.database.gen.tables.records.IpCachedRecord;
import stellar.plugin.util.Players;

import java.sql.SQLException;

public class AntiVPN { // also includes anti ddos from gh actions servers
    public static void load() {
        Vars.net.handleServer(Packets.Connect.class, (con, packet) -> {
            Events.fire(new EventType.ConnectionEvent(con));
            Seq<NetConnection> connections = Seq.with(Vars.net.getConnections()).filter(other -> other.address.equals(con.address));
            if (connections.size >= Const.MAX_IDENTICAL_IPS) {
                Vars.netServer.admins.blacklistDos(con.address);
                connections.each(NetConnection::close);
                Log.info("@ blacklisted because of ip spam", con.address);
            }
        });

        /* TODO: GCP and AWS
         * https://www.gstatic.com/ipranges/cloud.json
         * https://ip-ranges.amazonaws.com/ip-ranges.json
         */
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
                    if (record.getProxy() == 1 || record.getVpn() == 1) {
                        event.player.kick("No VPN is allowed");
                    }
                } catch (SQLException e) {
                    Log.err(e);
                }
            } else {
                Log.debug("Trying to get info...");
                String url = "http://proxycheck.io/v2/" + event.player.ip() + "?vpn=3&risk=2&key=" + Variables.config.pcToken;
                Http.get(url, res -> { // TODO: caching
                    String resp = res.getResultAsString();
                    Log.debug(resp);
                    Jval json = Jval.read(resp);


                    if (json.getString("status").equals("denied") || json.getString("status").equals("error")) {
                        Log.err(resp);
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
