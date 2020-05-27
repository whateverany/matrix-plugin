package dev.dhdf.polo.webclient.types;

import org.bukkit.entity.Player;


public class PoloPlayer {
    public final String name;
    public final String uuid;

    public PoloPlayer(Player player) {
        this.name = player.getDisplayName();
        this.uuid = player.getUniqueId().toString().replace("-", "");
    }
}
