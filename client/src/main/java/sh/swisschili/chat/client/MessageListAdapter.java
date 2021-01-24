package sh.swisschili.chat.client;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MessageListAdapter extends ComponentAdapter {
    private final JList<?> list;

    public MessageListAdapter(JList<?> list) {
        this.list = list;
    }

    @Override
    public void componentResized(ComponentEvent e) {
        // Invalidate cache
        list.setFixedCellHeight(10);
        list.setFixedCellHeight(-1);
    }
}
