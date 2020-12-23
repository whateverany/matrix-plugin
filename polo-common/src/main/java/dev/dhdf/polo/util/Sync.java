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

    private boolean supportsEvents;

    public Sync(WebClient client) {
        this.client = client;
        this.supportsEvents = true;
    }

    @Override
    public void run() {
        // Default to the events endpoint unless we know it isn't supported
        if (!supportsEvents || !this.client.getEvents()) {
            // Fall back to the legacy plain chat endpoint
            if (!this.client.getChat()) {
                // Even the legacy endpoint failed, next time try the events
                // endpoint too
                supportsEvents = true;
            } else if (supportsEvents) {
                // It worked! just use this in future
                logger.warn("Falling back to legacy chat endpoint, please update matrix-appservice-minecraft");
                supportsEvents = false;
            }
        }
    }
}
