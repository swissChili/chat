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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ChatServer {
    private final int port;
    private final Server server;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServer.class.getName());

    public ChatServer(ServerDatabase db, Args args) throws IOException, TimeoutException {
        this.port = args.port;
        ChatService chatService = new ChatService(args.mqHost, args.mqPort, db);
        chatService.setAllowUnsignedMessages(args.unsafe);
        ServerBuilder<?> builder = ServerBuilder
                .forPort(port)
                .addService(chatService)
                .addService(new AuthService(db, args.host));

        if (args.ssl) {
            builder = builder.useTransportSecurity(new File(args.certificate), new File(args.privateKey));
        }

        server = builder.build();
    }

    public void start() throws IOException {
        LOGGER.info("Starting server on port " + port);
        server.start();
    }

    private static class Args {
        @Parameter(names = {"--port", "-p"}, description = "Port to run server on")
        private Integer port = Constants.DEFAULT_SERVER_PORT;

        @Parameter(names = "--mq-host", description = "RabbitMQ host")
        private String mqHost = "localhost";

        @Parameter(names = "--mq-port", description = "RabbitMQ port")
        private Integer mqPort = 5672;

        @Parameter(names = { "--db-url", "-d" }, description = "MongoDB connection url", required = true)
        private String mongoUrl;

        @Parameter(names = { "-H", "--host" }, description = "Host name of server (default: localhost)")
        private String host = "localhost";

        @Parameter(names = { "--unsafe", "-u" }, description = "Forego signature validation (DO NOT use in production)")
        private Boolean unsafe = false;

        @Parameter(names = { "--ssl", "-s" }, description = "Use SSL")
        private Boolean ssl = false;

        @Parameter(names = { "--cert" }, description = "SSL Certificate")
        private String certificate = null;

        @Parameter(names = { "--private-key" }, description = "SSL Private key")
        private String privateKey = null;
    }

    public static void main(String[] argv) throws IOException, TimeoutException {
        Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        System.out.println(Constants.LICENSE_HEADER);

        LOGGER.info("Launching server");

        ServerDatabase db = new ServerDatabase(args.mongoUrl);
        ChatServer server = new ChatServer(db, args);
        server.start();
    }
}
