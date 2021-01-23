/*
Decentralized chat software
Copyright (C) 2021  swissChili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
 * The key refers to a unique identifier for each item. If an item is added with a key that is already present in the
 * list, the index that held that key is updated to hold the new item. Otherwise, the key is inserted and sorted
 * according to the provided Comparator.
 *
 * @param <K> The unique key type of each item, returned by KeyGetter.getKey() and used internally in a hash table
 * @param <T> The type stored in the list
 */
public class StreamingListModel<K, T> implements ListModel<T> {
    private final List<ListDataListener> listeners = new LinkedList<>();
    private final KeyGetter<K, T> keyGetter;
    private final Comparator<T> comparator;
    private final SortDirection direction;

    private final List<SharedContainer<T>> itemList = new LinkedList<>();
    private final Map<K, SharedContainer<T>> itemMap = new Hashtable<>();

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

    public interface KeyGetter<K, T> {
        /**
         * Get a unique identifier ("key") K for a type T
         *
         * @param value the value to get the key for
         * @return the unique key for that value
         */
        K getKey(T value);
    }

    private static class SharedContainer<T> {
        T value;

        public SharedContainer(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
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
            return comparator.compare(itemList.get(index).getValue(), value) < 1;
        } else {
            return comparator.compare(itemList.get(index).getValue(), value) > -1;
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
            itemMap.get(key).setValue(value);

            for (ListDataListener listener : listeners) {
                // FIXME: use the actual index instead of the whole list range
                listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, itemList.size()));
            }

            return;
        }

        SharedContainer<T> shared = new SharedContainer<>(value);
        itemMap.put(key, shared);

        for (int i = 0; i < itemList.size(); i++) {
            if (!goesAfter(i, value)) {
                itemList.add(i, shared);

                for (ListDataListener listener : listeners) {
                    listener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, i, i));
                }

                return;
            }
        }
        itemList.add(shared);
    }

    @Override
    public int getSize() {
        return itemList.size();
    }

    @Override
    public T getElementAt(int index) {
        return itemList.get(index).getValue();
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
