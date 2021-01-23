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

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.AuthGrpc;
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

    @Override
    public void getUserPublicKey(ChatProtos.PublicKeyRequest request, StreamObserver<ChatProtos.UserPublicKey> responseObserver) {
        try {
            byte[] bytes = database.getUserPublicKey(request.getUser().getName());
            responseObserver.onNext(ChatProtos.UserPublicKey.newBuilder()
                    .setPublicKey(ByteString.copyFrom(bytes)).build());
            responseObserver.onCompleted();
        } catch (ServerDatabase.UserNotFoundException e) {
            e.printStackTrace();
        }
    }
}
