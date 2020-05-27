package dev.dhdf.polo.mc;

import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    public void onPlayerChat(AsyncPlayerChatEvent ev) {
        String body = ev.getMessage();

        this.client.postChat(ev.getPlayer(), body);
    }
}
