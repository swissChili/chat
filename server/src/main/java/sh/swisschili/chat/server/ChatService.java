package sh.swisschili.chat.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ConnectionFactory;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;

import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.*;

public class ChatService extends ChatGrpc.ChatImplBase {
    private final Logger LOGGER = LoggerFactory.getLogger(ChatService.class.getName());
    private final Connection conn;
    private final ServerDatabase db;

    public ChatService(@NotNull String mqHost, int port, ServerDatabase db) throws IOException, TimeoutException {
        super();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(mqHost);
        connectionFactory.setPort(port);
        conn = connectionFactory.newConnection();
        this.db = db;
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

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            LOGGER.error("Could not create RabbitMQ channel in getMessages");
            responseObserver.onError(e);
        }
    }

    @Override
    public void sendMessage(ChatProtos.OutgoingMessage request, StreamObserver<ChatProtos.MessageResponse> responseObserver) {
        try {
            String exchangeName = ServerConstants.getChannelExchange(request.getChannel().getId());
            Channel channel = getChannel(exchangeName);

            LOGGER.info("Sending message: " + request.getMessage().getBody());
            channel.basicPublish(exchangeName, "", null, request.getMessage().toByteArray());
        } catch (IOException e) {
            LOGGER.error("Could not create RabbitMQ channel in sendMessage");
        }
        responseObserver.onNext(ChatProtos.MessageResponse.newBuilder().build());
        responseObserver.onCompleted();
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
                }
            };
            channel.basicConsume(queueName, true, callback, consumerTag -> {});
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }
}
