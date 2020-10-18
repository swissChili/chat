package sh.swisschili.chat.client;

import io.grpc.ManagedChannelBuilder;
import sh.swisschili.chat.util.AuthGrpc;
import sh.swisschili.chat.util.AuthGrpc.AuthStub;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatGrpc.ChatStub;
import io.grpc.Channel;
import sh.swisschili.chat.util.Constants;

import java.util.HashMap;

/**
 * Pooled server gRPC connections
 */
public class ServerPool {
    private final HashMap<String, ChatStub> chatStubs = new HashMap<>();
    private final HashMap<String, AuthStub> authStubs = new HashMap<>();

    /**
     * Connect to a server, returning an existing connection if already connected.
     * @param server Server host
     * @return gRPC stub
     */
    public ChatStub chatStubFor(String server) {
        if (!chatStubs.containsKey(server)) {
            Channel channel = ManagedChannelBuilder.forAddress(server, Constants.DEFAULT_SERVER_PORT)
                    .usePlaintext()
                    .build();
            ChatStub stub = ChatGrpc.newStub(channel);
            chatStubs.put(server, stub);
        }

        return chatStubs.get(server);
    }

    public AuthStub authStubFor(String server) {
        if (!authStubs.containsKey(server)) {
            Channel channel = ManagedChannelBuilder.forAddress(server, Constants.DEFAULT_SERVER_PORT)
                    .usePlaintext()
                    .build();
            AuthStub stub = AuthGrpc.newStub(channel);
            authStubs.put(server, stub);
        }

        return authStubs.get(server);
    }
}
