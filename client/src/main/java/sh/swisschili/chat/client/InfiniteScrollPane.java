/*
Decentralized chat software
Copyright (C) 2020  swissChili

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class InfiniteScrollPane<T> extends JScrollPane {
    private final JList<T> list;

    private static final Logger LOGGER = LoggerFactory.getLogger(InfiniteScrollPane.class);
    private int numberToShowAtOnce = 20;
    private final AtomicBoolean fetchingMore = new AtomicBoolean(false);

    private BufferedLoader<T> bufferedLoader = null;
    private final ItemAddedListener listener = new ItemAddedListener() {
        private boolean isAtBottom;
        @Override
        public void beforeItemAdded() {
            int min = getVerticalScrollBar().getMinimum();
            int max = getVerticalScrollBar().getMaximum();
            int visible = getVerticalScrollBar().getVisibleAmount();
            int maxPossible = max - visible;

            LOGGER.info(String.format("Before added, scrolled to %d of %d (%d-%d)", getVerticalScrollBar().getValue(), maxPossible,
                    min, max));

            isAtBottom = getVerticalScrollBar().getValue() == maxPossible;
        }

        @Override
        public void afterItemAdded() {
            int min = getVerticalScrollBar().getMinimum();
            int max = getVerticalScrollBar().getMaximum();
            int visible = getVerticalScrollBar().getVisibleAmount();
            int maxPossible = max - visible;

            if (isAtBottom) {
                LOGGER.info("At bottom of " + maxPossible);
                getVerticalScrollBar().setValue(max);
            }
        }
    };

    public interface BufferedLoader<T> {
        /**
         * Get a range of items from some buffer
         * @param number The number of items to load (not enforced, but a suggestion)
         * @return A future for the list model containing the items
         */
        CompletableFuture<Void> loadMore(int number);
        ListModel<T> getListModel();
        void addItemAddedListener(ItemAddedListener listener);
        void removeItemAddedListener(ItemAddedListener listener);
    }

    public InfiniteScrollPane(JList<T> list) {
        super(list);
        this.list = list;
        getViewport().addChangeListener(this::viewportStateChanged);
    }

    private void getMore(boolean scrollToBottomAfter) {
        if (bufferedLoader == null)
            return;

        fetchingMore.set(true);
        LOGGER.info("Getting more...");

        // TODO: use list preferredSize instead of number of items in model
        int sizeBefore = list.getModel().getSize();

        bufferedLoader.loadMore(numberToShowAtOnce).thenAccept(ignored -> {
            fetchingMore.set(false);

            int sizeAfter = list.getModel().getSize();
            double ratio = (double)sizeBefore / (double)sizeAfter;

            if (sizeBefore == sizeAfter)
                return;

            LOGGER.info("Added more...");

            LOGGER.info(String.format("old/new ratio is %f", ratio));
            //if (scrollToBottomAfter)
            getVerticalScrollBar().setValue(getVerticalScrollBar().getMaximum());
        }).exceptionally(t -> {
            fetchingMore.set(false);
            return null;
        });
    }

    public int getNumberToShowAtOnce() {
        return numberToShowAtOnce;
    }

    public void setNumberToShowAtOnce(int numberToShowAtOnce) {
        this.numberToShowAtOnce = numberToShowAtOnce;
    }

    public void setBufferedLoader(BufferedLoader<T> bufferedLoader) {
        if (this.bufferedLoader != null)
            this.bufferedLoader.removeItemAddedListener(listener);

        this.bufferedLoader = bufferedLoader;

        this.bufferedLoader.addItemAddedListener(listener);

        list.setModel(bufferedLoader.getListModel());

        getVerticalScrollBar().setValue(getVerticalScrollBar().getMaximum());

        if (bufferedLoader.getListModel().getSize() == 0)
            getMore(true);
    }

    public void viewportStateChanged(ChangeEvent e) {
        Point viewPos = getViewport().getViewPosition();
        if (viewPos.getY() == 0) {
            getMore(false);
        }
    }
}
