package dev.dhdf.polo.webclient;

/**
 * This reads from the config.yaml and inserts them into their
 * corresponding properties
 */
public class Config {
    public final String address;
    public final int port;
    public final String token;

    public Config(String address, int port, String token) {
        this.address = address;
        this.port = port;
        this.token = token;
    }
}
