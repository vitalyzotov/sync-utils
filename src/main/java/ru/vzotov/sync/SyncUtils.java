package ru.vzotov.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class SyncUtils {


    /**
     * <p>
     * Synchronizes the target list.
     * </p>
     * <p>
     * Items missing from the source are removed from the target list.
     * Items with matching identifiers are replaced in the target list.
     * </p>
     * <p>
     * New items missing from the target list are added to the end of the target list.
     * </p>
     *
     * @param target            the target list
     * @param source            the source list
     * @param id                function that calculates the item identifier
     * @param selection         current selection
     * @param selectionConsumer new selection handler
     * @param <T>               type of items
     * @param <I>               type of identifiers
     */
    public static <T, I> void sync(List<T> target, Collection<T> source, Function<T, I> id,
                                   Collection<Integer> selection, SelectionConsumer selectionConsumer) {
        sync(target, source, id, t -> target.size(), selection, selectionConsumer);
    }

    /**
     * <p>
     * Synchronizes the target list.
     * </p>
     * <p>
     * Items missing from the source are removed from the target list.
     * Items with matching identifiers are replaced in the target list.
     * </p>
     * <p>
     * New items missing from the target list are added according to the position in the source.
     * </p>
     *
     * @param target            the target list
     * @param source            the source list
     * @param id                function that calculates the item identifier
     * @param selection         current selection
     * @param selectionConsumer new selection handler
     * @param <T>               type of items
     * @param <I>               type of identifiers
     */
    public static <T, I> void adjust(List<T> target, Collection<T> source, Function<T, I> id,
                                     Collection<Integer> selection, SelectionConsumer selectionConsumer) {
        sync(target, source, id, null, selection, selectionConsumer);
    }

    /**
     * <p>
     * Synchronizes the target list.
     * </p>
     * <p>
     * Items missing from the source are removed from the target list.
     * Items with matching identifiers are replaced in the target list.
     * </p>
     * <p>
     * New items that are not in the target list are added to the list.
     * The insertion position is determined using the {@code index} function.
     * If {@code index} is {@code null}, the source list is used to determine the insertion position.
     * </p>
     *
     * @param target            Target list for synchronization. The method will update records in it according to specified parameters.
     * @param source            Data source.
     * @param id                function that calculates the item identifier
     * @param index             Function that calculates the index for inserting new records
     * @param selection         current selection
     * @param selectionConsumer new selection handler
     * @param <T>               type of items
     * @param <I>               type of identifiers
     */
    public static <T, I> void sync(List<T> target, Collection<T> source, Function<T, I> id, ToIntFunction<T> index,
                                   Collection<Integer> selection, SelectionConsumer selectionConsumer) {
        List<Integer> newSelection = new ArrayList<>(selection.size());
        Set<Integer> selected = new LinkedHashSet<>(selection);

        List<T> toInsert = new LinkedList<>();
        // calculate item identifiers in the data source
        Set<I> sourceKeys = source.stream().map(id).collect(Collectors.toSet());

        // cache storing indexes of items in the target list
        Map<I, Integer> targetIndices = new HashMap<>();
        // cache storing indexes of new records in the source collection
        Map<I, Integer> sourceIndices = new HashMap<>();

        //
        // Iteration 1 - deleting items
        // calculate new indexes of items in the target list, delete items missing in the source
        {
            int i = 0, j = 0; // i - new indices, j - old indices
            for (Iterator<T> iterator = target.listIterator(); iterator.hasNext(); j++) {
                T e = iterator.next();
                final I key = id.apply(e);
                if (sourceKeys.contains(key)) {
                    // the source contains this item. Index it.
                    if (selected.contains(j)) newSelection.add(i);
                    targetIndices.put(key, i++);
                } else {
                    // the item is missing from the source. Delete it.
                    iterator.remove();
                    selected.remove(j);
                }
            }
        }

        //
        // Iteration 2 - updating the elements
        // If the item is in the target list, updates it.
        // If the item is not in the target list, it memorizes it for later insertion.
        //
        {
            int i = 0;
            for (Iterator<T> iterator = source.iterator(); iterator.hasNext(); i++) {
                T e = iterator.next();
                final I key = id.apply(e);
                Integer idx = targetIndices.get(key);
                if (idx == null) {
                    toInsert.add(e);
                    if (index == null) sourceIndices.put(key, i);
                } else {
                    target.set(idx, e);
                }
            }
        }

        //
        // Iteration 3 - Insertion
        // When inserting, modify the selection
        //
        for (T e : toInsert) {
            int idx = index == null ? sourceIndices.get(id.apply(e)) : index.applyAsInt(e);
            target.add(idx, e);
            for (ListIterator<Integer> iterator = newSelection.listIterator(); iterator.hasNext(); ) {
                Integer s = iterator.next();
                if (s >= idx) {
                    iterator.set(s + 1);
                }
            }
        }


        int n = newSelection.size();
        if (n > 0) {
            selectionConsumer.select(newSelection.get(0),
                    n > 1 ? newSelection.subList(1, n).stream().mapToInt(Integer::intValue).toArray() : new int[0]);
        }
    }
}
