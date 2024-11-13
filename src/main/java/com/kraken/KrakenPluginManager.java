package com.kraken;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.KrakenClient;
import com.kraken.api.model.CognitoUser;
import com.kraken.api.model.PreSignedURL;
import com.kraken.api.model.ValidateLicenseRequest;
import com.kraken.loader.ByteArrayClassLoader;
import com.kraken.loader.JarResourceLoader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class KrakenPluginManager {

    @Inject
    private JarResourceLoader jarResourceLoader;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private KrakenClient krakenClient;

    @Getter
    private final Map<String, Plugin> pluginMap = new HashMap<>();

    private final List<Class<?>> pluginClasses = new ArrayList<>();

    // Tracks plugins which have passed the license verification.
    @Getter
    private final Map<String, Boolean> verifiedPlugins = new HashMap<>();

    private static final String PACKAGE_NAME = "com/krakenplugins";
    private static final String PLUGIN_BASE_CLASS_NAME = "net.runelite.client.plugins.Plugin";

    @Getter
    @Setter
    private CognitoUser user;


    /**
     * Iterate through each class which begins with com/krakenplugins in the JAR file, load each class
     * check its superclass for one that extends the RuneLite Plugin class. If superclass matches then add the class
     *  to a list. The Class will be loaded and cast to a Plugin object when it is passed to RuneLite
     * @param url PreSignedURL A presigned URL containing the JAR file for the plugin in S3.
     */
    public void loadPlugin(PreSignedURL url) {
        try(ByteArrayClassLoader loader = jarResourceLoader.loadJarFromSignedUrl(PACKAGE_NAME, url)) {

            // Iterate through each class which begins with com/krakenplugins in the JAR file, load each class
            // check its superclass for one that extends the RuneLite Plugin class. If superclass matches then add the class
            // to a list. The list
            for (String className : loader.getClassData().keySet()) {
                try {
                    Class<?> clazz = loader.loadClass(className);
                    if (clazz.getSuperclass() != null) {
                        if (clazz.getSuperclass().getName().equals(PLUGIN_BASE_CLASS_NAME)) {
                            log.debug("Main Plugin Class located: {}", className);
                            this.pluginClasses.add(clazz);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Class: {} could not be found. Error = {}", className, e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            log.error("IOException thrown while attempting to load JAR from signed URL: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasValidLicense(ValidateLicenseRequest validateLicenseRequest) {
        return krakenClient.validateLicense(validateLicenseRequest);
    }

     /**
     * Invokes RuneLite's plugin manager to side load the plugins, enabling and registering them with the EventBus. This
      * method should only be called after all the plugins are loaded from the various JAR files through the .loadPlugin() method.
     */
    public void startKrakenPlugins() {
        if(user == null) {
            log.info("User is null. Cannot validate plugins with null user.");
            return;
        }

        try {
            // Load, enable, and start the plugins with RuneLite, so they can be registered with the EventBus
            List<Plugin> plugins = pluginManager.loadPlugins(pluginClasses, null);

            for (Plugin plugin : plugins) {
                String licenseKey;
                Config config = pluginManager.getPluginConfigProxy(plugin);
                ConfigDescriptor configDescriptor = config == null ? null : configManager.getConfigDescriptor(config);

                if(configDescriptor != null) {
                    licenseKey = configManager.getConfiguration(configDescriptor.getGroup().value(), "licenseKey");
                } else {
                    log.error("Failed to get license key from plugin config for: {}", plugin.getName());
                    continue;
                }

                ValidateLicenseRequest req = new ValidateLicenseRequest(user.getCredentials(), licenseKey, HardwareUtils.getHardwareId());
                if(hasValidLicense(req)) {
                    verifiedPlugins.put(plugin.getName(), true);
                    pluginManager.startPlugin(plugin);
                } else {
                    log.info("License key provided: {} is not valid.", licenseKey);
                    verifiedPlugins.put(plugin.getName(), false);
                }
                pluginMap.put(plugin.getName(), plugin);
                pluginManager.setPluginEnabled(plugin, true);
            }
            log.info("Loaded {} Kraken plugin{}", plugins.size(), plugins.size() > 1 ? "s" : "");
        } catch(Exception e) {
            log.error("Exception thrown while attempting to invoke ExternalPluginManager refresh. Error = {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
