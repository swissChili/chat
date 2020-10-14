package sh.swisschili.chat.client;

import com.google.gson.Gson;

import java.io.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesObject {
    private static final Gson gson = new Gson();
    public static<T> void putObject(Preferences prefs, String key, T o) throws IOException {
        String serialized = gson.toJson(o);
        System.out.println(serialized);
        prefs.put(key, serialized);
    }

    public static<T> T getObject(Preferences prefs, String key, Class<T> typeParameter) throws BackingStoreException, IOException, ClassNotFoundException {
        String serialized = prefs.get(key, "");
        if (serialized.isEmpty())
            return null;

        return gson.fromJson(serialized, typeParameter);
    }
}
