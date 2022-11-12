package stellar.database.types;

public abstract class Entry {
    public Table table;

    @Override
    public String toString() {
        return super.toString();
    }

    public static Entry fromString(String content) {
        return null;
    }

    public Entry(Object... args) {

    }
}
