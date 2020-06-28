package de.eldoria.bigdoorsopener.util;

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateChecker {
    private UpdateChecker() {
    }

    public static void performAndNotifyUpdateCheck(Plugin plugin, int spigotId) {
        boolean updateAvailable = false;

        HttpURLConnection con;
        try {
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + spigotId);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
        } catch (IOException e) {
            return;
        }

        StringBuilder newestVersionRequest = new StringBuilder();
        try (InputStream stream = con.getInputStream(); BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                newestVersionRequest.append(inputLine);
            }
        } catch (IOException e) {
            return;
        }

        String newestVersion = newestVersionRequest.toString();

        String currentVersion = plugin.getDescription().getVersion();
        if (!currentVersion.equalsIgnoreCase(newestVersion)) {
            plugin.getLogger().warning("New version of " + plugin.getName() + " available.");
            plugin.getLogger().warning("Newest version: " + newestVersion + "! Current version: " + plugin.getDescription().getVersion() + "!");
            plugin.getLogger().warning("Download new version here: " + plugin.getDescription().getWebsite());
        }
    }
}