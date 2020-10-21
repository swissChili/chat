package sh.swisschili.chat.client;

import io.grpc.ManagedChannelBuilder;
import sh.swisschili.chat.util.AuthGrpc;
import sh.swisschili.chat.util.AuthGrpc.AuthStub;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatGrpc.ChatStub;
import io.grpc.Channel;
import sh.swisschili.chat.util.ChatProtos;
import sh.swisschili.chat.util.Constants;

import java.util.HashMap;

/**
 * Pooled server gRPC connections
 */
public class ServerPool {
    private final HashMap<String, Channel> channels = new HashMap<>();

    private Channel channelFor(String server) {
        if (!channels.containsKey(server)) {
            Channel channel = ManagedChannelBuilder.forAddress(server, Constants.DEFAULT_SERVER_PORT)
                    .usePlaintext()
                    .build();
            channels.put(server, channel);
            return channel;
        }

        return channels.get(server);
    }

    /**
     * Connect to a server, returning an existing connection if already connected.
     *
     * @param server Server host
     * @return gRPC stub
     */
    public ChatStub chatStubFor(String server) {
        return ChatGrpc.newStub(channelFor(server));
    }

    public AuthStub authStubFor(String server) {
        return AuthGrpc.newStub(channelFor(server));
    }
}
