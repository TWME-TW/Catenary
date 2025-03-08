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
    
    // 配置項
    private boolean debugMode;
    private int maxStructuresPerPlayer;
    private double maxRenderDistance;
    private boolean enablePermissions;
    
    public ConfigManager(Catenary plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * 載入配置
     */
    public void loadConfig() {
        // 確保配置檔案存在
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        // 載入配置
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 讀取配置項
        debugMode = config.getBoolean("debug", false);
        maxStructuresPerPlayer = config.getInt("limits.maxStructuresPerPlayer", 50);
        maxRenderDistance = config.getDouble("limits.maxRenderDistance", 64.0);
        enablePermissions = config.getBoolean("enablePermissions", true);
        
        // 更新配置，添加缺少的項目
        updateConfig();
    }
    
    /**
     * 更新配置檔
     */
    private void updateConfig() {
        boolean updated = false;
        
        // 檢查並添加缺少的配置項
        if (!config.contains("debug")) {
            config.set("debug", debugMode);
            updated = true;
        }
        
        if (!config.contains("limits.maxStructuresPerPlayer")) {
            config.set("limits.maxStructuresPerPlayer", maxStructuresPerPlayer);
            updated = true;
        }
        
        if (!config.contains("limits.maxRenderDistance")) {
            config.set("limits.maxRenderDistance", maxRenderDistance);
            updated = true;
        }
        
        if (!config.contains("enablePermissions")) {
            config.set("enablePermissions", enablePermissions);
            updated = true;
        }
        
        // 若有更新，保存配置
        if (updated) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().warning("無法保存更新的配置: " + e.getMessage());
            }
        }
    }
    
    /**
     * 重新載入配置
     */
    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("配置已重新載入");
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public int getMaxStructuresPerPlayer() {
        return maxStructuresPerPlayer;
    }
    
    public double getMaxRenderDistance() {
        return maxRenderDistance;
    }
    
    public boolean isEnablePermissions() {
        return enablePermissions;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}
