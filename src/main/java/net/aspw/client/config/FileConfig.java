package net.aspw.client.config;

import java.io.File;
import java.io.IOException;

public abstract class FileConfig {

    private final File file;

    public FileConfig(final File file) {
        this.file = file;
    }

    protected abstract void loadConfig() throws IOException;

    protected abstract void saveConfig() throws IOException;

    public void createConfig() throws IOException {
        file.createNewFile();
    }

    public boolean hasConfig() {
        return file.exists();
    }

    public File getFile() {
        return file;
    }
}
