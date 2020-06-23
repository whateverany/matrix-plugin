package dev.dhdf.polo.webclient;

/**
 * This reads from the config.yaml and inserts them into their
 * corresponding properties
 */
public class Config {
    public final String address;
    public final int port;
    public final String token;
    public final boolean relay_mc_membership;
    public final boolean relay_mx_kicks;
    public final boolean relay_mx_bans;

    public Config(String address, int port, String token,
                  boolean relay_mc_membership,
                  boolean relay_mx_kicks,
                  boolean relay_mx_bans) {
        this.address = address;
        this.port = port;
        this.token = token;
        this.relay_mc_membership = relay_mc_membership;
        this.relay_mx_kicks = relay_mx_kicks;
        this.relay_mx_bans = relay_mx_bans;
    }
}
