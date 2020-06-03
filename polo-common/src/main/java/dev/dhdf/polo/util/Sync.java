package dev.dhdf.polo.util;

import dev.dhdf.polo.webclient.WebClient;

import java.util.TimerTask;


/**
 * This class gets the chat from Marco in a set interval
 * (see Main class)
 */
public class Sync implements Runnable {
    private final WebClient client;

    public Sync(WebClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        this.client.getChat();
    }
}
