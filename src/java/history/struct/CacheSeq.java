package java.history.struct;

import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Time;

import java.util.Map;
import java.util.Objects;

public class CacheSeq<T> extends Seq<T> {
    private final Queue<Map.Entry<T, Long>> writeQueue;
    private final long expireAfterWriteNanos;
    private final int maximumSize;

    CacheSeq(Seqs.SeqBuilder<? super T> builder) {
        maximumSize = builder.maximumSize;
        expireAfterWriteNanos = builder.expireAfterWriteNanos;
        writeQueue = Seqs.safeQueue();
    }

    @Override
    public void add(T e) {
        if (e == null) {
            return;
        }

        super.add(e);
        writeQueue.add(Map.entry(e, Time.nanos()));

        cleanUpBySize();
        cleanUp();
    }

    @Override
    public T get(int index) {
        cleanUp();
        return super.get(index);
    }

    @Override
    public T peek() {
        cleanUp();
        return isEmpty() ? null : super.peek();
    }

    @Override
    public T first() {
        cleanUp();
        return isEmpty() ? null : super.first();
    }

    @Override
    public boolean remove(T value) {
        int index = writeQueue.indexOf(t -> Objects.equals(t.getKey(), value));
        if (index != -1) {
            writeQueue.removeIndex(index);
        }
        return super.remove(value);
    }

    public boolean isOverflown() {
        return size >= maximumSize;
    }

    public void cleanUp() {
        Map.Entry<T, Long> entry;
        while ((entry = writeQueue.last()) != null && isExpired(entry.getValue())) {
            remove(entry.getKey());
        }
    }

    public void cleanUpBySize() {
        while (size > maximumSize) {
            remove(first());
        }
    }

    private boolean isExpired(Long time) {
        return time != null && Time.timeSinceNanos(time) >= expireAfterWriteNanos;
    }
}
