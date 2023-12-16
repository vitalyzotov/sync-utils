package ru.vzotov.sync;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class SyncBuilder<T, I> {

    private final List<T> source;

    private List<T> target;

    private Function<T, I> id;

    private Collection<Integer> selection;

    private SelectionConsumer selectionConsumer;

    public SyncBuilder(List<T> source) {
        this.source = source;
    }

    public static <T, I> SyncBuilder<T, I> from(List<T> source) {
        return new SyncBuilder<>(source);
    }

    public SyncBuilder<T, I> to(List<T> target) {
        this.target = target;
        return this;
    }

    public SyncBuilder<T, I> identifiedBy(Function<T, I> id) {
        this.id = id;
        return this;
    }

    public SyncBuilder<T, I> select(Collection<Integer> selection) {
        this.selection = selection;
        return this;
    }

    public SyncBuilder<T, I> select(SelectionConsumer selectionConsumer) {
        this.selectionConsumer = selectionConsumer;
        return this;
    }

    public void sync() {
        SyncUtils.adjust(target, source, id, selection, selectionConsumer);
    }
}
