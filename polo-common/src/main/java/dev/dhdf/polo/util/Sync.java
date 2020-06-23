package dev.dhdf.polo.util;

import dev.dhdf.polo.webclient.WebClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;


/**
 * This class gets the chat from Marco in a set interval
 * (see Main class)
 */
public class Sync implements Runnable {
    private final WebClient client;
    private final Logger logger = LoggerFactory.getLogger(Sync.class);

    private boolean get_events;

    public Sync(WebClient client) {
        this.client = client;
        this.get_events = true;
    }

    @Override
    public void run() {
        boolean success = false;

        // Default to the events endpoint unless we know it isn't supported
        if (get_events)
            success = this.client.getEvents();

        // Fall back to the legacy plain chat endpoint
        if (!success) {
            success = this.client.getChat();

            if (!success) {
                // Even the legacy endpoint failed, next time try the events
                // endpoint too
                get_events = true;
            } else  if (get_events) {
                // It worked! just use this in future
                logger.warn("Falling back to legacy chat endpoint, please update matrix-appservice-minecraft");
                get_events = false;
            }
        }
    }
}
