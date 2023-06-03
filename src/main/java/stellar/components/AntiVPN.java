package stellar.components;

import arc.Events;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.game.EventType;
import stellar.Variables;
import stellar.util.NetUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class AntiVPN { // also includes anti ddos from gh actions servers
    public static void load() {
        Http.get("https://api.github.com/meta", res -> {
            Jval json = Jval.read(res.getResultAsString());
            Seq<String> ghIPs = json.get("actions").asArray().as();
            Variables.blacklistedSubnets.addAll(ghIPs);
            Log.info("Fetched @ Github Actions subnets", ghIPs.size);
        }, err -> {
            Log.err("Failed to fetch Github Actions subnets", err);
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            AtomicBoolean blocked = new AtomicBoolean(false);
            Variables.blacklistedSubnets.each(subnet -> {
                if (NetUtils.isIPInSubnet(event.player.ip(), subnet)) {
                    Log.info("Closed connection to Github bot @", event.player.ip());
                    event.player.con().close();
                    blocked.set(true);
                }
            });

            if (blocked.get()) {
                return;
            }
            Log.debug("Trying to get info...");
            Http.get("http://proxycheck.io/v2/" + event.player.ip() + "?vpn=3&key=" + Variables.config.pcToken, res -> { // TODO: caching
                String resp = res.getResultAsString();
                Log.debug(resp);
                Jval json = Jval.read(resp);
                if (json.getString("status").equals("denied") || json.getString("status").equals("error")) {
                    Log.err(res.getResultAsString());
                    return;
                }
                Jval data = json.get(event.player.ip());
                if (!(data.getString("proxy").equals("no") && data.getString("vpn").equals("no"))) {
                    event.player.kick("No VPN is allowed"); // TODO: bundles
                } // maybe do check on type of ip, only allow Residential/Wireless
            }, Log::err);
        });
    }
}
