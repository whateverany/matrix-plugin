package dev.dhdf.polo.webclient.types;

import java.util.UUID;

public class PoloPlayer {
    public final String name;
    public final String uuid;

    public PoloPlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid.toString().replace("-", "");
    }
}
