package sh.swisschili.chat.client;

import io.grpc.ManagedChannelBuilder;
import sh.swisschili.chat.ChatGrpc;
import sh.swisschili.chat.ChatGrpc.ChatStub;
import io.grpc.Channel;
import sh.swisschili.chat.util.Constants;

import java.util.HashMap;

/**
 * Pooled server gRPC connections
 */
public class ServerPool {
    private final HashMap<String, ChatStub> stubs = new HashMap<>();

    /**
     * Connect to a server, returning an existing connection if already connected.
     * @param server Server host
     * @return gRPC stub
     */
    public ChatStub stubFor(String server) {
        if (!stubs.containsKey(server)) {
            Channel channel = ManagedChannelBuilder.forAddress(server, Constants.DEFAULT_SERVER_PORT)
                    .usePlaintext()
                    .build();
            ChatStub stub = ChatGrpc.newStub(channel);
            stubs.put(server, stub);
        }

        return stubs.get(server);
    }
}
