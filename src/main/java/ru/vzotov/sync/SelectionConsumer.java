package ru.vzotov.sync;

@FunctionalInterface
public interface SelectionConsumer {
    void select(int index, int ... indices);
}
