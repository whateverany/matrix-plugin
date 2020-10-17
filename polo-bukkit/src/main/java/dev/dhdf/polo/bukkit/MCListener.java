package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;


/**
 * This class listens to all the standard bukkit events happening on Minecraft.
 */
public class MCListener extends PoloListener implements Listener {
    public MCListener(WebClient client) {
        super(client);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        Player player = ev.getPlayer();
        PoloPlayer poloPlayer = newPoloPlayer(player);

        this.client.postJoin(poloPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        Player player = ev.getPlayer();
        PoloPlayer poloPlayer = newPoloPlayer(player);

        this.client.postQuit(poloPlayer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent ev) {
        Player player = ev.getPlayer();
        String kickReason = ev.getReason();
        PoloPlayer poloPlayer = newPoloPlayer(player);

        this.client.postKick(poloPlayer, kickReason);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent ev) {
        String body = ev.getMessage();
        Player player = ev.getPlayer();
        PoloPlayer poloPlayer = newPoloPlayer(player);

        this.client.postChat(poloPlayer, body);
    }
}
