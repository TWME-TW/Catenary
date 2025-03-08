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
    private File presetsFile;
    
    public PresetManager(Catenary plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 載入所有預設
     */
    public void loadPresets() {
        presets.clear();
        
        // 載入內建預設
        loadBuiltinPresets();
        
        // 載入自訂預設
        loadCustomPresets();
        
        plugin.getLogger().info("已載入 " + presets.size() + " 個懸掛結構預設");
    }
    
    /**
     * 載入內建預設
     */
    private void loadBuiltinPresets() {
        // 創建預設配置檔案
        presetsFile = new File(plugin.getDataFolder(), "presets.yml");
        if (!presetsFile.exists()) {
            plugin.saveResource("presets.yml", false);
        }
        
        // 讀取預設配置
        FileConfiguration config = YamlConfiguration.loadConfiguration(presetsFile);
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        
        if (presetsSection != null) {
            for (String key : presetsSection.getKeys(false)) {
                ConfigurationSection presetSection = presetsSection.getConfigurationSection(key);
                if (presetSection == null) continue;
                
                try {
                    Preset preset = Preset.fromConfig(key, presetSection);
                    presets.put(key, preset);
                } catch (Exception e) {
                    plugin.getLogger().warning("載入預設 '" + key + "' 時發生錯誤: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 載入自訂預設
     */
    private void loadCustomPresets() {
        // 創建自訂預設目錄
        File customPresetsDir = new File(plugin.getDataFolder(), "custom_presets");
        if (!customPresetsDir.exists()) {
            customPresetsDir.mkdirs();
        }
        
        // 載入所有自訂預設檔案
        File[] files = customPresetsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String presetId = file.getName().replace(".yml", "");
                
                // 避免與內建預設衝突
                if (presets.containsKey(presetId)) {
                    presetId = "custom_" + presetId;
                }
                
                Preset preset = Preset.fromConfig(presetId, config);
                presets.put(presetId, preset);
            } catch (Exception e) {
                plugin.getLogger().warning("載入自訂預設 '" + file.getName() + "' 時發生錯誤: " + e.getMessage());
            }
        }
    }
    
    /**
     * 添加新的預設
     */
    public void addPreset(Preset preset) {
        presets.put(preset.getId(), preset);
    }
    
    /**
     * 保存預設為自訂預設
     */
    public void saveCustomPreset(Preset preset) throws IOException {
        File customPresetsDir = new File(plugin.getDataFolder(), "custom_presets");
        if (!customPresetsDir.exists()) {
            customPresetsDir.mkdirs();
        }
        
        File presetFile = new File(customPresetsDir, preset.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("name", preset.getName());
        config.set("description", preset.getDescription());
        config.set("material", preset.getRenderItem().getItem().getType().name());
        config.set("isBlock", preset.getRenderItem().isBlock());
        config.set("scale", preset.getRenderItem().getScale());
        config.set("rotation.x", preset.getRenderItem().getRotationX());
        config.set("rotation.y", preset.getRenderItem().getRotationY());
        config.set("rotation.z", preset.getRenderItem().getRotationZ());
        config.set("slack", preset.getDefaultSlack());
        config.set("segments", preset.getDefaultSegments());
        config.set("spacing", preset.getDefaultSpacing());
        config.set("icon", preset.getDisplayIcon().getType().name());
        config.set("requirePermission", preset.isRequirePermission());
        
        config.save(presetFile);
    }
    
    /**
     * 取得所有預設
     */
    public Collection<Preset> getAllPresets() {
        return presets.values();
    }
    
    /**
     * 取得特定預設
     */
    public Preset getPreset(String id) {
        return presets.get(id);
    }
    
    /**
     * 取得隨機預設
     */
    public Preset getRandomPreset() {
        if (presets.isEmpty()) return null;
        
        List<Preset> allPresets = new ArrayList<>(presets.values());
        int randomIndex = new Random().nextInt(allPresets.size());
        return allPresets.get(randomIndex);
    }
    
    /**
     * 移除預設
     */
    public void removePreset(String id) {
        presets.remove(id);
        
        // 若是自訂預設，也需刪除檔案
        File customPresetFile = new File(plugin.getDataFolder(), "custom_presets/" + id + ".yml");
        if (customPresetFile.exists()) {
            customPresetFile.delete();
        }
    }
}
