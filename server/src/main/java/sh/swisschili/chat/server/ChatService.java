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

package sh.swisschili.chat.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.*;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos;

import java.io.IOException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChatService extends ChatGrpc.ChatImplBase {
    private final Logger LOGGER = LoggerFactory.getLogger(ChatService.class.getName());
    private final Connection conn;
    private final ServerDatabase db;
    private final ServerPool pool = new ServerPool();
    private final Map<ChatProtos.User, PublicKey> keys = new HashMap<>();
    private final ReadWriteLock keysLock = new ReentrantReadWriteLock();

    private boolean allowUnsignedMessages = false;

    public ChatService(@NotNull String mqHost, int port, ServerDatabase db) throws IOException, TimeoutException {
        super();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(mqHost);
        connectionFactory.setPort(port);
        conn = connectionFactory.newConnection();
        this.db = db;
    }

    private CompletableFuture<PublicKey> getUserPublicKey(ChatProtos.User user) {
        CompletableFuture<PublicKey> future = new CompletableFuture<>();

        LOGGER.info("Getting public key for " + user);

        Lock lock = keysLock.readLock();
        lock.lock();
        boolean containsKey = keys.containsKey(user);

        if (containsKey) {
            LOGGER.info("Public key cached");
            future.complete(keys.get(user));
            lock.unlock();
        } else {
            lock.unlock();
            LOGGER.info("Public key not cached, querying server for details");

            StreamObserver<ChatProtos.UserPublicKey> listener = new StreamObserver<ChatProtos.UserPublicKey>() {
                @Override
                public void onNext(ChatProtos.UserPublicKey value) {
                    try {
                        LOGGER.info("Got public key from server");
                        PublicKey publicKey = SignedAuth.pubKeyFromBytes(value.getPublicKey().toByteArray());

                        Lock writeLock = keysLock.writeLock();
                        writeLock.lock();
                        keys.put(user, publicKey);
                        writeLock.unlock();

                        future.complete(publicKey);
                    } catch (InvalidKeySpecException e) {
                        LOGGER.warn(String.format("Could not decode public key for %s", user.getName()));
                        onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("Completed public key request");
                }
            };

            pool.authStubFor(user.getHost())
                    .getUserPublicKey(ChatProtos.PublicKeyRequest.newBuilder()
                                    .setUser(user).build(),
                            listener);
        }

        return future;
    }

    private Channel getChannel(String name) throws IOException {
        Channel channel = conn.createChannel();
        channel.exchangeDeclare(name, "fanout");
        return channel;
    }

    private String getQueue(Channel channel, String exchangeName) throws IOException {
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");
        return queueName;
    }

    @Override
    public void getMessages(ChatProtos.Channel request, StreamObserver<ChatProtos.Message> responseObserver) {
        try {
            String exchangeName = ServerConstants.getChannelExchange(request.getId());
            Channel channel = getChannel(exchangeName);
            String queueName = getQueue(channel, exchangeName);

            LOGGER.info("Connected to RabbitMQ channel");

            DeliverCallback deliverCallback = (consumerTag, message) -> {
                ChatProtos.Message m = ChatProtos.Message.parseFrom(message.getBody());
                LOGGER.info("Message received from " + m.getSender().getName());

                // responseObserver never finishes
                try {
                    responseObserver.onNext(m);
                } catch (Exception e) {
                    LOGGER.info("Client disconnected");
                    // If the stream fails, unsubscribe from the message queue
                    channel.basicCancel(consumerTag);
                }
            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            LOGGER.error("Could not create RabbitMQ channel in getMessages");
            responseObserver.onError(e);
        }
    }

    @Override
    public void sendMessage(ChatProtos.OutgoingMessage request, StreamObserver<ChatProtos.MessageResponse> responseObserver) {
        CompletableFuture<PublicKey> publicKeyFuture = getUserPublicKey(request.getMessage().getSender());

        publicKeyFuture.thenAccept(publicKey -> {
            if (!allowUnsignedMessages) {
                if (!SignedAuth.verify(publicKey, request.getSignature().toByteArray(), request.getMessage().toByteArray(),
                        request.getChannel().toByteArray())
                ) {
                    LOGGER.info("Signature invalid, throwing SignedAuthenticationError");
                    responseObserver.onError(new SignedAuthenticationError());
                    return;
                }
            }

            try {
                String exchangeName = ServerConstants.getChannelExchange(request.getChannel().getId());
                Channel channel = getChannel(exchangeName);

                ChatProtos.Message message = ChatProtos.Message.newBuilder(request.getMessage())
                        .setId(db.saveMessage(request.getChannel(), request.getMessage()).toString())
                        .build();

                LOGGER.info("Sending message: " + message.getBody());
                channel.basicPublish(exchangeName, "", null, message.toByteArray());
            } catch (IOException e) {
                LOGGER.error("Could not create RabbitMQ channel in sendMessage");
                responseObserver.onError(e);
            }

            responseObserver.onNext(ChatProtos.MessageResponse.newBuilder().build());
            responseObserver.onCompleted();
        }).exceptionally(t -> {
            LOGGER.warn("getUserPubicKey returned a failure");
            responseObserver.onError(t);

            return null;
        });
    }

    @Override
    public void createGroup(ChatProtos.CreateGroupRequest request, StreamObserver<ChatProtos.CreateGroupResponse> responseObserver) {
        ChatProtos.Group group = db.createGroup(request.getGroupName());

        LOGGER.info("Added group " + group);

        ChatProtos.CreateGroupResponse response = ChatProtos.CreateGroupResponse.newBuilder()
                .setStatus(ChatProtos.Status.OK)
                .setGroup(group)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createChannel(ChatProtos.CreateChannelRequest request, StreamObserver<ChatProtos.CreateChannelResponse> responseObserver) {
        ChatProtos.Channel channel = db.createChannel(request.getGroup(), request.getChannelName());

        LOGGER.info("Added channel " + channel);

        ChatProtos.CreateChannelResponse response = ChatProtos.CreateChannelResponse.newBuilder()
                .setStatus(ChatProtos.Status.OK)
                .setChannel(channel)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void addUser(ChatProtos.AddUserRequest request, StreamObserver<ChatProtos.AddUserResponse> responseObserver) {
        ChatProtos.User user = db.getOrAddUser(request.getName(), request.getHost());

        LOGGER.info("Added user " + user.toString());

        ChatProtos.AddUserResponse response = ChatProtos.AddUserResponse.newBuilder()
                .setStatus(ChatProtos.Status.OK)
                .setUser(user)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getGroupChannels(ChatProtos.Group request, StreamObserver<ChatProtos.GroupChannelsResponse> responseObserver) {
        List<ChatProtos.Channel> channels = db.getGroupChannels(request);
        LOGGER.info(String.format("Got %d channels in group %s", channels.size(), request.getName()));

        ChatProtos.GroupChannelsResponse response = ChatProtos.GroupChannelsResponse.newBuilder()
                .addAllChannels(channels)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getGroupByName(ChatProtos.GroupByNameRequest request, StreamObserver<ChatProtos.Group> responseObserver) {
        try {
            ChatProtos.Group g = db.getGroupByName(request.getName());
            responseObserver.onNext(g);
            responseObserver.onCompleted();
        } catch (ClassNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void setUserStatus(ChatProtos.SetUserStatusRequest request, StreamObserver<ChatProtos.SetUserStatusResponse> responseObserver) {
        try {
            String exchangeName = ServerConstants.getGroupUserStatusExchange(request.getGroup().getId());
            Channel channel = getChannel(exchangeName);
            channel.basicPublish(exchangeName, "", null, request.getStatus().toByteArray());

            db.setUserStatus(request.getStatus(), request.getGroup());

            LOGGER.info(String.format("Set user status %s", request.toString()));

            responseObserver.onNext(ChatProtos.SetUserStatusResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getGroupUserStatuses(ChatProtos.GroupUserStatusRequest request, StreamObserver<ChatProtos.UserStatus> responseObserver) {
        try {
            String exchangeName = ServerConstants.getGroupUserStatusExchange(request.getGroup().getId());
            Channel channel = getChannel(exchangeName);
            String queueName = getQueue(channel, exchangeName);

            for (ChatProtos.UserStatus status : db.getUserStatuses(request.getGroup())) {
                responseObserver.onNext(status);
            }

            DeliverCallback callback = (consumerTag, message) -> {
                ChatProtos.UserStatus status = ChatProtos.UserStatus.parseFrom(message.getBody());

                try {
                    responseObserver.onNext(status);
                } catch (Exception e) {
                    LOGGER.info("Client disconnected");
                    channel.basicCancel(consumerTag);

                    setUserOffline(request.getFor(), request.getGroup());
                }
            };
            channel.basicConsume(queueName, true, callback, consumerTag -> {
            });
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getMessageRange(ChatProtos.MessageRangeRequest request, StreamObserver<ChatProtos.MessageRangeResponse> responseObserver) {
        List<ChatProtos.Message> messages = db.getMessageRange(request.getChannel(), request.getFrom(), request.getCount());
        responseObserver.onNext(ChatProtos.MessageRangeResponse.newBuilder()
                .addAllMessages(messages).build());
        responseObserver.onCompleted();
    }

    public void setAllowUnsignedMessages(boolean allowUnsignedMessages) {
        this.allowUnsignedMessages = allowUnsignedMessages;
    }

    private void setUserOffline(ChatProtos.User user, ChatProtos.Group group) {
        db.setUserStatus(ChatProtos.UserStatus.newBuilder().setUser(user)
                        .setPresence(ChatProtos.Presence.OFFLINE)
                        .build(),
                group);
    }
}
