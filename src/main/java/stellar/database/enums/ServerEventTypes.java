package stellar.database.enums;

public enum ServerEventTypes {
    START,
    STOP,
    GAMEOVER,
    NEWWAVE,
    MAPLOAD,
    ADMIN_REQUEST;

    public static ServerEventTypes parse(String text) {
        for (ServerEventTypes e : ServerEventTypes.values()) {
            if (e.name().equalsIgnoreCase(text)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase().replace("_", "-");
    }
}

