package database;

import java.util.Objects;

public class Field<T>{

    private final String name;
    private final Class<T> type;

    public Field(String field, Class<T> type) {
        this.name = field;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Field<?> row = (Field<?>)o;
        return this.name.equals(row.name) && type.equals(row.type);
    }

    @Override
    public int hashCode(){
        return Objects.hash(name, type);
    }

    @Override
    public String toString(){
        return "Field{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
