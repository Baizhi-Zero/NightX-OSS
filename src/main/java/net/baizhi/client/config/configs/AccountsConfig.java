package net.baizhi.client.config.configs;

import com.google.gson.*;
import net.baizhi.client.auth.account.CrackedAccount;
import net.baizhi.client.auth.account.MinecraftAccount;
import net.baizhi.client.auth.manage.AccountSerializer;
import net.baizhi.client.config.FileConfig;
import net.baizhi.client.config.FileManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AccountsConfig extends FileConfig {
    private final List<MinecraftAccount> accounts = new ArrayList<>();

    public AccountsConfig(final File file) {
        super(file);
    }

    @Override
    protected void loadConfig() throws IOException {
        clearAccounts();

        final JsonElement jsonElement = new JsonParser().parse(new BufferedReader(new FileReader(getFile())));

        if (jsonElement instanceof JsonNull)
            return;

        for (final JsonElement accountElement : jsonElement.getAsJsonArray()) {
            final JsonObject accountObject = accountElement.getAsJsonObject();

            try {

                accounts.add(AccountSerializer.INSTANCE.fromJson(accountElement.getAsJsonObject()));
            } catch (JsonSyntaxException | IllegalStateException e) {

                JsonElement name = accountObject.get("name");

                final CrackedAccount crackedAccount = new CrackedAccount();

                crackedAccount.setName(name.getAsString());

                accounts.add(crackedAccount);
            }
        }
    }

    @Override
    protected void saveConfig() throws IOException {
        final JsonArray jsonArray = new JsonArray();

        for (final MinecraftAccount minecraftAccount : accounts) {
            jsonArray.add(AccountSerializer.INSTANCE.toJson(minecraftAccount));
        }

        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(FileManager.PRETTY_GSON.toJson(jsonArray));
        printWriter.close();
    }

    public void addCrackedAccount(final String name) {
        final CrackedAccount crackedAccount = new CrackedAccount();
        crackedAccount.setName(name);

        if (accountExists(crackedAccount))
            return;

        accounts.add(crackedAccount);
    }

    public void addAccount(final MinecraftAccount account) {
        accounts.add(account);
    }

    public void removeAccount(final int selectedSlot) {
        accounts.remove(selectedSlot);
    }

    public void removeAccount(MinecraftAccount account) {
        accounts.remove(account);
    }

    public boolean accountExists(final MinecraftAccount newAccount) {
        for (final MinecraftAccount minecraftAccount : accounts)
            if (minecraftAccount.getClass().getName().equals(newAccount.getClass().getName()) && minecraftAccount.getName().equals(newAccount.getName()))
                return true;
        return false;
    }

    public void clearAccounts() {
        accounts.clear();
    }

    public List<MinecraftAccount> getAccounts() {
        return accounts;
    }
}
