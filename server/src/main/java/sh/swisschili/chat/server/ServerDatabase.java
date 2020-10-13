package sh.swisschili.chat.server;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import static com.mongodb.client.model.Filters.*;

import org.bson.BsonArray;
import org.bson.Document;
import org.bson.types.ObjectId;

import sh.swisschili.chat.util.ChatProtos.Channel;
import sh.swisschili.chat.util.ChatProtos.Group;
import sh.swisschili.chat.util.ChatProtos.User;

import javax.print.Doc;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ServerDatabase {
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> groups;

    public ServerDatabase(String url) {
        ConnectionString connectionString = new ConnectionString(url);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true).build();

        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase db = mongoClient.getDatabase("chat");

        users = db.getCollection("users");
        groups = db.getCollection("groups");
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

    public User addUser(String name, String host) {
        ObjectId userId = new ObjectId();
        Document userDoc = new Document("name", name)
                .append("host", host)
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
}
