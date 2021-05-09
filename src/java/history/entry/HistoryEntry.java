package java.history.entry;

import java.util.concurrent.TimeUnit;

public interface HistoryEntry{

    String getMessage();

    long getLastAccessTime(TimeUnit unit);
}
