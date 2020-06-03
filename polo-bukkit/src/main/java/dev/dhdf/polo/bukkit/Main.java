package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.util.Sync;
import dev.dhdf.polo.webclient.Config;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


/**
 * This starts the plugin
 */
public class Main extends JavaPlugin implements PoloPlugin {
    @Override
    public void onEnable() {
        // Save the default config if it isn't already there
        this.saveDefaultConfig();

        // First read from the config
        FileConfiguration pluginConfig = this.getConfig();
        Logger logger = getLogger();
        boolean isValid = checkIntegrity(pluginConfig);

        logger.info("Started Polo plugin");

        if (!isValid)
            logger.severe("Incomplete config.yml, please modify");

        Config config = new Config(
                pluginConfig.getString("address"),
                pluginConfig.getInt("port"),
                pluginConfig.getString("token")
        );

        // Start up the web client
        WebClient webClient = new WebClient(
                this,
                config
        );

        // Make the WebClient log through our PluginLogger
        Logger.getLogger(WebClient.class.getName()).setParent(getLogger());

        // Start up the Minecraft event listener
        MCListener mcListener = new MCListener(webClient);

        getServer().getPluginManager().registerEvents(mcListener, this);

        logger.info("Started webclient and chat listeners");

        // See if the address and port are pointing to Marco
        boolean vibeCheck = webClient.vibeCheck();

        if (vibeCheck) {
            logger.finer("Started bridge");
            Timer timer = new Timer();
            TimerTask timerTask = new Sync(webClient);
            timer.scheduleAtFixedRate(
                    timerTask,
                    0,
                    5000
            );
        } else {
            logger.severe("Couldn't properly connect to marco is the address and port set properly?");
        }
    }

    @Override
    public void onDisable() {
    }


    /**
     * This checks the integrity of the config.yaml
     *
     * @param config The config to review
     * @return {boolean}
     */
    private boolean checkIntegrity(FileConfiguration config) {
        String address = config.getString("address");
        String token = config.getString("token");
        int port = config.getInt("port");

        if (address == null || address.isEmpty())
            return false;
        else if (token == null || token.isEmpty())
            return false;
        else
            return (port != 0);
    }

    @Override
    public void broadcastMessage(String message) {
        this.getServer().broadcastMessage(message);
    }
}
