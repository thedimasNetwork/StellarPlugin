package stellar.database.enums;

public enum PlayerEventTypes {
    JOIN,
    LEAVE,
    KICK,
    BAN,
    CHAT,
    COMMAND,
    BUILD,
    BREAK;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static PlayerEventTypes parse(String value) {
        for (PlayerEventTypes type : PlayerEventTypes.values()) {
            if (type.toString().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
