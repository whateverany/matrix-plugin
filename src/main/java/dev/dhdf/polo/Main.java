package dev.dhdf.polo;

import dev.dhdf.polo.mc.MCListener;
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
public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        // First read from the config
        FileConfiguration pluginConfig = this.getConfig();
        Logger logger = getLogger();
        boolean isValid = checkIntegrity(pluginConfig);

        logger.info("Started Polo plugin");

        if (!isValid) {
            pluginConfig = this.genConfig();
            logger.warning("Generated default config");
            logger.severe("Please modify the configuration");
        }

        Config config = new Config(pluginConfig);

        // Start up the web client
        WebClient webClient = new WebClient(
                this.getServer(),
                config
        );
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

        if (address == null)
            return false;
        else if (token == null)
            return false;
        else
            return (port == 0);
    }

    /**
     * This gets the default config.yaml and returns it
     *
     * @return FileConfiguration
     */
    private FileConfiguration genConfig() {
        this.saveDefaultConfig();
        return this.getConfig();
    }
}
