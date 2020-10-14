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

    public ChatServer(String mqHost, int mqPort, int port, ServerDatabase db) throws IOException, TimeoutException {
        this.port = port;
        server = ServerBuilder
                .forPort(port)
                .addService(new ChatService(mqHost, mqPort, db))
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
    }

    public static void main(String[] argv) throws IOException, TimeoutException {
        Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        LOGGER.info("Launching server");

        ServerDatabase db = new ServerDatabase(args.mongoUrl);
        new ChatServer(args.mqHost, args.mqPort, args.port, db).start();
    }
}
