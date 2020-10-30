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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.swisschili.chat.util.ChatProtos;
import sh.swisschili.chat.util.ChatProtos.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

public class ServerDatabase {
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> groups;
    private final MongoCollection<Document> registered;
    private final MongoCollection<Document> userStatuses;
    private final MongoCollection<Document> messages;

    private final PasswordAuthentication auth = new PasswordAuthentication();
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerDatabase.class);

    public static class UserNotFoundException extends Exception {
    }

    public static class UsernameRegisteredException extends Exception {
    }

    public ServerDatabase(String url) {
        ConnectionString connectionString = new ConnectionString(url);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true).build();

        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase db = mongoClient.getDatabase("chat");

        users = db.getCollection("users");
        groups = db.getCollection("groups");
        registered = db.getCollection("registeredUsers");
        userStatuses = db.getCollection("userStatuses");
        messages = db.getCollection("messages");
    }

    /**
     * Create a group with the given name
     *
     * @param name The name to give the group
     * @return The created group
     */
    public Group createGroup(String name) {
        ObjectId id = new ObjectId();
        Document doc = new Document("users", new BsonArray())
                .append("channels", Collections.singletonList(
                        new Document("_id", new ObjectId()).append("name", "general")))
                .append("_id", id)
                .append("name", name);
        groups.insertOne(doc);

        return Group.newBuilder()
                .setName(name)
                .setId(id.toString())
                .build();
    }

    /**
     * Create a channel in a given group
     *
     * @param group The group to create the channel in
     * @param name  The name to give the channel
     * @return The created channel
     */
    public Channel createChannel(Group group, String name) {
        ObjectId id = new ObjectId();
        Document channelDoc = new Document("name", name)
                .append("_id", id);

        Document groupDoc = new Document("_id", new ObjectId(group.getId()));

        Document updateDoc = new Document("$push", new Document("channels", channelDoc));

        groups.findOneAndUpdate(groupDoc, updateDoc);

        return Channel.newBuilder()
                .setId(id.toString())
                .setName(name)
                .build();
    }

    public User getOrAddUser(String name, String host) {
        Document queryDoc = new Document("name", name)
                .append("host", host);

        Document existing = users.find(queryDoc)
                .projection(new Document())
                .first();

        if (existing != null) {
            return User.newBuilder()
                    .setId(existing.getObjectId("_id").toString())
                    .setHost(host)
                    .setName(name)
                    .build();
        }

        ObjectId userId = new ObjectId();
        Document userDoc = new Document(queryDoc)
                .append("_id", userId);
        users.insertOne(userDoc);

        return User.newBuilder()
                .setId(userId.toString())
                .setName(name)
                .setHost(host)
                .build();
    }

    public Group getGroupByName(String name) throws ClassNotFoundException {
        Document doc = groups.find(eq("name", name))
                .projection(new Document("channels", 0)
                        .append("users", 0)).first();

        if (doc == null)
            throw new ClassNotFoundException(String.format("Group %s not found", name));

        return Group.newBuilder()
                .setName(doc.getString("name"))
                .setId(doc.getObjectId("_id").toString())
                .build();
    }

    public List<Channel> getGroupChannels(Group group) {
        List<Channel> channels = new ArrayList<>();

        for (Document doc : groups.aggregate(Arrays.asList(
                Aggregates.match(Filters.eq("_id", new ObjectId(group.getId()))),
                Aggregates.unwind("$channels"),
                Aggregates.replaceRoot("$channels")))) {
            channels.add(Channel.newBuilder()
                    .setId(doc.getObjectId("_id").toString())
                    .setName(doc.getString("name"))
                    .build());
        }

        return channels;
    }

    public void createUser(String name, String password, byte[] publicKey, byte[] privateKey) throws UsernameRegisteredException {
        registered.createIndex(new Document("name", 1), new IndexOptions().unique(true));

        try {
            registered.insertOne(new Document("name", name)
                    .append("password", auth.hash(password.toCharArray()))
                    .append("publicKey", new BsonBinary(publicKey))
                    .append("privateKey", new BsonBinary(privateKey)));
        } catch (Exception e) {
            throw new UsernameRegisteredException();
        }
    }

    public boolean authenticateUser(String name, String password) throws UserNotFoundException {
        Document user = registered.find(eq("name", name))
                .projection(new Document("password", 1)
                        .append("_id", 0)).first();

        if (user == null)
            throw new UserNotFoundException();

        String pass = user.getString("password");
        return auth.authenticate(password.toCharArray(), pass);
    }

    public byte[] getUserPublicKey(String name) throws UserNotFoundException {
        Document user = registered.find(eq("name", name))
                .projection(new Document("publicKey", 1)
                        .append("_id", 0)).first();

        if (user == null)
            throw new UserNotFoundException();

        return user.get("publicKey", Binary.class).getData();
    }

    public void setUserStatus(UserStatus status, Group group) {
        User user = status.getUser();
        int statusValue = -1;
        if (!status.hasCustom()) {
            statusValue = status.getPresenceValue();
        }

        Document userStatus = new Document("userId", new ObjectId(user.getId()))
                .append("name", user.getName())
                .append("host", user.getHost())
                .append("statusValue", statusValue)
                .append("groupId", new ObjectId(group.getId()));

        if (status.hasCustom()) {
            userStatus = userStatus.append("customStatus", status.getCustom().getName());
        }

        userStatuses.insertOne(userStatus);
    }

    public Iterable<UserStatus> getUserStatuses(Group group) {
        return userStatuses.find(new Document("groupId", group.getId()))
                .map(doc -> {
                    User user = User.newBuilder()
                            .setHost(doc.getString("host"))
                            .setName(doc.getString("name"))
                            .setId(doc.getObjectId("userId").toString())
                            .build();
                    UserStatus.Builder status = UserStatus.newBuilder()
                            .setUser(user);
                    int statusValue = doc.getInteger("statusValue", 0);
                    if (statusValue == -1) {
                        status.setCustom(CustomPresence.newBuilder()
                                .setName(doc.getString("customStatus")).build());
                    } else {
                        status.setPresenceValue(statusValue);
                    }

                    return status.build();
                });
    }

    /**
     * Save a message to the database and return its id.
     *
     * @param channel The channel the message was sent in
     * @param message The message
     * @return The message's id
     */
    public ObjectId saveMessage(Channel channel, Message message) {
        ObjectId id = new ObjectId();
        User user = message.getSender();
        Document messageDoc = new Document("body", message.getBody())
                .append("unixTime", message.getUnixTime())
                .append("_id", id)
                .append("senderName", user.getName())
                .append("senderHost", user.getHost())
                .append("senderId", new ObjectId(user.getId()))
                .append("channelId", new ObjectId(channel.getId()));
        messages.insertOne(messageDoc);

        return id;
    }

    public List<Message> getMessageRange(Channel channel, int start, int number) {
        LOGGER.info(String.format("Getting from %d # %d", start, number));
        return StreamSupport.stream(
                messages.find(new Document("channelId", new ObjectId(channel.getId())))
                        .sort(new Document("unixTime", -1))
                        .skip(start)
                        .limit(number)
                        .spliterator(), false)
                .map(doc -> {
                    User sender = User.newBuilder()
                            .setName(doc.getString("senderName"))
                            .setHost(doc.getString("senderHost"))
                            .setId(doc.getObjectId("senderId").toString())
                            .build();

                    return Message.newBuilder()
                            .setId(doc.getObjectId("_id").toString())
                            .setBody(doc.getString("body"))
                            .setUnixTime(doc.getLong("unixTime"))
                            .setSender(sender)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
