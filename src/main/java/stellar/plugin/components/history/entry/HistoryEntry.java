package stellar.plugin.components.history.entry;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public interface HistoryEntry {

    String getMessage(Locale locale);

    long getTimestamp(TimeUnit unit);
}
