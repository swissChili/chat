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

package sh.swisschili.chat.util;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import sh.swisschili.chat.util.AuthGrpc;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.AuthGrpc.AuthStub;
import sh.swisschili.chat.util.ChatGrpc.ChatStub;

import java.util.HashMap;

/**
 * Pooled server gRPC connections
 */
public class ServerPool {
    private final HashMap<String, Channel> channels = new HashMap<>();

    private static final boolean useSsl = System.getenv("CHAT_NO_SSL") == null;

    private Channel channelFor(String server) {
        if (!channels.containsKey(server)) {
            ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(server.trim(), Constants.DEFAULT_SERVER_PORT);

            if (useSsl) {
                builder = builder.useTransportSecurity();
            } else {
                builder = builder.usePlaintext();
            }

            Channel channel = builder.build();

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
