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
 * This class listens to all the events happening on Minecraft. Currently
 * it only listens to player chat events.
 */
public class MCListener implements Listener {

    private final WebClient client;

    public MCListener(WebClient client) {
        this.client = client;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        Player player = ev.getPlayer();
        String joinMessage = ev.getJoinMessage();

        this.client.postJoin(new PoloPlayer(player.getName(), player.getUniqueId()), joinMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        Player player = ev.getPlayer();
        String quitMessage = ev.getQuitMessage();

        this.client.postQuit(new PoloPlayer(player.getName(), player.getUniqueId()), quitMessage);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent ev) {
        Player player = ev.getPlayer();
        String kickReason = ev.getReason();

        this.client.postKick(new PoloPlayer(player.getName(), player.getUniqueId()), kickReason);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent ev) {
        String body = ev.getMessage();
        Player player = ev.getPlayer();

        if (!ev.isCancelled())
            this.client.postChat(new PoloPlayer(player.getName(), player.getUniqueId()), body);
    }
}
