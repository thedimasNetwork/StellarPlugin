package stellar.plugin.components.history.struct;

import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Time;

import java.util.Objects;

public class CacheSeq<T> extends Seq<T> {
    private final Queue<ValueReference<T>> writeQueue;
    private final long expireAfterWriteNanos;
    private final int maximumSize;

    CacheSeq(Seqs.SeqBuilder<? super T> builder) {
        maximumSize = builder.maximumSize;
        expireAfterWriteNanos = builder.expireAfterWriteNanos;
        writeQueue = Seqs.safeQueue();
    }

    @Override
    public Seq<T> add(T e) {
        if (e == null) {
            return null;
        }

        super.add(e);
        writeQueue.add(new ValueReference<>(Time.nanos(), e));

        cleanUpBySize();
        cleanUp();
        return this;
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
        int index = writeQueue.indexOf(t -> Objects.equals(t.value, value));
        if (index != -1) {
            writeQueue.removeIndex(index);
        }
        return super.remove(value);
    }

    public boolean isOverflown() {
        return size >= maximumSize;
    }

    public void cleanUp() {
        ValueReference<T> valueReference;
        while ((valueReference = writeQueue.last()) != null && isExpired(valueReference.writeTime)) {
            remove(valueReference.value);
        }
    }

    public void cleanUpBySize() {
        while (size > maximumSize) {
            remove(first());
        }
    }

    private boolean isExpired(long time) {
        return Time.timeSinceNanos(time) >= expireAfterWriteNanos;
    }

    static class ValueReference<T> {
        private final long writeTime;
        private final T value;

        public ValueReference(long writeTime, T value) {
            this.writeTime = writeTime;
            this.value = value;
        }
    }
}
