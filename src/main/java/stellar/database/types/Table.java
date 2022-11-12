package stellar.database.types;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class Table {
    String title;
    Field<String> key;
    String all;
    String allRaw;
}
