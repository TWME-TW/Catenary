package dev.twme.catenary.model;

import dev.twme.catenary.Catenary;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 預設管理器
 */
public class PresetManager {
    
    private final Catenary plugin;
    private final Map<String, Preset> presets = new HashMap<>();
    private File presetFile;
    
    public PresetManager(Catenary plugin) {
        this.plugin = plugin;
        this.presetFile = new File(plugin.getDataFolder(), "presets.yml");
    }
    
    /**
     * 載入所有預設
     */
    public void loadPresets() {
        presets.clear();
        
        // 建立預設文件（如果不存在）
        if (!presetFile.exists()) {
            plugin.saveResource("presets.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(presetFile);
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        
        if (presetsSection == null) {
            plugin.getLogger().warning("No presets found in presets.yml");
            createDefaultPresets();
            return;
        }
        
        // 載入所有預設
        for (String presetId : presetsSection.getKeys(false)) {
            ConfigurationSection section = presetsSection.getConfigurationSection(presetId);
            if (section != null) {
                try {
                    Preset preset = Preset.fromConfig(presetId, section);
                    presets.put(presetId, preset);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load preset '" + presetId + "': " + e.getMessage());
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + presets.size() + " presets");
        
        // 如果沒有載入任何預設，建立一些默認值
        if (presets.isEmpty()) {
            createDefaultPresets();
        }
    }
    
    /**
     * 建立預設值
     */
    private void createDefaultPresets() {
        plugin.getLogger().info("Creating default presets...");
        
        // 建立預設
        addDefaultPreset("chain", "鏈條", "標準懸掛鏈條", Material.CHAIN, false, 0.3, 10, 0.5, Material.CHAIN, false);
        addDefaultPreset("lantern", "燈籠", "懸掛式燈籠", Material.LANTERN, false, 0.2, 8, 2.0, Material.LANTERN, false);
        addDefaultPreset("powerline", "電線", "高壓電線", Material.BLACK_WOOL, true, 0.15, 15, 1.0, Material.LIGHTNING_ROD, false);
        
        // 保存預設
        savePresets();
    }
    
    /**
     * 添加默認預設
     */
    private void addDefaultPreset(String id, String name, String description, Material material, boolean isBlock, 
                                 double slack, int segments, double spacing, Material icon, boolean requirePermission) {
        RenderItem renderItem = new RenderItem(material, isBlock, 1.0f, 0, 0, 0);
        Preset preset = new Preset(id, name, description, renderItem, slack, segments, spacing, 
                                  new org.bukkit.inventory.ItemStack(icon), requirePermission);
        presets.put(id, preset);
    }
    
    /**
     * 保存所有預設
     */
    public void savePresets() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection presetsSection = config.createSection("presets");
        
        for (Map.Entry<String, Preset> entry : presets.entrySet()) {
            String id = entry.getKey();
            Preset preset = entry.getValue();
            
            ConfigurationSection section = presetsSection.createSection(id);
            section.set("name", preset.getName());
            section.set("description", preset.getDescription());
            
            // 渲染項目設定
            section.set("material", preset.getRenderItem().getItem().getType().name());
            section.set("isBlock", preset.getRenderItem().isBlock());
            section.set("scale", preset.getRenderItem().getScale());
            
            // 旋轉設定
            ConfigurationSection rotationSection = section.createSection("rotation");
            rotationSection.set("x", preset.getRenderItem().getRotationX());
            rotationSection.set("y", preset.getRenderItem().getRotationY());
            rotationSection.set("z", preset.getRenderItem().getRotationZ());
            
            // 其他參數
            section.set("slack", preset.getDefaultSlack());
            section.set("segments", preset.getDefaultSegments());
            section.set("spacing", preset.getDefaultSpacing());
            section.set("icon", preset.getDisplayIcon().getType().name());
            section.set("requirePermission", preset.isRequirePermission());
        }
        
        try {
            config.save(presetFile);
            plugin.getLogger().info("Saved " + presets.size() + " presets");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save presets: " + e.getMessage());
        }
    }
    
    /**
     * 取得預設
     */
    public Preset getPreset(String id) {
        return presets.get(id);
    }
    
    /**
     * 取得所有預設
     */
    public Collection<Preset> getAllPresets() {
        return presets.values();
    }
    
    /**
     * 添加新預設
     */
    public void addPreset(Preset preset) {
        presets.put(preset.getId(), preset);
        savePresets();
    }
    
    /**
     * 移除預設
     */
    public boolean removePreset(String id) {
        if (presets.remove(id) != null) {
            savePresets();
            return true;
        }
        return false;
    }
}
