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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerChannel {
    private final ServerPool pool;
    private final Channel channel;
    private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
    private final ChatGrpc.ChatStub stub;
    private final User user;
    private final LinkedList<ItemAddedListener> itemAddedListeners = new LinkedList<>();

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
                SwingUtilities.invokeLater(() -> {
                    itemAddedListeners.forEach(ItemAddedListener::beforeItemAdded);
                    messageModel.addElement(value);
                    itemAddedListeners.forEach(ItemAddedListener::afterItemAdded);
                });
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

    public InfiniteScrollPane.BufferedLoader<Message> getBufferedLoader() {
        return new InfiniteScrollPane.BufferedLoader<>() {
            @Override
            public CompletableFuture<Void> loadMore(int number) {
                LOGGER.info(String.format("Starting to fetch from %d length %d", messageModel.getSize(), number));

                CompletableFuture<Void> future = new CompletableFuture<>();
                stub.getMessageRange(MessageRangeRequest.newBuilder()
                                .setFrom(messageModel.getSize())
                                .setCount(number)
                                .setChannel(channel).build(),
                        new StreamObserver<>() {
                            @Override
                            public void onNext(MessageRangeResponse value) {
//                                for (int i = value.getMessagesCount() - 1; i >= 0; i--) {
                                SwingUtilities.invokeLater(() -> {
                                    for (int i = 0; i < value.getMessagesCount(); i++) {
                                        messageModel.add(0, value.getMessagesList().get(i));
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable t) {
                                future.completeExceptionally(t);
                            }

                            @Override
                            public void onCompleted() {
                                LOGGER.info("getRange COMPLETED");
                                future.complete(null);
                            }

                        });
                return future;
            }

            @Override
            public ListModel<Message> getListModel() {
                return messageModel;
            }

            @Override
            public void addItemAddedListener(ItemAddedListener listener) {
                itemAddedListeners.add(listener);
            }

            @Override
            public void removeItemAddedListener(ItemAddedListener listener) {
                itemAddedListeners.remove(listener);
            }
        };
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
