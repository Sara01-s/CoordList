package com.lavy01.coordlist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomConfigFile<T extends JavaPlugin> {
    
	private final T plugin;
    private final String configFileName;

    private FileConfiguration customConfigFile;
    private File nativeFile;


	/**
	 * @param plugin Spigot plugin being used.
	 * @param dataFileName The name of the custom config file, where data will be stored. (Without extension, .yml will be used).
	 */
	public CustomConfigFile(final T plugin, final String dataFileName) {
		this.plugin = plugin;
        this.configFileName = dataFileName + ".yml";

        initCustomConfigFile();
	}

    public FileConfiguration getConfig() {
        if (this.customConfigFile == null) {
            reloadData();
        }

        return this.customConfigFile;
    }
    	
	public void reloadData() {
        this.nativeFile = new File(this.plugin.getDataFolder(), configFileName);
		this.customConfigFile = YamlConfiguration.loadConfiguration(this.nativeFile);

		final InputStream reader = this.plugin.getResource(configFileName);

		if (reader != null) {
			var readData = YamlConfiguration.loadConfiguration(new InputStreamReader(reader));

			this.customConfigFile.setDefaults(readData);
            this.plugin.getLogger().log(Level.INFO, configFileName + " data reloaded");
		}
	}

    public void saveData() {
        if (this.customConfigFile == null || this.nativeFile == null) {
            return;
        }

        try {
            this.getConfig().save(this.nativeFile);
        } 
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save data to: " + configFileName, e);
        }
    }

    private void initCustomConfigFile() {
        if (this.nativeFile == null) {
            this.nativeFile = new File(this.plugin.getDataFolder(), this.configFileName);
        }

        if (!this.nativeFile.exists()) {
            this.plugin.saveResource(configFileName, false);
        }
    }

}
