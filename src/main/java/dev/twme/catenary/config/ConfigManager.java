package dev.twme.catenary.config;

import dev.twme.catenary.Catenary;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * 配置管理器
 */
public class ConfigManager {

    private final Catenary plugin;
    private FileConfiguration config;
    private File configFile;
    
    public ConfigManager(Catenary plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * 載入配置
     */
    public void loadConfig() {
        // 確保插件資料夾存在
        plugin.getDataFolder().mkdirs();
        
        // 如果配置檔案不存在，則建立預設配置
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 設定預設值
        config.addDefault("general.debugMode", false);
        config.addDefault("general.maxStructuresPerPlayer", 50);
        config.addDefault("rendering.viewDistance", 64);
        config.addDefault("rendering.updateInterval", 20);
        
        // 保存預設值
        config.options().copyDefaults(true);
        saveConfig();
    }
    
    /**
     * 重新載入配置
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Configuration reloaded.");
    }
    
    /**
     * 保存配置
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config to " + configFile + ": " + e.getMessage());
        }
    }
    
    /**
     * 取得配置
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 取得除錯模式設定
     */
    public boolean isDebugMode() {
        return config.getBoolean("general.debugMode", false);
    }
    
    /**
     * 取得每玩家最大結構數量
     */
    public int getMaxStructuresPerPlayer() {
        return config.getInt("general.maxStructuresPerPlayer", 50);
    }
    
    /**
     * 取得渲染視距
     */
    public int getViewDistance() {
        return config.getInt("rendering.viewDistance", 64);
    }
    
    /**
     * 取得更新間隔
     */
    public int getUpdateInterval() {
        return config.getInt("rendering.updateInterval", 20);
    }
}
