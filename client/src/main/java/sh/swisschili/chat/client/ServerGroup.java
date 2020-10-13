package sh.swisschili.chat.client;

import io.grpc.stub.StreamObserver;
import sh.swisschili.chat.util.ChatProtos.*;

import java.util.List;
import java.util.stream.Collectors;

public class ServerGroup {
    private final String server;
    private final String groupName;
    private Group group = null;
    private final ServerPool pool;
    private final ErrorListener error;
    private final ChannelsReceivedListener listener;

    private List<ServerChannel> channels = null;

    public interface ChannelsReceivedListener {
        void channelsReceived(List<ServerChannel> channels);
    }

    public interface ErrorListener {
        void onError(Throwable error);
    }

    public ServerGroup(ServerPool pool, String server, String groupName, ErrorListener error,
                       ChannelsReceivedListener listener) {
        this.server = server;
        this.pool = pool;
        this.error = error;
        this.listener = listener;
        this.groupName = groupName;

        StreamObserver<Group> observer = new StreamObserver<Group>() {
            @Override
            public void onNext(Group value) {
                group = value;
            }

            @Override
            public void onError(Throwable t) {
                error.onError(t);
            }

            @Override
            public void onCompleted() {
                pool.stubFor(server)
                        .getGroupChannels(group, new StreamObserver<>() {
                            @Override
                            public void onNext(GroupChannelsResponse value) {
                                channels = value.getChannelsList().stream()
                                        .map(channel -> new ServerChannel(pool, server, channel))
                                        .collect(Collectors.toList());
                            }

                            @Override
                            public void onError(Throwable t) {
                                error.onError(t);
                            }

                            @Override
                            public void onCompleted() {
                                listener.channelsReceived(channels);
                            }
                        });
            }
        };

        pool.stubFor(server)
                .getGroupByName(GroupByNameRequest.newBuilder().setName(groupName).build(), observer);
    }

    @Override
    public String toString() {
        return groupName + "#" + server;
    }
}
