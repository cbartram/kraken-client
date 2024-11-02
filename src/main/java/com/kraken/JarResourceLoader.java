package com.kraken;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;


@Slf4j
public class JarResourceLoader {

    private final List<String> jarPaths = new ArrayList<>();

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
        } catch (IOException e) {
            log.error("Error reading jar file paths: {}", e.getMessage());
        }
    }

    public void listClasses(String jarPath, Predicate<String> filter) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Only include .class files, exclude inner classes
                if (name.endsWith(".class") && !name.contains("$")) {
                    // Convert path format to package format
                    if(filter.test(name)) {
                        log.info("Jar class: {}", name);
                    }
                }
            }
        } catch(IOException e) {
            log.error("IOException thrown while attempting to list jar classes for JAR: {}. Error = {}", jarPath, e.getMessage());
        }
    }

    /**
     * Loads Plugin classes for each discovered JAR file.
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

                        // Only include .class files, exclude inner classes
                        if (name.endsWith(".class") && !name.contains("$") && name.startsWith(packageName)) {
                            String className = name.substring(0, name.length() - 6)
                                    .replace('/', '.');

                            Class<?> potentialPluginClass = loader.loadClass(className);
                            log.info("Loaded class: {}, class extends plugin = {}", className, potentialPluginClass.isAssignableFrom(Plugin.class));
                            if (potentialPluginClass.isAssignableFrom(Plugin.class)) {
                                log.info("{} extends the Plugin class from RuneLite. Adding to list.", className);
                                classes.add(potentialPluginClass);
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
