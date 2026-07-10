package net.baizhi.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.baizhi.client.Launch;
import net.baizhi.client.config.configs.AccountsConfig;
import net.baizhi.client.config.configs.FriendsConfig;
import net.baizhi.client.config.configs.ModulesConfig;
import net.baizhi.client.config.configs.ValuesConfig;
import net.baizhi.client.utils.ClientUtils;
import net.baizhi.client.utils.MinecraftInstance;

import java.io.File;
import java.lang.reflect.Field;

public class FileManager extends MinecraftInstance {

    public static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    public File dir = new File(mc.mcDataDir, Launch.CLIENT_FOLDER);

    public final File settingsDir = new File(dir, "configs");

    public final File soundsDir = new File(dir, "sounds");

    public final FileConfig modulesConfig = new ModulesConfig(new File(dir, "toggled.json"));

    public final FileConfig valuesConfig = new ValuesConfig(new File(dir, "value.json"));

    public final AccountsConfig accountsConfig = new AccountsConfig(new File(dir, "alts.json"));

    public final FriendsConfig friendsConfig = new FriendsConfig(new File(dir, "friends.json"));

    public FileManager() {
        setupFolder();
    }

    public void setupFolder() {
        if (!dir.exists())
            dir.mkdir();

        if (!settingsDir.exists())
            settingsDir.mkdir();

        if (!soundsDir.exists())
            soundsDir.mkdir();
    }

    public void loadConfigs(final FileConfig... configs) {
        for (final FileConfig fileConfig : configs)
            loadConfig(fileConfig);
    }

    public void loadConfig(final FileConfig config) {
        if (!config.hasConfig()) {
            ClientUtils.getLogger().info("[FileManager] Skipped loading config: " + config.getFile().getName() + ".");

            saveConfig(config, true);
            return;
        }

        try {
            config.loadConfig();
            ClientUtils.getLogger().info("[FileManager] Loaded config: " + config.getFile().getName() + ".");
        } catch (final Throwable t) {
            ClientUtils.getLogger().error("[FileManager] Failed to load config file: " + config.getFile().getName() + ".", t);
        }
    }

    public void saveAllConfigs() {
        for (final Field field : getClass().getDeclaredFields()) {
            if (field.getType() == FileConfig.class) {
                try {
                    if (!field.isAccessible())
                        field.setAccessible(true);

                    final FileConfig fileConfig = (FileConfig) field.get(this);
                    saveConfig(fileConfig);
                } catch (final IllegalAccessException e) {
                    ClientUtils.getLogger().error("[FileManager] Failed to save config file of field " +
                            field.getName() + ".", e);
                }
            }
        }
    }

    public void saveConfig(final FileConfig config) {
        saveConfig(config, false);
    }

    private void saveConfig(final FileConfig config, final boolean ignoreStarting) {
        if (!ignoreStarting && Launch.INSTANCE.isStarting())
            return;

        try {
            if (!config.hasConfig())
                config.createConfig();

            config.saveConfig();
            ClientUtils.getLogger().info("[FileManager] Saved config: " + config.getFile().getName() + ".");
        } catch (final Throwable t) {
            ClientUtils.getLogger().error("[FileManager] Failed to save config file: " +
                    config.getFile().getName() + ".", t);
        }
    }
}
