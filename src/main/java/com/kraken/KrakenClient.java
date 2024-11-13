package com.kraken;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class KrakenClient {

    public static boolean checkJavaVersion() {
        String javaHome = System.getProperty("java.home");
        String javaVersion = System.getProperty("java.version");

        // Extract the major version number from the java.version property
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(javaVersion);
        if (matcher.find()) {
            int majorVersion = Integer.parseInt(matcher.group(1));

            if (majorVersion > 11) {
                showUnsupportedVersionDialog(javaHome, javaVersion);
                return false;
            } else {
                log.info("Java version {} is supported.", javaVersion);
                return true;
            }
        } else {
            log.error("Unable to determine Java version from: {}", javaVersion);
            return false;
        }
    }

    private static void showUnsupportedVersionDialog(String javaHome, String javaVersion) {
        JOptionPane.showMessageDialog(
                null,
                "Only Java 11 is supported. You are using Java " + javaVersion + " located at " + javaHome + ". Download Java 11 JDK here: https://adoptium.net/temurin/releases/?version=11",
                "Unsupported Java Version",
                JOptionPane.ERROR_MESSAGE
        );
    }


    public static void main(String[] args) throws Exception {
        checkJavaVersion();

        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
        ExternalPluginManager.loadBuiltin(KrakenLoaderPlugin.class);
        RuneLite.main(args);
    }
}
