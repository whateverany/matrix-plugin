package dev.dhdf.polo.webclient;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * This reads from the config.yaml and inserts them into their
 * corresponding properties
 */
public class Config {
    public final String address;
    public final int port;
    public final String token;

    public Config(FileConfiguration config) {
        address = config.getString("address");
        token = config.getString("token");
        port = config.getInt("port");
    }
}
