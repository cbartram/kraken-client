package com.kraken;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.loader.JarResourceLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class KrakenPluginManager {

    private final PluginManager pluginManager;

    private List<Class<?>> pluginClasses;

    @Getter
    private final Map<String, Plugin> pluginMap = new HashMap<>();

    private static final String PACKAGE_NAME = "com/krakenplugins";

    @Inject
    public KrakenPluginManager(JarResourceLoader jarLoader, PluginManager pluginManager) {
        this.pluginManager = pluginManager;

        try {
            this.pluginClasses = jarLoader.loadPluginClasses(PACKAGE_NAME);
            log.info("Loaded {} Kraken plugin class{}.", pluginClasses.size(), pluginClasses.size() > 1 ? "es" : "");

            for (Class<?> pluginClass : pluginClasses) {
                // Unchecked cast is OK because the JAR loader checks that each class extends Plugin.
                Plugin plugin = this.instantiate((Class<Plugin>) pluginClass);
                this.pluginMap.put(plugin.getName(), plugin);
            }

            log.info("Loaded {} Kraken plugin(s)", this.pluginMap.size());
        } catch (MalformedURLException | PluginInstantiationException e) {
            log.error("Failed to load plugin classes from Jar file.", e);
        }
    }

    /**
     * Creates an instance of a Kraken plugin using reflection.
     * Note: this method skips a lot of the RuneLite dependency checking and guice module bindings so this plugin won't
     * function as a normal RuneLite plugin. However, these same plugins are loaded into RuneLite correctly
     * with loadKrakenPlugins(). These plugins simply exist to distinguish Kraken vs RL plugins in the UI NOT to
     * actually run in game.
     * @param clazz Loaded class which extends the RuneLite plugin class.
     * @return Plugin a loaded Plugin object.
     * @throws PluginInstantiationException
     */
    private Plugin instantiate(Class<Plugin> clazz) throws PluginInstantiationException {
		Plugin plugin;
		try {
			plugin = clazz.getDeclaredConstructor().newInstance();
		} catch (ThreadDeath e) {
            log.error("Thread death while trying to instantiate plugin class {}.", clazz.getName(), e);
			throw e;
		} catch (Throwable ex) {
			throw new PluginInstantiationException(ex);
		}

		return plugin;
	}

     /**
     * Reads the downloaded JAR files, finds the Plugin classes, and invokes RuneLite's plugin manager
     * to side load the plugins.
     */
    public void loadKrakenPlugins() {
        try {
            // Load, enable, and start the plugins with RuneLite, so they can be registered with the EventBus
            List<Plugin> plugins = pluginManager.loadPlugins(pluginClasses, null);

            for (Plugin plugin : plugins) {
                pluginManager.setPluginEnabled(plugin, true);
                pluginManager.startPlugin(plugin);
            }
        } catch(Exception e) {
            log.error("Exception thrown while attempting to invoke ExternalPluginManager refresh. Error = {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
