package sh.swisschili.chat.client;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.*;

/**
 * List model for use in streaming scenarios, ie: where items are transmitted over a network with possible delays.
 * This class incrementally sorts items as they are added and supports O(1) updates of already existing items. It
 * operates under the assumption that the criteria on which items are sorted will not change.
 *
 * @param <K> The unique key type of each item, returned by KeyGetter.getKey() and used internally in a hash table
 * @param <T> The type stored in the list
 */
public class StreamingListModel<K, T> implements ListModel<T> {
    private final List<ListDataListener> listeners = new LinkedList<>();
    private final KeyGetter<K, T> keyGetter;
    private final Comparator<T> comparator;
    private final SortDirection direction;

    private final List<T> itemList = new LinkedList<>();
    private final Map<K, T> itemMap = new Hashtable<>();

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

    public interface KeyGetter<K, T> {
        /**
         * Get a unique identifier ("key") K for a type T
         *
         * @param value the value to get the type for
         * @return the unique key for that type
         */
        K getKey(T value);
    }

    /**
     * @param keyGetter  Used to get a unique, indexable key for each value
     * @param comparator Used to sort items
     * @param direction  The direction in which to sort items
     */
    public StreamingListModel(KeyGetter<K, T> keyGetter, Comparator<T> comparator, SortDirection direction) {
        this.keyGetter = keyGetter;
        this.comparator = comparator;
        this.direction = direction;
    }

    private boolean goesAfter(int index, T value) {
        if (direction.equals(SortDirection.ASCENDING)) {
            return comparator.compare(itemList.get(index), value) < 1;
        } else {
            return comparator.compare(itemList.get(index), value) > -1;
        }
    }

    /**
     * Add a given value to the list, if its key (got from the KeyGetter) is not unique, the original value with that
     * key will instead be replaced with the value provided.
     *
     * @param value The value to add
     */
    public void add(T value) {
        K key = keyGetter.getKey(value);
        if (itemMap.containsKey(key)) {
            itemMap.put(key, value);
            int index = itemList.indexOf(value);

            for (ListDataListener listener : listeners) {
                listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index));
            }

            return;
        }
        itemMap.put(key, value);

        for (int i = 0; i < itemList.size(); i++) {
            if (!goesAfter(i, value)) {
                itemList.add(i, value);

                for (ListDataListener listener : listeners) {
                    listener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, i, i));
                }

                return;
            }
        }
        itemList.add(value);
    }

    @Override
    public int getSize() {
        return itemList.size();
    }

    @Override
    public T getElementAt(int index) {
        return itemList.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }
}
