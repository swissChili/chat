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

import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos.*;
import sh.swisschili.chat.util.ServerPool;

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

        StreamObserver<AddUserResponse> addUserObserver = new StreamObserver<AddUserResponse>() {
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
                                .getGroupChannels(group, new StreamObserver<GroupChannelsResponse>() {
                                    @Override
                                    public void onNext(GroupChannelsResponse value) {
                                        channels = value.getChannelsList().stream()
                                                .map(channel -> new ServerChannel(pool, server, channel, authorizedUser))
                                                .collect(Collectors.toList());

                                        model.clear();
                                        channels.forEach(model::addElement);
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
                                        new StreamObserver<UserStatus>() {
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
                        new StreamObserver<SetUserStatusResponse>() {
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

    public void createChannel(String name) {
        ChatGrpc.ChatStub stub = pool.chatStubFor(server);

        ServerGroup g = this;

        stub.createChannel(CreateChannelRequest.newBuilder().setGroup(group)
                        .setChannelName(name).build(),
                new StreamObserver<CreateChannelResponse>() {
                    @Override
                    public void onNext(CreateChannelResponse value) {
                        ServerChannel channel = new ServerChannel(pool, server, value.getChannel(), authorizedUser);
                        g.channels.add(channel);
                        g.model.addElement(channel);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        // nothing
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
        return groupName; // + "#" + server;
    }
}
