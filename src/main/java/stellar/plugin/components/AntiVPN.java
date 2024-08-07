package stellar.plugin.components;

import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.NetConnection;
import mindustry.net.Packets;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import stellar.database.DatabaseAsync;
import stellar.database.gen.Tables;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.util.Players;
import stellar.plugin.util.logger.DiscordLogger;

import java.util.concurrent.CompletableFuture;

public class AntiVPN { // also includes anti ddos from gh actions servers
    private static final JsonReader jsonReader = new JsonReader();

    public static void load() {
        Vars.net.handleServer(Packets.Connect.class, (con, packet) -> {
            Events.fire(new EventType.ConnectionEvent(con));
            Seq<NetConnection> connections = Seq.with(Vars.net.getConnections()).filter(other -> other.address.equals(con.address));
            if (connections.size > Const.maxIdenticalIPs) {
                Vars.netServer.admins.blacklistDos(con.address);
                connections.each(NetConnection::close);
                Log.info("@ blacklisted because of ip spam", con.address);
            }
        });

        /* TODO: GCP and AWS
         * https://www.gstatic.com/ipranges/cloud.json
         * https://ip-ranges.amazonaws.com/ip-ranges.json
         */
        Request ghRequest = new Request.Builder()
                .url("https://api.github.com/meta")
                .build();

        try (Response response = Variables.httpClient.newCall(ghRequest).execute()) {
            JsonValue json = jsonReader.parse(response.body().string());
            for (String subnet : json.get("actions").asStringArray()) {
                if (subnet.contains(":")) {
                    return; // skipping IPv6
                }
                Variables.blacklistedSubnets.add(subnet);
            }
            Log.info("Fetched @ Github Actions subnets", Variables.blacklistedSubnets.size);

        } catch (Throwable t) {
            Log.err("Failed to fetch Github Actions subnets", t);
        }

        Events.on(EventType.PlayerConnect.class, event -> {
            if (Players.isBot(event.player)) {
                event.player.kick(Packets.KickReason.typeMismatch);
                return;
            }

            DatabaseAsync.getContextAsync().thenComposeAsync(context -> {
                boolean exists = !context
                        .selectFrom(Tables.ipCached)
                        .where(Tables.ipCached.ip.eq(event.player.ip()))
                        .fetch().isEmpty();

                if (exists) {
                    return CompletableFuture.supplyAsync(() -> context // TODO: Database.getIpInfo
                            .selectFrom(Tables.ipCached)
                            .where(Tables.ipCached.ip.eq(event.player.ip()))
                            .fetchOne());
                } else {
                    Log.debug("Trying to get info...");
                    HttpUrl url = HttpUrl.parse("http://proxycheck.io/v2/" + event.player.ip()).newBuilder()
                            .addQueryParameter("vpn", "3")
                            .addQueryParameter("risk", "2")
                            .addQueryParameter("key", Variables.config.pcToken)
                            .build();

                    Request ipRequest = new Request.Builder()
                            .url(url)
                            .build();

                    try (Response response = Variables.httpClient.newCall(ipRequest).execute()) {
                        JsonValue json = jsonReader.parse(response.body().string());
                        JsonValue data = json.get(event.player.ip());
                        return DatabaseAsync.createIpAsync(
                                event.player.ip(),
                                data.getString("proxy").equals("yes"),
                                data.getString("vpn").equals("yes"),
                                data.getString("type"),
                                data.getInt("risk", 0)
                        ).exceptionally(t -> {
                            Log.err(t);
                            DiscordLogger.err(t);
                            return null;
                        });
                    } catch (Throwable t) {
                        Log.err("Failed to get IP info", t);
                        DiscordLogger.err("Failed to get IP info", t);
                        return null;
                    }
                }
            }).thenAcceptAsync(record -> {
                if (record.isVpn() || record.isProxy()) {
                    event.player.kick("No VPN is allowed");
                }
            }).exceptionally(t -> {
                Log.err(t);
                DiscordLogger.err(t);
                return null;
            });
        });
    }
}
