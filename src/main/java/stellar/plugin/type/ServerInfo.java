package stellar.plugin.type;

import lombok.Data;

@Data
public class ServerInfo {
    private final String id;
    private final String name;
    private final String emoji;
    private final String address;

    public String getNameFormatted() {
        return String.format("[#e6bd74]\uE829[] [#f92672]thedimas [#e6bd74]%s[#a6e22e]%s[#e6bd74]%s[]", emoji, name, emoji);
    }

    public String getDomain() {
        return address.split(":")[0];
    }

    public int getPort() {
        return Integer.parseInt(address.split(":")[1]);
    }
}
