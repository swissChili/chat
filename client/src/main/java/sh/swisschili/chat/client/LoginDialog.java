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

import com.google.protobuf.ByteString;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import io.grpc.stub.StreamObserver;
import sh.swisschili.chat.util.AuthGrpc;
import sh.swisschili.chat.util.ChatProtos;
import sh.swisschili.chat.util.ServerPool;
import sh.swisschili.chat.util.SignedAuth;

import javax.swing.*;
import java.awt.*;
import java.security.KeyPair;

public class LoginDialog extends JDialog {
    private JPanel rootPanel;
    private JTextField username;
    private JTextField host;
    private JPasswordField password;
    private JButton registerButton;
    private JButton logInButton;
    private JButton cancelButton;
    private JLabel welcome;

    private final LoginListener listener;
    private final ServerPool pool;
    private final KeyPair keys;

    public enum Method {
        LOG_IN,
        REGISTER,
    }

    public interface LoginListener {
        void loggedIn(LoginDialog dialog, ChatProtos.User user, char[] password);
    }

    public LoginDialog(ServerPool pool, String message, JFrame owner, KeyPair keys, LoginListener listener) {
        super(owner);

        this.listener = listener;
        this.pool = pool;
        this.keys = keys;
        welcome.setText(message);

        add(rootPanel);
        setMinimumSize(new Dimension(400, 240));
        getRootPane().setDefaultButton(logInButton);

        logInButton.addActionListener(e -> doLogin(Method.LOG_IN));
        registerButton.addActionListener(e -> doLogin(Method.REGISTER));
        cancelButton.addActionListener(e -> setVisible(false));
    }

    private void doLogin(Method method) {
        LoginDialog dialog = this;

        AuthGrpc.AuthStub stub = pool.authStubFor(host.getText());
        if (method.equals(Method.LOG_IN)) {
            ChatProtos.SignInRequest request = ChatProtos.SignInRequest.newBuilder()
                    .setName(username.getText())
                    .setPassword(String.valueOf(password.getPassword()))
                    .build();

            stub.signIn(request, new StreamObserver<ChatProtos.SignInResponse>() {
                private ChatProtos.User user;
                @Override
                public void onNext(ChatProtos.SignInResponse value) {
                    user = value.getUser();
                }

                @Override
                public void onError(Throwable t) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, t, "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }

                @Override
                public void onCompleted() {
                    listener.loggedIn(dialog, user, password.getPassword());
                }
            });
        } else {
            ChatProtos.RegisterRequest request = ChatProtos.RegisterRequest.newBuilder()
                    .setName(username.getText())
                    .setPassword(String.valueOf(password.getPassword()))
                    .setPublicKey(ByteString.copyFrom(SignedAuth.pubKeyToBytes(keys.getPublic())))
                    .build();

            stub.register(request, new StreamObserver<ChatProtos.RegisterResponse>() {
                private boolean completed = false;

                @Override
                public void onNext(ChatProtos.RegisterResponse value) {
                    if (completed)
                        return;

                    if (value.hasRedirect()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(dialog, value.getRedirect().getTo(), value.getRedirect().getMessage(),
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    } else {
                        completed = true;
                        listener.loggedIn(dialog, value.getUser(), password.getPassword());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, t, "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }

                @Override
                public void onCompleted() {
                }
            });
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        rootPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        rootPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Name");
        CellConstraints cc = new CellConstraints();
        panel1.add(label1, cc.xy(1, 1));
        username = new JTextField();
        panel1.add(username, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label2 = new JLabel();
        label2.setText("Host");
        panel1.add(label2, cc.xy(1, 3));
        host = new JTextField();
        panel1.add(host, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label3 = new JLabel();
        label3.setText("Password");
        panel1.add(label3, cc.xy(1, 5));
        password = new JPasswordField();
        panel1.add(password, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        registerButton = new JButton();
        registerButton.setText("Register");
        panel2.add(registerButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        logInButton = new JButton();
        logInButton.setText("Log in");
        panel2.add(logInButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel2.add(cancelButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        welcome = new JLabel();
        welcome.setText("Label");
        rootPanel.add(welcome, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
