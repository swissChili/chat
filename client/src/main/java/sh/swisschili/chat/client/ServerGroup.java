package sh.swisschili.chat.client;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos.*;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public class ServerGroup {
    private final String server;
    private final String groupName;
    private Group group;
    private final ServerPool pool;
    private final ErrorListener error;
    private final ChannelsReceivedListener listener;

    private final DefaultListModel<ServerChannel> model = new DefaultListModel<>();
    private final StreamingListModel<String, UserStatus> userModel = new StreamingListModel<>(
            status -> status.getUser().getName(), new UserStatusComparator(), StreamingListModel.SortDirection.DESCENDING);

    private List<ServerChannel> channels = null;
    private User authorizedUser;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerGroup.class);

    public interface ChannelsReceivedListener {
        void channelsReceived(ServerGroup serverGroup, List<ServerChannel> channels);
    }

    public interface ErrorListener {
        void onError(Throwable error);
    }

    public ServerGroup(ServerPool pool, String server, String groupName, User user, ErrorListener error,
                       ChannelsReceivedListener listener) {
        this.server = server;
        this.pool = pool;
        this.error = error;
        this.listener = listener;
        this.groupName = groupName;

        ServerGroup serverGroup = this;

        StreamObserver<AddUserResponse> addUserObserver = new StreamObserver<>() {
            @Override
            public void onNext(AddUserResponse value) {
                authorizedUser = value.getUser();
            }

            @Override
            public void onError(Throwable t) {
                error.onError(t);
            }

            @Override
            public void onCompleted() {
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
                        pool.chatStubFor(server)
                                .getGroupChannels(group, new StreamObserver<>() {
                                    @Override
                                    public void onNext(GroupChannelsResponse value) {
                                        channels = value.getChannelsList().stream()
                                                .map(channel -> new ServerChannel(pool, server, channel, authorizedUser))
                                                .collect(Collectors.toList());

                                        model.clear();
                                        model.addAll(channels);
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        error.onError(t);
                                    }

                                    @Override
                                    public void onCompleted() {
                                        listener.channelsReceived(serverGroup, channels);
                                    }
                                });

                        pool.chatStubFor(server)
                                .getGroupUserStatuses(GroupUserStatusRequest.newBuilder().setGroup(group).build(),
                                        new StreamObserver<>() {
                                            @Override
                                            public void onNext(UserStatus value) {
                                                LOGGER.info("Got user status " + value.toString());
                                                userModel.add(value);
                                            }

                                            @Override
                                            public void onError(Throwable t) {
                                                error.onError(t);
                                            }

                                            @Override
                                            public void onCompleted() {
                                            }
                                        });
                    }
                };

                pool.chatStubFor(server)
                        .getGroupByName(GroupByNameRequest.newBuilder().setName(groupName).build(), observer);
            }
        };

        ChatGrpc.ChatStub stub = pool.chatStubFor(server);
        stub.addUser(AddUserRequest.newBuilder()
                        .setHost(user.getHost())
                        .setName(user.getName())
                        .build(),
                addUserObserver);
    }

    public void setStatus(UserStatus status) {
        status = UserStatus.newBuilder(status)
                .setUser(authorizedUser)
                .build();

        LOGGER.info(String.format("Setting status: %s\n", status.toString()));

        pool.chatStubFor(server)
                .setUserStatus(SetUserStatusRequest.newBuilder()
                                .setGroup(group)
                                .setStatus(status)
                                .build(),
                        new StreamObserver<>() {
                            @Override
                            public void onNext(SetUserStatusResponse value) {
                            }

                            @Override
                            public void onError(Throwable t) {
                                error.onError(t);
                            }

                            @Override
                            public void onCompleted() {
                            }
                        });
    }

    public DefaultListModel<ServerChannel> getModel() {
        return model;
    }

    public ListModel<UserStatus> getUserModel() {
        return userModel;
    }

    @Override
    public String toString() {
        return groupName + "#" + server;
    }
}
