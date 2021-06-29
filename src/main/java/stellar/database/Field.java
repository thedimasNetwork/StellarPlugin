package stellar.database;

import java.util.Objects;

public class Field<T> {

    private final String name;
    private final Class<T> type;
    private final String table;

    public Field(String field, Class<T> type, String table) {
        this.name = field;
        this.type = type;
        this.table = table;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public String getTable() {
        return table;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Field<?> row = (Field<?>) o;
        return this.name.equals(row.name) && type.equals(row.type) && table.equals(row.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, type);
    }

    @Override
    public String toString() {
        return name;
    }
}
