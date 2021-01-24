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

import com.github.weisj.darklaf.LafManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.ChatProtos.*;
import sh.swisschili.chat.util.Constants;
import sh.swisschili.chat.util.ServerPool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class MainWindow {
    private JPanel panel1;
    private JButton sendButton;
    private JTextField messageField;
    private JList<Message> messages;
    private JList<ServerGroup> groups;
    private JList<ServerChannel> channels;
    private JList<UserStatus> users;
    private JScrollPane messagesScrollPane;
    private JPanel leftPanel;
    private JButton settingsButton;
    private JPanel userPanel;
    private JComboBox<String> statusBox;
    private JButton joinButton;
    private JButton newChannelButton;

    private JFrame frame;

    private final UserComponent userComponent = new UserComponent();

    private final DefaultListModel<ServerGroup> groupModel = new DefaultListModel<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class.getName());

    private ServerChannel currentChannel = null;
    private static final ServerPool pool = new ServerPool();
    private final User currentUser;
    private final Presence userPresence = Presence.ONLINE;

    private static final Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);

    private void createUIComponents() {
        userPanel = userComponent.getRootPanel();
        messages = new JList<Message>() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        messages.addComponentListener(new MessageListAdapter(messages));
        messagesScrollPane = new InfiniteScrollPane<>(messages);
    }

    private static class GroupsPopUp extends JPopupMenu {
        JMenuItem addGroup;

        public GroupsPopUp(ActionListener addGroupListener) {
            addGroup = new JMenuItem("Add group", ColoredIcon.buildIcon(FontAwesome.PLUS, 14));
            addGroup.addActionListener(addGroupListener);
            add(addGroup);
        }
    }

    public MainWindow() throws UserCredentials.CredentialsNotFound {
        $$$setupUI$$$();

        currentUser = UserCredentials.getUser();
        userComponent.setUser(currentUser);

        users.setCellRenderer(new UserStatusComponent.Renderer());

        DefaultComboBoxModel<String> statusModel = new DefaultComboBoxModel<>();
        Arrays.asList("Online", "Away", "Do not disturb").forEach(statusModel::addElement);
        statusBox.setModel(statusModel);

        statusBox.addActionListener(e -> {
            Presence presence = null;
            String selected = (String) statusBox.getSelectedItem();
            if (selected == null)
                return;

            switch (selected) {
                case "Online":
                    presence = Presence.ONLINE;
                    break;
                case "Away":
                    presence = Presence.AWAY;
                    break;
                case "Do not disturb":
                    presence = Presence.DND;
            }

            UserStatus.Builder builder = UserStatus.newBuilder();
            if (presence == null) {
                builder.setCustom(CustomPresence.newBuilder()
                        .setName(selected).build());
            } else {
                builder.setPresence(presence);
            }

            updateStatus(builder.build());
        });

        groups.setComponentPopupMenu(new GroupsPopUp(e -> new AddGroupDialog(this::groupAdded)
                .setVisible(true)));

        final ActionListener sendActionListener = e -> sendMessage();

        messages.setSelectionModel(new NoSelectionModel());
        messagesScrollPane.setHorizontalScrollBar(null);
        messagesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        settingsButton.setText("");
        settingsButton.setIcon(ColoredIcon.buildIcon(FontAwesome.COG, 14));

        sendButton.setText("");
        sendButton.setIcon(ColoredIcon.buildIcon(FontAwesome.PAPER_PLANE, 14));

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

            InfiniteScrollPane<Message> infiniteScrollPane = (InfiniteScrollPane<Message>) messagesScrollPane;
            infiniteScrollPane.setBufferedLoader(channel.getBufferedLoader());
            currentChannel = channel;
        });

        settingsButton.addActionListener(e -> {
            new SettingsDialog(frame).setVisible(true);
        });

        joinButton.addActionListener(e ->
                new AddGroupDialog(this::groupAdded)
                        .setVisible(true));


        newChannelButton.addActionListener(e -> {
            ServerGroup group = groups.getSelectedValue();
            if (group == null)
                return;
            new AddChannelDialog(group::createChannel).setVisible(true);
        });
    }

    private void updateStatus(UserStatus status) {
        for (int i = 0; i < groupModel.size(); i++) {
            groupModel.get(i).setStatus(status);
        }
    }

    private void groupAdded(String groupName, String server) {
        LOGGER.info(String.format("Group added: %s#%s", groupName, server));

        ServerGroup serverGroup = new ServerGroup(pool, server, groupName, currentUser, this::onError,
                (sg, groupChannels) -> {
                    sg.setStatus(UserStatus.newBuilder()
                            .setUser(currentUser)
                            .setPresence(userPresence)
                            .build());
                    SwingUtilities.invokeLater(this::groupSelected);
                });

        groupModel.addElement(serverGroup);
    }

    private void groupSelected() {
        ServerGroup selected = groups.getSelectedValue();
        if (selected == null)
            return;

        LOGGER.info("Group selected " + selected);

        channels.setModel(selected.getModel());
        channels.setSelectedIndex(0);
        users.setModel(selected.getUserModel());
    }

    private void onError(Throwable t) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(panel1, t.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    protected void sendMessage() {
        if (messageField.getText().trim().isEmpty() || currentChannel == null)
            return;

        LOGGER.info("Message sent " + messageField.getText());

        Message message = Message.newBuilder()
                .setBody(messageField.getText())
                .setSender(currentChannel.getUser())
                .setUnixTime(System.currentTimeMillis())
                .build();

        messageField.setText("");

        LOGGER.info("Sending message to server");

        try {
            currentChannel.sendMessage(message, UserCredentials.getUserKeys());
        } catch (UserCredentials.CredentialsNotFound e) {
            onError(e);
        }
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    private static void showMainWindow() throws UserCredentials.CredentialsNotFound {
        JFrame frame = new JFrame("Chat");
        MainWindow mainWindow = new MainWindow();
        mainWindow.setFrame(frame);
        frame.setContentPane(mainWindow.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(912, 640));
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        System.out.println(Constants.LICENSE_HEADER);

        IconFontSwing.register(FontAwesome.getIconFont());

        System.setProperty("darklaf.decorations", "false");
        System.setProperty("darklaf.allowNativeCode", "true");

        LafManager.installTheme(ThemeFactory.byName(preferences.get("theme.name", "IntelliJ")));
        LafManager.install();

        if (UserCredentials.notLoggedIn()) {
            AtomicBoolean loggedIn = new AtomicBoolean(false);

            JFrame loginFrame = new JFrame();
            loginFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    LOGGER.info("Login window closed");

                    if (loggedIn.get())
                        super.windowClosed(e);
                    else {
                        LOGGER.info("Login window closed, exiting");
                        System.exit(0);
                    }
                }
            });

            KeyPair keys = UserCredentials.createUserKeys();

            LoginDialog loginDialog = new LoginDialog(pool, "Welcome", loginFrame, keys, (dialog, user, password) -> {
                SwingUtilities.invokeLater(() -> {
                    LOGGER.info(String.format("Logged in %s@%s (%s)", user.getName(), user.getHost(), String.valueOf(password)));
                    loggedIn.set(true);

                    dialog.setVisible(false);

                    UserCredentials.setUser(user);
                    UserCredentials.setPassword(password);

                    try {
                        showMainWindow();
                    } catch (UserCredentials.CredentialsNotFound e) {
                        LOGGER.error("Credentials not found after login");
                    }
                });
            });
            loginDialog.setVisible(true);
        } else {
            try {
                showMainWindow();
            } catch (UserCredentials.CredentialsNotFound e) {
                LOGGER.error("Credentials not found (this should never happen)");
            }
        }
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
        messagesScrollPane.setHorizontalScrollBarPolicy(31);
        panel2.add(messagesScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(200, -1), null, null, 0, false));
        messagesScrollPane.setViewportView(messages);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane2.setRightComponent(panel4);
        users = new JList();
        panel4.add(users, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(leftPanel);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        leftPanel.add(panel5, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        settingsButton = new JButton();
        settingsButton.setText("Settings");
        panel5.add(settingsButton, cc.xy(5, 1));
        statusBox = new JComboBox();
        statusBox.setEditable(true);
        panel5.add(statusBox, cc.xyw(1, 3, 5));
        panel5.add(userPanel, cc.xy(1, 1));
        final Spacer spacer1 = new Spacer();
        panel5.add(spacer1, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JScrollPane scrollPane1 = new JScrollPane();
        leftPanel.add(scrollPane1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        channels = new JList();
        scrollPane1.setViewportView(channels);
        final JScrollPane scrollPane2 = new JScrollPane();
        leftPanel.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        groups = new JList();
        scrollPane2.setViewportView(groups);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        leftPanel.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        joinButton = new JButton();
        joinButton.setText("Join");
        panel6.add(joinButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        leftPanel.add(panel7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        newChannelButton = new JButton();
        newChannelButton.setText("New");
        panel7.add(newChannelButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
