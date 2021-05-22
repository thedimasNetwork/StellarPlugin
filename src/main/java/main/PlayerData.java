package main;

public class PlayerData {
    public String uuid;
    public String ip;
    public String name;
    public String locale;
    public String translator;
    public String playTime;
    public boolean admin;
    public boolean banned;

    public PlayerData() {
        translator = "off";
        playTime = "0";
        banned = false;
    }

    public PlayerData(String uuid, String ip, String name, String locale, String translator, String playTime, boolean admin, boolean banned) {
        this.uuid = uuid;
        this.ip = ip;
        this.name = name;
        this.locale = locale;
        this.translator = translator;
        this.playTime = playTime;
        this.admin = admin;
        this.banned = banned;
    }
}
