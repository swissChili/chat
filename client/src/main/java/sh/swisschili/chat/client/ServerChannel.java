package sh.swisschili.chat.client;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos.*;

import javax.swing.*;
import org.slf4j.*;
import sh.swisschili.chat.util.ServerPool;
import sh.swisschili.chat.util.SignedAuth;

import java.security.KeyException;
import java.security.KeyPair;
import java.security.SignatureException;

public class ServerChannel {
    private final ServerPool pool;
    private final Channel channel;
    private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
    private final ChatGrpc.ChatStub stub;
    private final User user;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPool.class.getName());

    public interface MessageListener {
        void onMessage(Message message);
    }

    public ServerChannel(ServerPool pool, String server, Channel channel, User user) {
        this.pool = pool;
        this.channel = channel;
        this.user = user;
        stub = pool.chatStubFor(server);

        stub.getMessages(channel, new StreamObserver<Message>() {
            @Override
            public void onNext(Message value) {
                messageModel.addElement(value);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("Error getting messages " + t);
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    public void sendMessage(Message message, KeyPair keys) {
        try {
            byte[] signature = SignedAuth.sign(keys, message.toByteArray(), channel.toByteArray());

            stub.sendMessage(OutgoingMessage.newBuilder()
                            .setMessage(message)
                            .setChannel(channel)
                            .setSignature(ByteString.copyFrom(signature)).build(),
                    new StreamObserver<MessageResponse>() {
                        @Override
                        public void onNext(MessageResponse value) {
                        }

                        @Override
                        public void onError(Throwable t) {
                            LOGGER.error("Message failed to send " + t);
                        }

                        @Override
                        public void onCompleted() {
                            LOGGER.info("Message sent successfully");
                        }
                    });
        } catch (KeyException | SignatureException ignored) {
        }
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

    public User getUser() {
        return user;
    }
}
