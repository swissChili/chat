package sh.swisschili.chat.client;

import com.github.weisj.darklaf.components.border.DarkBorders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.NotNull;
import sh.swisschili.chat.util.ChatProtos.Message;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class MessageCell implements ListCellRenderer<Message> {
    private JPanel rootPanel;
    private JTextArea body;
    private JLabel sender;
    private JLabel time;

    public MessageCell() {
    }

    public MessageCell(@NotNull Message value) {
        $$$setupUI$$$();

        body.setText(value.getBody());
        body.setLineWrap(true);
        body.setWrapStyleWord(true);

        sender.setText(value.getSender().getName());
        time.setText(new Date(value.getUnixTime()).toString());
        Font f = sender.getFont();
        sender.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

        rootPanel.setBorder(DarkBorders.createLineBorder(2, 0, 0, 0));
    }

    @Override
    public JComponent getListCellRendererComponent(JList<? extends Message> list, Message value, int index,
                                                   boolean isSelected, boolean cellHasFocus) {
        return new MessageCell(value).rootPanel;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 5));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel1.setEnabled(true);
        rootPanel.add(panel1, BorderLayout.NORTH);
        sender = new JLabel();
        sender.setText("Sender");
        CellConstraints cc = new CellConstraints();
        panel1.add(sender, cc.xy(1, 1));
        time = new JLabel();
        Font timeFont = this.$$$getFont$$$(null, -1, -1, time.getFont());
        if (timeFont != null) time.setFont(timeFont);
        time.setText("Time");
        panel1.add(time, cc.xy(3, 1));
        body = new JTextArea();
        body.setEditable(true);
        body.setLineWrap(true);
        rootPanel.add(body, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
