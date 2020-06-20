package dev.dhdf.polo.webclient;

/**
 * This reads from the config.yaml and inserts them into their
 * corresponding properties
 */
public class Config {
    public final String address;
    public final int port;
    public final String token;
    public final boolean relayMinecraftMembership;

    public Config(String address, int port, String token,
                  boolean relayMinecraftMembership) {
        this.address = address;
        this.port = port;
        this.token = token;
        this.relayMinecraftMembership = relayMinecraftMembership;
    }
}
