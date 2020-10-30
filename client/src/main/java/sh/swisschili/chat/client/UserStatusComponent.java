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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import jiconfont.icons.font_awesome.FontAwesome;
import sh.swisschili.chat.util.ChatProtos;

import javax.swing.*;
import java.awt.*;

public class UserStatusComponent {
    private JLabel status;
    private JPanel user;
    private JPanel rootPanel;

    private final ChatProtos.UserStatus userStatus;

    public UserStatusComponent(ChatProtos.UserStatus userStatus) {
        this.userStatus = userStatus;
        $$$setupUI$$$();

        status.setText("");

        Color color = Color.GREEN;
        String presenceText = null;
        ChatProtos.Presence presence = userStatus.getPresence();

        if (presence.equals(ChatProtos.Presence.AWAY)) {
            color = Color.YELLOW;
            presenceText = "Away";
        } else if (presence.equals(ChatProtos.Presence.DND)) {
            color = Color.RED;
            presenceText = "Do not disturb";
        } else if (presence.equals(ChatProtos.Presence.ONLINE)) {
            presenceText = "Online";
        }

        if (userStatus.hasCustom()) {
            presenceText = userStatus.getCustom().getName();
        }

        status.setIcon(ColoredIcon.buildIcon(FontAwesome.USER, 14, color));

        if (presenceText != null)
            rootPanel.setToolTipText(presenceText);
    }

    private void createUIComponents() {
        user = new UserComponent(userStatus.getUser()).getRootPanel();
    }

    public JPanel getRootPanel() {
        return rootPanel;
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
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        status = new JLabel();
        status.setText("Status");
        rootPanel.add(status, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rootPanel.add(user, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    public static class Renderer implements ListCellRenderer<ChatProtos.UserStatus> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ChatProtos.UserStatus> list, ChatProtos.UserStatus value, int index, boolean isSelected, boolean cellHasFocus) {
            return new UserStatusComponent(value).rootPanel;
        }
    }
}
