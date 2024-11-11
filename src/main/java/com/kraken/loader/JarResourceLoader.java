package com.kraken.loader;

import com.kraken.api.model.PreSignedURL;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;


@Slf4j
public class JarResourceLoader {

    /**
     * Reads an InputStream into a byte array using a 16KB buffer for efficiency.
     * Used to load individual class files from a JAR stream into memory.
     * @param is InputStream
     * @return byte[] An array of bytes.
     */
    private static byte[] readEntryBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    /**
     * Loads the class which extends net.runelite.client.plugins.Plugin for each discovered JAR file.
     * @param packageName String the name of the package to filter for within the JAR file. JAR files often contain many classes
     *                    that are not the plugin classes i.e. dependencies, metadata, etc...
     * @param preSignedURL PreSignedURL A Pre signed S3 url enabling the JAR file to be downloaded.
     * @return List of Plugin classes.
     */
    public ByteArrayClassLoader loadJarFromSignedUrl(final String packageName, final PreSignedURL preSignedURL) {
        Map<String, byte[]> classData = new HashMap<>();
        URLConnection connection;

        try {
            URL url = new URL(preSignedURL.getUrl());
            connection = url.openConnection();
        } catch (IOException e) {
            log.error("IOException thrown while attempting to open connection to pre signed url. Error = {}", e.getMessage());
            return null;
        }

        try (JarInputStream jarStream = new JarInputStream(connection.getInputStream())) {
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                String name = entry.getName();

                // Load both classes and anonymous inner classes with $1 in the class name
                if (name.endsWith(".class") && name.startsWith(packageName)) {
                    byte[] classBytes = readEntryBytes(jarStream);
                    String className = name.substring(0, name.length() - 6)
                            .replace('/', '.');

                    log.debug("Adding potential plugin class: {}", className);
                    classData.put(className, classBytes);
                }
            }
        }  catch (IOException e) {
            log.error("Failed to read jar classes into a byte array {}", e.getMessage());
        }
        return new ByteArrayClassLoader(classData);
    }
}
