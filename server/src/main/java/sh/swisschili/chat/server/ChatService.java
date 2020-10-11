package sh.swisschili.chat.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class ChatService extends ChatGrpc.ChatImplBase {
    private final Logger LOGGER = Logger.getLogger(ChatService.class.getName());
    private final Connection conn;

    public ChatService(@NotNull String mqHost, int port) throws IOException, TimeoutException {
        super();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(mqHost);
        connectionFactory.setPort(port);
        conn = connectionFactory.newConnection();
    }

    private Channel getChannel(String name) throws IOException {
        Channel channel = conn.createChannel();
        channel.exchangeDeclare(name, "fanout");
        return channel;
    }

    @Override
    public void getMessages(ChatProtos.Channel request, StreamObserver<ChatProtos.Message> responseObserver) {
        try {
            String exchangeName = "channel:" + request.getName();
            Channel channel = getChannel(exchangeName);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, exchangeName, "");

            LOGGER.info("Connected to RabbitMQ channel");

            DeliverCallback deliverCallback = (consumerTag, message) -> {
                ChatProtos.Message m = ChatProtos.Message.parseFrom(message.getBody());
                LOGGER.info("Message received from " + m.getSender().getName());

                // responseObserver never finishes
                try {
                    responseObserver.onNext(m);
                } catch (Exception e) {
                    // If the stream fails, unsubscribe from the message queue
                    channel.basicCancel(consumerTag);
                }
            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            LOGGER.severe("Could not create RabbitMQ channel in getMessages");
            responseObserver.onError(e);
        }
    }

    @Override
    public void sendMessage(ChatProtos.OutgoingMessage request, StreamObserver<ChatProtos.MessageResponse> responseObserver) {
        try {
            String exchangeName = "channel:" + request.getChannel().getName();
            Channel channel = getChannel(exchangeName);

            LOGGER.info("Sending message: " + request.getMessage().getBody());
            channel.basicPublish(exchangeName, "", null, request.getMessage().toByteArray());
        } catch (IOException e) {
            LOGGER.severe("Could not create RabbitMQ channel in sendMessage");
        }
        responseObserver.onNext(ChatProtos.MessageResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
