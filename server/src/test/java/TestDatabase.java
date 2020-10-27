import org.junit.Test;
import sh.swisschili.chat.server.ServerDatabase;
import sh.swisschili.chat.util.ChatProtos.*;

import java.util.List;
import java.util.logging.Logger;

public class TestDatabase {
    private final ServerDatabase db;
    private final Logger LOGGER = Logger.getLogger(TestDatabase.class.getName());

    {
        String dbUrl = System.getProperty("db-url");
        LOGGER.info(dbUrl);
        db = new ServerDatabase(dbUrl);
    }

    @Test
    public void addUser() {
        User user = db.getOrAddUser("joe", "localhost");
        LOGGER.info("User id is " + user.getId());

        assert user.getId().length() > 4;
        assert user.getName().equals("joe");
        assert user.getHost().equals("localhost");
    }

    @Test
    public void groupTest() {
        Group group = db.createGroup("test-group");
        LOGGER.info(String.format("Created group %s", group.getId()));

        List<Channel> channels = db.getGroupChannels(group);

        assert channels.size() == 1;
        assert channels.get(0).getName().equals("general");
    }

    @Test
    public void messageRangeTest() throws ClassNotFoundException {
        LOGGER.info("Getting last 10 messages");
        Channel channel = db.getGroupChannels(db.getGroupByName("test-group")).get(0);
        LOGGER.info("Channel is " + channel);
        db.getMessageRange(channel, 0, 10)
                .forEach(System.out::println);
    }
}
