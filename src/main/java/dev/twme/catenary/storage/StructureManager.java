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
    }
    
    /**
     * 載入所有結構
     */
    public void loadStructures() {
        structures.clear();
        
        // 如果檔案不存在，建立空檔案
        if (!structuresFile.exists()) {
            try {
                structuresFile.getParentFile().mkdirs();
                structuresFile.createNewFile();
                
                // 建立初始化的 YAML 檔案
                FileConfiguration config = new YamlConfiguration();
                config.createSection("structures");
                config.save(structuresFile);
            } catch (IOException e) {
                plugin.getLogger().severe("無法建立結構檔案: " + e.getMessage());
                return;
            }
        }
        
        // 載入結構資料
        FileConfiguration config = YamlConfiguration.loadConfiguration(structuresFile);
        ConfigurationSection structuresSection = config.getConfigurationSection("structures");
        
        if (structuresSection == null) {
            return;
        }
        
        for (String key : structuresSection.getKeys(false)) {
            try {
                ConfigurationSection structureSection = structuresSection.getConfigurationSection(key);
                if (structureSection == null) continue;
                
                // 解析基本資料
                UUID id = UUID.fromString(key);
                UUID ownerId = UUID.fromString(structureSection.getString("ownerId"));
                String name = structureSection.getString("name");
                boolean visible = structureSection.getBoolean("visible", true);
                
                // 解析世界資料
                String worldName = structureSection.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("結構 " + key + " 的世界 '" + worldName + "' 不存在，跳過載入。");
                    continue;
                }
                
                // 解析起點和終點
                ConfigurationSection startSection = structureSection.getConfigurationSection("start");
                ConfigurationSection endSection = structureSection.getConfigurationSection("end");
                
                if (startSection == null || endSection == null) {
                    plugin.getLogger().warning("結構 " + key + " 缺少起點或終點資料，跳過載入。");
                    continue;
                }
                
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
                
                // 解析渲染物品
                ConfigurationSection renderItemSection = structureSection.getConfigurationSection("renderItem");
                if (renderItemSection == null) {
                    plugin.getLogger().warning("結構 " + key + " 缺少渲染物品資料，跳過載入。");
                    continue;
                }
                
                Material material = Material.getMaterial(renderItemSection.getString("material", "CHAIN"));
                if (material == null) material = Material.CHAIN;
                
                boolean isBlock = renderItemSection.getBoolean("isBlock", false);
                float scale = (float) renderItemSection.getDouble("scale", 1.0);
                float rotX = (float) renderItemSection.getDouble("rotX", 0);
                float rotY = (float) renderItemSection.getDouble("rotY", 0);
                float rotZ = (float) renderItemSection.getDouble("rotZ", 0);
                
                RenderItem renderItem = new RenderItem(material, isBlock, scale, rotX, rotY, rotZ);
                
                // 解析曲線參數
                double slack = structureSection.getDouble("slack", 0.3);
                int segments = structureSection.getInt("segments", 10);
                double spacing = structureSection.getDouble("spacing", 0.5);
                
                // 計算點位
                List<Vector3D> points = calculator.calculatePoints(start, end, slack, segments);
                
                // 建立結構物件
                CatenaryStructure structure = new CatenaryStructure(
                    id, ownerId, name, start, end, points,
                    renderItem, slack, segments, spacing, visible, world
                );
                
                // 添加到結構清單
                structures.put(id, structure);
                
            } catch (Exception e) {
                plugin.getLogger().warning("載入結構 " + key + " 時發生錯誤: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("已載入 " + structures.size() + " 個懸掛結構");
    }
    
    /**
     * 保存所有結構
     */
    public void saveStructures() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection structuresSection = config.createSection("structures");
        
        for (CatenaryStructure structure : structures.values()) {
            try {
                ConfigurationSection structureSection = structuresSection.createSection(structure.getId().toString());
                
                // 保存基本資料
                structureSection.set("ownerId", structure.getOwnerId().toString());
                structureSection.set("name", structure.getName());
                structureSection.set("visible", structure.isVisible());
                structureSection.set("world", structure.getWorld().getName());
                
                // 保存起點和終點
                ConfigurationSection startSection = structureSection.createSection("start");
                startSection.set("x", structure.getStart().getX());
                startSection.set("y", structure.getStart().getY());
                startSection.set("z", structure.getStart().getZ());
                
                ConfigurationSection endSection = structureSection.createSection("end");
                endSection.set("x", structure.getEnd().getX());
                endSection.set("y", structure.getEnd().getY());
                endSection.set("z", structure.getEnd().getZ());
                
                // 保存渲染物品
                ConfigurationSection renderItemSection = structureSection.createSection("renderItem");
                renderItemSection.set("material", structure.getRenderItem().getItem().getType().name());
                renderItemSection.set("isBlock", structure.getRenderItem().isBlock());
                renderItemSection.set("scale", structure.getRenderItem().getScale());
                renderItemSection.set("rotX", structure.getRenderItem().getRotationX());
                renderItemSection.set("rotY", structure.getRenderItem().getRotationY());
                renderItemSection.set("rotZ", structure.getRenderItem().getRotationZ());
                
                // 保存曲線參數
                structureSection.set("slack", structure.getSlack());
                structureSection.set("segments", structure.getSegments());
                structureSection.set("spacing", structure.getSpacing());
                
            } catch (Exception e) {
                plugin.getLogger().warning("保存結構 " + structure.getId() + " 時發生錯誤: " + e.getMessage());
            }
        }
        
        try {
            config.save(structuresFile);
            plugin.getLogger().info("已保存 " + structures.size() + " 個懸掛結構");
        } catch (IOException e) {
            plugin.getLogger().severe("無法保存結構資料: " + e.getMessage());
        }
    }
    
    /**
     * 添加新結構
     */
    public void addStructure(CatenaryStructure structure) {
        structures.put(structure.getId(), structure);
        plugin.getDisplayEntityManager().renderStructure(structure);
    }
    
    /**
     * 移除結構
     */
    public void removeStructure(UUID structureId) {
        CatenaryStructure structure = structures.remove(structureId);
        if (structure != null) {
            plugin.getDisplayEntityManager().removeStructureEntities(structureId);
        }
    }
    
    /**
     * 根據 ID 取得結構
     */
    public CatenaryStructure getStructure(UUID id) {
        return structures.get(id);
    }
    
    /**
     * 取得所有結構
     */
    public Collection<CatenaryStructure> getAllStructures() {
        return structures.values();
    }
    
    /**
     * 取得玩家擁有的結構
     */
    public List<CatenaryStructure> getPlayerStructures(UUID playerId) {
        return structures.values().stream()
            .filter(structure -> structure.getOwnerId().equals(playerId))
            .toList();
    }
    
    /**
     * 根據名稱模糊查找玩家的結構
     */
    public List<CatenaryStructure> findPlayerStructuresByName(UUID playerId, String partialName) {
        return getPlayerStructures(playerId).stream()
            .filter(structure -> structure.getName().toLowerCase().contains(partialName.toLowerCase()))
            .toList();
    }
    
    /**
     * 尋找特定區域內的結構
     */
    public List<CatenaryStructure> findStructuresInArea(Location center, double radius) {
        World world = center.getWorld();
        List<CatenaryStructure> results = new ArrayList<>();
        
        for (CatenaryStructure structure : structures.values()) {
            if (!structure.getWorld().equals(world)) continue;
            
            double distanceToStart = center.distance(structure.getStartLocation());
            double distanceToEnd = center.distance(structure.getEndLocation());
            
            if (distanceToStart <= radius || distanceToEnd <= radius) {
                results.add(structure);
            }
        }
        
        return results;
    }
    
    /**
     * 重新渲染所有結構
     */
    public void renderAllStructures() {
        for (CatenaryStructure structure : structures.values()) {
            plugin.getDisplayEntityManager().renderStructure(structure);
        }
    }
    
    /**
     * 清理無效的結構實體
     */
    public void cleanupInvalidEntities() {
        plugin.getDisplayEntityManager().cleanupAllEntities();
        renderAllStructures();
    }
}
