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
import lombok.extern.slf4j.Slf4j;
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
    private KrakenClient krakenClient;

    @Getter
    private final Map<String, Plugin> pluginMap = new HashMap<>();

    @Getter
    private final Map<String, String> licenseKeys = new HashMap<>();


    private List<Class<?>> pluginClasses = new ArrayList<>();


    private static final String PACKAGE_NAME = "com/krakenplugins";
    private static final String PLUGIN_BASE_CLASS_NAME = "net.runelite.client.plugins.Plugin";


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

     /**
     * Invokes RuneLite's plugin manager to side load the plugins, enabling and registering them with the EventBus. This
      * method should only be called after all the plugins are loaded from the various JAR files through the .loadPlugin() method.
     */
    public void startKrakenPlugins(CognitoUser user) {
        try {
            // Load, enable, and start the plugins with RuneLite, so they can be registered with the EventBus
            List<Plugin> plugins = pluginManager.loadPlugins(pluginClasses, null);
            licenseKeys.keySet().forEach(k -> log.info("Licence Key plugin: {}", k));
            for (Plugin plugin : plugins) {
                krakenClient.validateLicense(new ValidateLicenseRequest(user.getCredentials(), licenseKeys.get(plugin.getName()), HardwareUtils.getHardwareId()));

                pluginManager.setPluginEnabled(plugin, true);
                pluginManager.startPlugin(plugin);
                pluginMap.put(plugin.getName(), plugin);
            }
            log.info("Loaded {} Kraken plugin{}", plugins.size(), plugins.size() > 1 ? "s" : "");
        } catch(Exception e) {
            log.error("Exception thrown while attempting to invoke ExternalPluginManager refresh. Error = {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
