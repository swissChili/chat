/*
Decentralized chat software
Copyright (C) 2020  swissChili

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
import sh.swisschili.chat.util.Constants;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.*;

public class ChatServer {
    private final int port;
    private final Server server;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServer.class.getName());

    public ChatServer(String host, String mqHost, int mqPort, int port, ServerDatabase db, boolean unsafe) throws IOException, TimeoutException {
        this.port = port;
        ChatService chatService = new ChatService(mqHost, mqPort, db);
        chatService.setAllowUnsignedMessages(unsafe);
        server = ServerBuilder
                .forPort(port)
                .addService(chatService)
                .addService(new AuthService(db, host))
                .build();
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
        ChatServer server = new ChatServer(args.host, args.mqHost, args.mqPort, args.port, db, args.unsafe);
        server.start();
    }
}
