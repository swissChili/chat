package sh.swisschili.chat.server;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class ServerDatabase {
    private final MongoClient mongoClient;
    private final MongoDatabase db;

    public ServerDatabase(String url) {
        ConnectionString connectionString = new ConnectionString(url);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true).build();

        mongoClient = MongoClients.create(settings);
        db = mongoClient.getDatabase("chat");
    }
}
