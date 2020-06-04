package dev.dhdf.polo;

public interface PoloPlugin {
    /**
     * Broadcast a Matrix message to the server.
     * @param message The message to broadcast
     */
    public void broadcastMessage(String message);

    /**
     * Execute a task asynchronously using the plugin's scheduler.
     * @param task The task to execute asynchronously
     */
    public void executeAsync(Runnable task);
}
