package sh.swisschili.chat.client;

import io.grpc.stub.StreamObserver;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos.*;

import javax.swing.*;
import java.util.logging.Logger;

public class ServerChannel {
    private final ServerPool pool;
    private final Channel channel;
    private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
    private final ChatGrpc.ChatStub stub;

    private static final Logger LOGGER = Logger.getLogger(ServerPool.class.getName());

    public interface MessageListener {
        void onMessage(Message message);
    }

    public ServerChannel(ServerPool pool, String server, Channel channel, MessageListener listener) {
        this.pool = pool;
        this.channel = channel;
        stub = pool.stubFor(server);

        stub.getMessages(channel, new StreamObserver<Message>() {
            @Override
            public void onNext(Message value) {
                messageModel.addElement(value);
                listener.onMessage(value);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.severe("Error getting messages " + t);
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    public void sendMessage(Message message) {
        stub.sendMessage(OutgoingMessage.newBuilder()
                        .setMessage(message)
                        .setChannel(channel).build(),
                new StreamObserver<MessageResponse>() {
                    @Override
                    public void onNext(MessageResponse value) {
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.severe("Message failed to send " + t);
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.info("Message sent successfully");
                    }
                });
    }

    @Override
    public String toString() {
        return channel.getName();
    }

    public Channel getChannel() {
        return channel;
    }

    public DefaultListModel<Message> getMessageModel() {
        return messageModel;
    }
}
