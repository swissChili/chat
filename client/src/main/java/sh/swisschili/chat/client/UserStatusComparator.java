package sh.swisschili.chat.client;

import sh.swisschili.chat.util.ChatProtos.*;

import java.util.Comparator;

public class UserStatusComparator implements Comparator<UserStatus> {
    @Override
    public int compare(UserStatus o1, UserStatus o2) {
        return o1.getUser().getName().compareTo(o2.getUser().getName());
    }
}
