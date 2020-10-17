package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


/**
 * This is a base class for listeners of events happening on Minecraft.
 */
public class PoloListener {
    protected final Plugin plugin;
    protected final WebClient client;
    private final BukkitHelper helper;

    public PoloListener(Plugin plugin, WebClient client) {
        this.plugin = plugin;
        this.client = client;
        this.helper = new BukkitHelper();
    }

    protected PoloPlayer newPoloPlayer(Player player) {
        return new PoloPlayer(player.getName(), player.getUniqueId(), helper.getTexture(player));
    }
}
