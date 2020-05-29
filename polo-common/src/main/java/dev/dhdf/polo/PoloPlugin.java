package dev.dhdf.polo;

public interface PoloPlugin {
    /**
     * Broadcast a Matrix message to the server.
     * @param message The message to broadcast
     */
    public void broadcastMessage(String message);
}
