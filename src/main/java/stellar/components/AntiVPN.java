package stellar.components;

import arc.Events;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.game.EventType;
import stellar.Variables;

public class AntiVPN { // also includes anti ddos from gh actions servers
    public static void load() {
        Events.on(EventType.PlayerConnect.class, event -> {
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
                    event.player.kick("No VPN is allowed"); // TODO: bundles...
                } // maybe do check on type of ip, only allow Residental/Wireless
            }, Log::err);
        });
    }
}
