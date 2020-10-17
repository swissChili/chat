package sh.swisschili.chat.client;

import com.github.weisj.darklaf.LafManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.ChatProtos.Message;
import sh.swisschili.chat.util.ChatProtos.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

public class MainWindow {
    private JPanel panel1;
    private JButton sendButton;
    private JTextField messageField;
    private JList<Message> messages;
    private JList<ServerGroup> groups;
    private JList<ServerChannel> channels;
    private JList<User> users;
    private JScrollPane messagesScrollPane;
    private JPanel leftPanel;
    private JButton settingsButton;
    private JPanel userPanel;

    private JFrame frame;

    private final UserComponent userComponent = new UserComponent();

    private final DefaultListModel<ServerGroup> groupModel = new DefaultListModel<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class.getName());

    private ServerChannel currentChannel = null;
    private final ServerPool pool = new ServerPool();
    private UserCredentials credentials;

    private static final Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);

    private void createUIComponents() {
        userPanel = userComponent.getRootPanel();
    }

    private static class GroupsPopUp extends JPopupMenu {
        JMenuItem addGroup;

        public GroupsPopUp(ActionListener addGroupListener) {
            addGroup = new JMenuItem("Add group");
            addGroup.addActionListener(addGroupListener);
            add(addGroup);
        }
    }

    public MainWindow() {
        $$$setupUI$$$();

        groups.setComponentPopupMenu(new GroupsPopUp(e -> new AddGroupDialog(this::groupAdded)
                .setVisible(true)));

        final ActionListener sendActionListener = e -> sendMessage();

        messages.setSelectionModel(new NoSelectionModel());
        messagesScrollPane.setHorizontalScrollBar(null);

        leftPanel.setMinimumSize(new Dimension(320, 600));

        sendButton.addActionListener(sendActionListener);
        messageField.addActionListener(sendActionListener);
        messages.setCellRenderer(new MessageCell());

        groups.setModel(groupModel);
        groups.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        groups.addListSelectionListener(e -> groupSelected());

        channels.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        channels.addListSelectionListener(e -> {
            ServerChannel channel = channels.getSelectedValue();
            if (channel == null)
                return;

            LOGGER.info("Channel selected " + channel);

            messages.setModel(channel.getMessageModel());
            currentChannel = channel;
        });

        settingsButton.addActionListener(e -> {
            new SettingsDialog(frame).setVisible(true);
        });
    }

    private void groupAdded(String groupName, String server) {
        LOGGER.info(String.format("Group added: %s#%s", groupName, server));

        groupModel.addElement(new ServerGroup(pool, server, groupName, this::onError, groupChannels ->
                SwingUtilities.invokeLater(this::groupSelected)));
    }

    private void groupSelected() {
        ServerGroup selected = groups.getSelectedValue();
        if (selected == null)
            return;

        LOGGER.info("Group selected " + selected);

        channels.setModel(selected.getModel());
        channels.setSelectedIndex(0);
    }

    private void onError(Throwable t) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(panel1, t.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    protected void sendMessage() {
        if (messageField.getText().isBlank() || currentChannel == null)
            return;

        LOGGER.info("Message sent " + messageField.getText());

        User.Builder userBuilder = User.newBuilder().setName("asdf").setId("0");

        Message message = Message.newBuilder()
                .setBody(messageField.getText())
                .setSender(userBuilder.build())
                .setUnixTime(System.currentTimeMillis())
                .build();

        messageField.setText("");

        LOGGER.info("Sending message to server");

        currentChannel.sendMessage(message);
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public static void main(String[] args) {
        System.setProperty("darklaf.decorations", "false");
        System.setProperty("darklaf.allowNativeCode", "true");

        LafManager.installTheme(ThemeFactory.byName(preferences.get("theme.name", "IntelliJ")));
        LafManager.install();

        JFrame frame = new JFrame("Chat");
        MainWindow mainWindow = new MainWindow();
        mainWindow.setFrame(frame);
        frame.setContentPane(mainWindow.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(912, 640));
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JSplitPane splitPane1 = new JSplitPane();
        panel1.add(splitPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JSplitPane splitPane2 = new JSplitPane();
        splitPane2.setContinuousLayout(true);
        splitPane2.setResizeWeight(1.0);
        splitPane1.setRightComponent(splitPane2);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane2.setLeftComponent(panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel2.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        messageField = new JTextField();
        messageField.setText("");
        CellConstraints cc = new CellConstraints();
        panel3.add(messageField, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        sendButton = new JButton();
        sendButton.setText("Send");
        panel3.add(sendButton, cc.xy(3, 1));
        messagesScrollPane = new JScrollPane();
        messagesScrollPane.setHorizontalScrollBarPolicy(31);
        panel2.add(messagesScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        messages = new JList();
        messagesScrollPane.setViewportView(messages);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane2.setRightComponent(panel4);
        users = new JList();
        panel4.add(users, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(leftPanel);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        leftPanel.add(panel5, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        settingsButton = new JButton();
        settingsButton.setText("Settings");
        panel5.add(settingsButton, cc.xy(5, 1));
        panel5.add(userPanel, cc.xy(1, 1));
        final Spacer spacer1 = new Spacer();
        panel5.add(spacer1, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JScrollPane scrollPane1 = new JScrollPane();
        leftPanel.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        channels = new JList();
        scrollPane1.setViewportView(channels);
        final JScrollPane scrollPane2 = new JScrollPane();
        leftPanel.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        groups = new JList();
        scrollPane2.setViewportView(groups);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
