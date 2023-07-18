package stellar.plugin.util;

import arc.util.Strings;
import org.jooq.DMLQuery;

public class NetUtils {
    public static boolean isIPInSubnet(String ipAddress, String subnet) {
        String[] subnetParts = subnet.split("/");
        String subnetAddress = subnetParts[0];

        int subnetPrefix = Strings.parseInt(subnetParts[1]);
        int ipInt = ipToInt(ipAddress);
        int subnetInt = ipToInt(subnetAddress);
        int subnetMask = (0xFFFFFFFF << (32 - subnetPrefix));

        return (ipInt & subnetMask) == (subnetInt & subnetMask);
    }

    public static int ipToInt(String ipAddress) {
        String[] ipParts = ipAddress.split("\\.");
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8 | Strings.parseInt(ipParts[i]);
        }
        return result;
    }

    /** Use this to put all DML queries into separate thread to prevent hanging **/
    public static void updateBackground(DMLQuery<?> step) {
        new Thread(step::execute).start();
    }
}
