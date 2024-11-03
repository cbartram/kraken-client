package com.kraken.loader;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;


@Slf4j
public class JarResourceLoader {

    private final List<String> jarPaths = new ArrayList<>();
    private static final String PLUGIN_BASE_CLASS_NAME = "net.runelite.client.plugins.Plugin";

    /**
     * Handles finding and loading the right JAR files which contain compiled plugin classes. The constructor
     * will automatically search the resources folder to locate any JAR files.
     *
     * TODO in the future we need some safety feature here to prevent malicious jars from automatically being loaded.
     */
    public JarResourceLoader() {
        Path resourcesDir = Paths.get("src/main/resources");
        try (Stream<Path> paths = Files.walk(resourcesDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(e -> jarPaths.add(e.toFile().getPath()));

            log.info("Found: {} Kraken jar files.", jarPaths.size());
        } catch (IOException e) {
            log.error("Error reading jar file paths: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the class which extends net.runelite.client.plugins.Plugin for each discovered JAR file.
     * @return List of Plugin classes.
     * @throws MalformedURLException
     */
    public List<Class<?>> loadPluginClasses(final String packageName) throws MalformedURLException {
        List<Class<?>> classes = new ArrayList<>();

        for (String jarPath : this.jarPaths) {
            URL url = new URL("file:" + jarPath);

            try (URLClassLoader loader = new URLClassLoader(new URL[]{url})) {
                try (JarFile jarFile = new JarFile(jarPath)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        // Load both classes and anonymous inner classes with $1 in the class name
                        if (name.endsWith(".class") && name.startsWith(packageName)) {
                            String className = name.substring(0, name.length() - 6)
                                    .replace('/', '.');
                            Class<?> potentialPluginClass = loader.loadClass(className);
                            log.debug("Loaded class: {}", className);
                            if(potentialPluginClass.getSuperclass() != null) {
                                if (potentialPluginClass.getSuperclass().getName().equals(PLUGIN_BASE_CLASS_NAME)) {
                                    log.debug("Main Plugin Class located: {}", className);
                                    classes.add(potentialPluginClass);
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    log.error("Failed to load jar classes. {}", e.getMessage());
                } catch (ClassNotFoundException e) {
                    log.error("No class found. Error = {}", e.getMessage());
                    e.printStackTrace();
                }
            } catch (IOException e) {
                log.error("Failed to create URLClassLoader. Error = {}", e.getMessage());
            }
        }
        return classes;
    }
}
