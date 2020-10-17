package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;


/**
 * This is a base class for listeners of events happening on Minecraft.
 */
public class PoloListener {
    protected final WebClient client;

    public PoloListener(WebClient client) {
        this.client = client;
    }

    protected PoloPlayer newPoloPlayer(Player player) {
        return new PoloPlayer(player.getName(), player.getUniqueId());
    }
}
