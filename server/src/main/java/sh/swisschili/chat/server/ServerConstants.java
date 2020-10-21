package sh.swisschili.chat.server;

import sh.swisschili.chat.util.ChatProtos.*;

public class ServerConstants {
    public static String getChannelExchange(String id) {
        return "sh.swisschili.chat.channel.messages:" + id;
    }

    public static String getGroupUserStatusExchange(String groupId) {
        return "sh.swisschili.chat.group.statuses:" + groupId;
    }
}
