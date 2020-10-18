package sh.swisschili.chat.server;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.AuthGrpc;
import sh.swisschili.chat.util.ChatGrpc;
import sh.swisschili.chat.util.ChatProtos;

public class AuthService extends AuthGrpc.AuthImplBase {
    private final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final ServerDatabase database;
    private final String host;

    public AuthService(ServerDatabase database, String host) {
        this.database = database;
        this.host = host;
    }

    @Override
    public void signIn(ChatProtos.SignInRequest request, StreamObserver<ChatProtos.SignInResponse> responseObserver) {
        try {
            if (database.authenticateUser(request.getName(), request.getPassword())) {
                LOGGER.info("User logged in");

                responseObserver.onNext(ChatProtos.SignInResponse.newBuilder()
                        .setUser(ChatProtos.User.newBuilder()
                                .setName(request.getName())
                                .setHost(host).build())
                        .build());
                responseObserver.onCompleted();
            } else {
                LOGGER.info("User failed to log in (invalid credentials)");
            }
        } catch (ServerDatabase.UserNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void register(ChatProtos.RegisterRequest request, StreamObserver<ChatProtos.RegisterResponse> responseObserver) {
        try {
            database.createUser(request.getName(), request.getPassword(), request.getPublicKey().toByteArray(),
                    request.getPrivateKey().toByteArray());

            LOGGER.info("Created user");

            responseObserver.onNext(ChatProtos.RegisterResponse.newBuilder()
                    .setUser(ChatProtos.User.newBuilder()
                            .setName(request.getName())
                            .setHost(host).build())
                    .build());
            responseObserver.onCompleted();
        } catch (ServerDatabase.UsernameRegisteredException e) {
            responseObserver.onError(e);
        }
    }
}
