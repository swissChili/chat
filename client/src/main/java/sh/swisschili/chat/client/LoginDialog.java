package sh.swisschili.chat.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginDialog extends JDialog {
    private JPanel rootPanel;
    private JTextField username;
    private JTextField host;
    private JPasswordField password;
    private JButton registerButton;
    private JButton logInButton;
    private JButton cancelButton;

    public enum Method {
        LOG_IN,
        REGISTER,
    }

    public interface LoginListener {
        void loginRequested(String name, String host, char[] password, Method method);
    }

    public LoginDialog(JFrame owner, LoginListener listener) {
        super(owner);

        add(rootPanel);

        logInButton.addActionListener(e ->
            listener.loginRequested(username.getText(), host.getText(), password.getPassword(), Method.LOG_IN));

        registerButton.addActionListener(e ->
            listener.loginRequested(username.getText(), host.getText(), password.getPassword(), Method.REGISTER));


        cancelButton.addActionListener(e ->
                setVisible(false));
    }
}
