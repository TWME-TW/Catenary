package dev.twme.catenary.render;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.math.Vector3D;
import dev.twme.catenary.model.CatenaryStructure;
import dev.twme.catenary.model.RenderItem;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import java.util.*;

/**
 * 顯示實體管理器
 */
public class DisplayEntityManager {

    private final Catenary plugin;
    private final Map<UUID, List<Display>> structureEntities = new HashMap<>();
    private final String STRUCTURE_ID_KEY = "catenary_structure_id";
    
    public DisplayEntityManager(Catenary plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 渲染懸掛結構
     */
    public void renderStructure(CatenaryStructure structure) {
        // 清除現有的顯示實體
        removeStructureEntities(structure.getId());
        
        // 如果結構不可見，則不渲染
        if (!structure.isVisible()) {
            return;
        }
        
        List<Vector3D> points = structure.getPoints();
        if (points == null || points.isEmpty()) {
            return;
        }
        
        List<Display> entities = new ArrayList<>();
        RenderItem renderItem = structure.getRenderItem();
        World world = structure.getWorld();
        double spacing = structure.getSpacing();
        
        // 計算每個點位之間的距離
        double totalDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            totalDistance += points.get(i).distance(points.get(i - 1));
        }
        
        // 計算需要渲染的實體數量
        int totalDisplays = Math.max(1, (int)(totalDistance / spacing));
        
        // 計算等距離的渲染點
        double remainingDistance = 0;
        Vector3D prevPoint = points.get(0);
        
        for (int i = 1; i < points.size(); i++) {
            Vector3D currentPoint = points.get(i);
            double segmentDistance = prevPoint.distance(currentPoint);
            
            // 計算方向向量
            Vector3D direction = currentPoint.subtract(prevPoint).normalize();
            
            // 渲染這個線段上的實體
            double distanceAlongSegment = remainingDistance;
            while (distanceAlongSegment < segmentDistance) {
                // 計算位置
                Vector3D position = prevPoint.add(direction.multiply(distanceAlongSegment));
                
                // 建立顯示實體
                Display displayEntity = createDisplayEntity(world, position, renderItem, structure.getId());
                if (displayEntity != null) {
                    entities.add(displayEntity);
                }
                
                distanceAlongSegment += spacing;
            }
            
            remainingDistance = distanceAlongSegment - segmentDistance;
            prevPoint = currentPoint;
        }
        
        // 儲存這個結構的所有實體
        structureEntities.put(structure.getId(), entities);
    }
    
    /**
     * 建立顯示實體
     */
    private Display createDisplayEntity(World world, Vector3D position, RenderItem renderItem, UUID structureId) {
        Display display;
        Location location = position.toLocation(world);
        
        if (renderItem.isBlock()) {
            // 建立方塊顯示
            BlockDisplay blockDisplay = (BlockDisplay) world.spawnEntity(location, EntityType.BLOCK_DISPLAY);
            blockDisplay.setBlock(renderItem.getItem().getType().createBlockData());
            display = blockDisplay;
        } else {
            // 建立物品顯示
            ItemDisplay itemDisplay = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
            itemDisplay.setItemStack(renderItem.getItem());
            display = itemDisplay;
        }
        
        // 設定轉換資訊
        display.setTransformation(renderItem.getTransformation());
        
        // 設定顯示設定
        display.setBrightness(new Display.Brightness(15, 15)); // 最大亮度
        display.setShadowRadius(0); // 沒有陰影
        display.setShadowStrength(0);
        display.setViewRange(64f); // 可見範圍
        
        // 儲存結構ID
        display.getPersistentDataContainer().set(
            plugin.getNamespacedKey(STRUCTURE_ID_KEY),
            PersistentDataType.STRING,
            structureId.toString()
        );
        
        return display;
    }
    
    /**
     * 移除結構的顯示實體
     */
    public void removeStructureEntities(UUID structureId) {
        List<Display> entities = structureEntities.remove(structureId);
        if (entities != null) {
            for (Display entity : entities) {
                entity.remove();
            }
        }
    }
    
    /**
     * 移除所有顯示實體
     */
    public void removeAllEntities() {
        for (UUID structureId : new ArrayList<>(structureEntities.keySet())) {
            removeStructureEntities(structureId);
        }
    }
    
    /**
     * 尋找特定點位附近的結構
     */
    public Set<UUID> findStructuresNearLocation(Location location, double radius) {
        Set<UUID> nearbyStructures = new HashSet<>();
        World world = location.getWorld();
        
        for (Map.Entry<UUID, List<Display>> entry : structureEntities.entrySet()) {
            for (Display entity : entry.getValue()) {
                if (entity.getWorld().equals(world) && entity.getLocation().distance(location) <= radius) {
                    nearbyStructures.add(entry.getKey());
                    break;
                }
            }
        }
        
        return nearbyStructures;
    }
}
