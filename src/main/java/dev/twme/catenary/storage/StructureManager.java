package dev.twme.catenary.storage;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.math.CatenaryCalculator;
import dev.twme.catenary.math.Vector3D;
import dev.twme.catenary.model.CatenaryStructure;
import dev.twme.catenary.model.Preset;
import dev.twme.catenary.model.RenderItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理懸掛結構的儲存和載入
 */
public class StructureManager {

    private final Catenary plugin;
    private final Map<UUID, CatenaryStructure> structures = new HashMap<>();
    private final CatenaryCalculator calculator;
    private File structuresFile;
    
    public StructureManager(Catenary plugin) {
        this.plugin = plugin;
        this.calculator = new CatenaryCalculator();
        this.structuresFile = new File(plugin.getDataFolder(), "structures.yml");
        
        // 載入已保存的結構
        loadStructures();
    }
    
    /**
     * 載入所有結構
     */
    public void loadStructures() {
        structures.clear();
        
        if (!structuresFile.exists()) {
            plugin.getLogger().info("No structures file found, creating a new one.");
            try {
                structuresFile.getParentFile().mkdirs();
                structuresFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create structures file: " + e.getMessage());
            }
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(structuresFile);
        ConfigurationSection structuresSection = config.getConfigurationSection("structures");
        
        if (structuresSection == null) {
            plugin.getLogger().info("No structures found to load.");
            return;
        }
        
        for (String key : structuresSection.getKeys(false)) {
            ConfigurationSection section = structuresSection.getConfigurationSection(key);
            if (section == null) continue;
            
            try {
                UUID id = UUID.fromString(key);
                UUID ownerId = UUID.fromString(section.getString("owner", ""));
                String name = section.getString("name", "未命名結構");
                
                // 載入世界
                String worldName = section.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Could not load structure " + id + ": world '" + worldName + "' not found");
                    continue;
                }
                
                // 載入向量
                ConfigurationSection startSection = section.getConfigurationSection("start");
                ConfigurationSection endSection = section.getConfigurationSection("end");
                
                Vector3D start = new Vector3D(
                    startSection.getDouble("x"),
                    startSection.getDouble("y"),
                    startSection.getDouble("z")
                );
                
                Vector3D end = new Vector3D(
                    endSection.getDouble("x"),
                    endSection.getDouble("y"),
                    endSection.getDouble("z")
                );
                
                // 載入參數
                double slack = section.getDouble("slack", 0.3);
                int segments = section.getInt("segments", 10);
                double spacing = section.getDouble("spacing", 0.5);
                
                // 載入渲染項目
                ConfigurationSection renderSection = section.getConfigurationSection("render");
                Material material = Material.getMaterial(renderSection.getString("material", "CHAIN"));
                if (material == null) material = Material.CHAIN;
                
                boolean isBlock = renderSection.getBoolean("isBlock", false);
                float scale = (float) renderSection.getDouble("scale", 1.0);
                float rotX = (float) renderSection.getDouble("rotationX", 0);
                float rotY = (float) renderSection.getDouble("rotationY", 0);
                float rotZ = (float) renderSection.getDouble("rotationZ", 0);
                
                RenderItem renderItem = new RenderItem(material, isBlock, scale, rotX, rotY, rotZ);
                
                // 建立結構
                CatenaryStructure structure = new CatenaryStructure(
                    id, ownerId, name, world, start, end, slack, segments, spacing, renderItem
                );
                
                // 計算點位
                List<Vector3D> points = calculator.calculatePoints(start, end, slack, segments);
                structure.setPoints(points);
                
                // 載入可見性
                structure.setVisible(section.getBoolean("visible", true));
                
                // 將結構加入管理
                structures.put(id, structure);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading structure " + key + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + structures.size() + " structures.");
    }
    
    /**
     * 保存所有結構
     */
    public void saveStructures() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection structuresSection = config.createSection("structures");
        
        for (Map.Entry<UUID, CatenaryStructure> entry : structures.entrySet()) {
            UUID id = entry.getKey();
            CatenaryStructure structure = entry.getValue();
            
            ConfigurationSection section = structuresSection.createSection(id.toString());
            
            // 保存基本資訊
            section.set("owner", structure.getOwnerId().toString());
            section.set("name", structure.getName());
            section.set("world", structure.getWorld().getName());
            
            // 保存向量
            ConfigurationSection startSection = section.createSection("start");
            startSection.set("x", structure.getStart().getX());
            startSection.set("y", structure.getStart().getY());
            startSection.set("z", structure.getStart().getZ());
            
            ConfigurationSection endSection = section.createSection("end");
            endSection.set("x", structure.getEnd().getX());
            endSection.set("y", structure.getEnd().getY());
            endSection.set("z", structure.getEnd().getZ());
            
            // 保存參數
            section.set("slack", structure.getSlack());
            section.set("segments", structure.getSegments());
            section.set("spacing", structure.getSpacing());
            
            // 保存渲染項目
            ConfigurationSection renderSection = section.createSection("render");
            renderSection.set("material", structure.getRenderItem().getItem().getType().name());
            renderSection.set("isBlock", structure.getRenderItem().isBlock());
            renderSection.set("scale", structure.getRenderItem().getScale());
            renderSection.set("rotationX", structure.getRenderItem().getRotationX());
            renderSection.set("rotationY", structure.getRenderItem().getRotationY());
            renderSection.set("rotationZ", structure.getRenderItem().getRotationZ());
            
            // 保存可見性
            section.set("visible", structure.isVisible());
        }
        
        try {
            config.save(structuresFile);
            plugin.getLogger().info("Saved " + structures.size() + " structures.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save structures: " + e.getMessage());
        }
    }
    
    /**
     * 添加新結構
     */
    public void addStructure(CatenaryStructure structure) {
        structures.put(structure.getId(), structure);
        saveStructures();
    }
    
    /**
     * 移除結構
     */
    public void removeStructure(UUID structureId) {
        if (structures.remove(structureId) != null) {
            // 移除顯示實體
            plugin.getDisplayEntityManager().removeStructureEntities(structureId);
            saveStructures();
        }
    }
    
    /**
     * 取得結構
     */
    public CatenaryStructure getStructure(UUID structureId) {
        return structures.get(structureId);
    }
    
    /**
     * 取得玩家的所有結構
     */
    public List<CatenaryStructure> getPlayerStructures(UUID playerId) {
        return structures.values().stream()
            .filter(structure -> structure.getOwnerId().equals(playerId))
            .collect(Collectors.toList());
    }
    
    /**
     * 通過名稱搜尋玩家結構
     */
    public List<CatenaryStructure> findPlayerStructuresByName(UUID playerId, String name) {
        return structures.values().stream()
            .filter(structure -> structure.getOwnerId().equals(playerId))
            .filter(structure -> structure.getName().toLowerCase().contains(name.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * 清理無效實體
     */
    public void cleanupInvalidEntities() {
        // 清理所有結構的顯示實體
        for (UUID structureId : structures.keySet()) {
            plugin.getDisplayEntityManager().removeStructureEntities(structureId);
        }
        
        // 重新渲染所有可見結構
        for (CatenaryStructure structure : structures.values()) {
            if (structure.isVisible()) {
                plugin.getDisplayEntityManager().renderStructure(structure);
            }
        }
    }
    
    /**
     * 取得所有結構
     */
    public Collection<CatenaryStructure> getAllStructures() {
        return structures.values();
    }
}
