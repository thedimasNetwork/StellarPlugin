package stellar.database.enums;

import lombok.Getter;

public enum ServerEventTypes {
    START,
    STOP,
    GAMEOVER,
    NEWWAVE,
    MAPLOAD,
    ADMIN_REQUEST;

    @Override
    public String toString() {
        return this.name().toLowerCase().replace("_", "-");
    }

    public static ServerEventTypes parse(String text) {
        for (ServerEventTypes e : ServerEventTypes.values()) {
            if (e.name().equalsIgnoreCase(text)) {
                return e;
            }
        }
        return null;
    }
}
