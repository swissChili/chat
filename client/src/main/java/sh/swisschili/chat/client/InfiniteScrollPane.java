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

import com.github.weisj.darklaf.theme.info.FontSizeRule;
import org.intellij.lang.annotations.JdkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.util.List;
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
                getVerticalScrollBar().setValue(maxPossible);
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
        getVerticalScrollBar().addAdjustmentListener(this::onScroll);
    }

    private void onScroll(AdjustmentEvent e) {
//        if (e.getValueIsAdjusting())
//            return;

        LOGGER.info("Adjustment type is " + e.getAdjustmentType());

        int max = e.getAdjustable().getMaximum();
        int min = e.getAdjustable().getMinimum();
        int visible = e.getAdjustable().getVisibleAmount();
        int current = e.getValue();
        int maxPossible = max - visible;

        //LOGGER.info(String.format("Scrolled to %d in range %d-%d (%d)", current, min, max, visible));

        if (fetchingMore.get()) {
            LOGGER.info("Already fetching more");
            return;
        }
        if (current == min) {
            getMore();
        }
    }

    private void getMore() {
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

            int max = getVerticalScrollBar().getMaximum();
            int min = getVerticalScrollBar().getMinimum();
            int visible = getVerticalScrollBar().getVisibleAmount();
            int current = getVerticalScrollBar().getValue();
            int maxPossible = max - visible;
            int newPosition = maxPossible - (int)((maxPossible - min) * ratio);

            LOGGER.info(String.format("old/new ratio is %f, from %d to %d", ratio, sizeBefore, sizeAfter));
            getVerticalScrollBar().setValue(max);
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
        if (bufferedLoader.getListModel().getSize() == 0)
            getMore();

        getVerticalScrollBar().setValue(getVerticalScrollBar().getMaximum());
    }
}
