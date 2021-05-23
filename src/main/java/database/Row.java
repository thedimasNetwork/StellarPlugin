package database;

import java.util.Objects;

public class Row<T>{

    public final String field;
    public final Class<T> type;

    public Row(String field, Class<T> type) {
        this.field = field;
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Row<?> row = (Row<?>)o;
        return field.equals(row.field) && type.equals(row.type);
    }

    @Override
    public int hashCode(){
        return Objects.hash(field, type);
    }

    @Override
    public String toString(){
        return "Row{" +
                "field='" + field + '\'' +
                ", type=" + type +
                '}';
    }
}
