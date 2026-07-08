package net.aspw.client.config.configs;

import com.google.gson.*;
import net.aspw.client.config.FileConfig;
import net.aspw.client.config.FileManager;
import net.aspw.client.utils.ClientUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FriendsConfig extends FileConfig {

    private final List<Friend> friends = new ArrayList<>();

    public FriendsConfig(final File file) {
        super(file);
    }

    @Override
    protected void loadConfig() throws IOException {
        clearFriends();
        try {
            final JsonElement jsonElement = new JsonParser().parse(new BufferedReader(new FileReader(getFile())));

            if (jsonElement instanceof JsonNull)
                return;

            for (final JsonElement friendElement : jsonElement.getAsJsonArray()) {
                final JsonObject friendObject = friendElement.getAsJsonObject();
                addFriend(friendObject.get("playerName").getAsString(), friendObject.get("alias").getAsString());
            }

        } catch (final JsonSyntaxException | IllegalStateException ex) {

            ClientUtils.getLogger().info("[FileManager] Try to load old Friends config...");

            final BufferedReader bufferedReader = new BufferedReader(new FileReader(getFile()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.contains("{") && !line.contains("}")) {
                    line = line.replace(" ", "").replace("\"", "").replace(",", "");

                    if (line.contains(":")) {
                        final String[] data = line.split(":");
                        addFriend(data[0], data[1]);
                    } else
                        addFriend(line);
                }
            }
            bufferedReader.close();
            ClientUtils.getLogger().info("[FileManager] Loaded old Friends config...");

            saveConfig();
            ClientUtils.getLogger().info("[FileManager] Saved Friends to new config...");
        }
    }

    @Override
    protected void saveConfig() throws IOException {
        final JsonArray jsonArray = new JsonArray();

        for (final Friend friend : getFriends()) {
            final JsonObject friendObject = new JsonObject();
            friendObject.addProperty("playerName", friend.getPlayerName());
            friendObject.addProperty("alias", friend.getAlias());
            jsonArray.add(friendObject);
        }

        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(FileManager.PRETTY_GSON.toJson(jsonArray));
        printWriter.close();
    }

    public boolean addFriend(final String playerName) {
        return addFriend(playerName, playerName);
    }

    public boolean addFriend(final String playerName, final String alias) {
        if (isFriend(playerName))
            return false;

        friends.add(new Friend(playerName, alias));
        return true;
    }

    public boolean removeFriend(final String playerName) {
        if (!isFriend(playerName))
            return false;

        friends.removeIf(friend -> friend.getPlayerName().equals(playerName));
        return true;
    }

    public boolean isFriend(final String playerName) {
        for (final Friend friend : friends)
            if (friend.getPlayerName().equals(playerName))
                return true;
        return false;
    }

    public void clearFriends() {
        friends.clear();
    }

    public List<Friend> getFriends() {
        return friends;
    }

    public static class Friend {

        private final String playerName;
        private final String alias;

        Friend(final String playerName, final String alias) {
            this.playerName = playerName;
            this.alias = alias;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getAlias() {
            return alias;
        }
    }
}
