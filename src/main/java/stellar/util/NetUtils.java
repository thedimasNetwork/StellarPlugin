package stellar.util;

import arc.util.Strings;

public class NetUtils {
    public static boolean isIPInSubnet(String ipAddress, String subnet) {
        String[] subnetParts = subnet.split("/");
        String subnetAddress = subnetParts[0];
        int subnetPrefix = Strings.parseInt(subnetParts[1]);

        // Convert IP address and subnet address to integer representation
        int ipInt = ipToInt(ipAddress);
        int subnetInt = ipToInt(subnetAddress);

        // Calculate the subnet mask
        int subnetMask = (0xFFFFFFFF << (32 - subnetPrefix));

        // Check if the IP address is within the subnet
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
}
