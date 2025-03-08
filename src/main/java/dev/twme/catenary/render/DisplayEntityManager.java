package dev.twme.catenary.render;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.math.Vector3D;
import dev.twme.catenary.model.CatenaryStructure;
import dev.twme.catenary.model.RenderItem;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
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
        
        // 計算點的總數和間距
        double totalLength = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalLength += points.get(i).distance(points.get(i + 1));
        }
        
        // 生成顯示實體
        double currentDistance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3D startPoint = points.get(i);
            Vector3D endPoint = points.get(i + 1);
            double segmentLength = startPoint.distance(endPoint);
            
            while (currentDistance < totalLength) {
                // 計算當前點在當前線段上的位置
                double t = (currentDistance - (totalLength - segmentLength)) / segmentLength;
                if (t < 0 || t > 1) {
                    break;
                }
                
                // 計算實際位置
                Vector3D position = startPoint.add(endPoint.subtract(startPoint).multiply(t));
                
                // 創建顯示實體
                Display entity = createDisplayEntity(world, position, renderItem, structure.getId());
                if (entity != null) {
                    entities.add(entity);
                }
                
                currentDistance += spacing;
            }
        }
        
        // 存儲結構的實體列表
        structureEntities.put(structure.getId(), entities);
    }
    
    /**
     * 創建顯示實體
     */
    private Display createDisplayEntity(World world, Vector3D position, RenderItem renderItem, UUID structureId) {
        Location location = new Location(world, position.getX(), position.getY(), position.getZ());
        
        Display entity;
        if (renderItem.isBlock()) {
            // 創建方塊顯示實體
            BlockDisplay blockDisplay = world.spawn(location, BlockDisplay.class);
            blockDisplay.setBlock(renderItem.getItem().getType().createBlockData());
            entity = blockDisplay;
        } else {
            // 創建物品顯示實體
            ItemDisplay itemDisplay = world.spawn(location, ItemDisplay.class);
            itemDisplay.setItemStack(renderItem.getItem());
            entity = itemDisplay;
        }
        
        // 設定變換屬性
        Transformation transformation = renderItem.getTransformation();
        entity.setTransformation(transformation);
        
        // 設定基本顯示屬性
        entity.setBrightness(new Display.Brightness(15, 15));
        entity.setShadowRadius(0);
        entity.setShadowStrength(0);
        
        // 在持久化資料中存儲結構 ID
        entity.getPersistentDataContainer().set(
            plugin.getNamespacedKey(STRUCTURE_ID_KEY),
            PersistentDataType.STRING,
            structureId.toString()
        );
        
        return entity;
    }
    
    /**
     * 移除結構的顯示實體
     */
    public void removeStructureEntities(UUID structureId) {
        List<Display> entities = structureEntities.remove(structureId);
        if (entities != null) {
            entities.forEach(Display::remove);
        }
    }
    
    /**
     * 清理所有顯示實體
     */
    public void cleanupAllEntities() {
        for (List<Display> entities : structureEntities.values()) {
            entities.forEach(Display::remove);
        }
        structureEntities.clear();
    }
    
    /**
     * 尋找特定區域內的懸掛結構實體
     */
    public List<Display> findStructureEntitiesInArea(Location center, double radius) {
        List<Display> result = new ArrayList<>();
        World world = center.getWorld();
        
        if (world == null) {
            return result;
        }
        
        // 獲取區域內的所有顯示實體
        world.getEntitiesByClass(Display.class).stream()
            .filter(entity -> entity.getPersistentDataContainer().has(plugin.getNamespacedKey(STRUCTURE_ID_KEY), PersistentDataType.STRING))
            .filter(entity -> entity.getLocation().distance(center) <= radius)
            .forEach(result::add);
        
        return result;
    }
    
    /**
     * 更新可見結構的顯示狀態
     */
    public void updateVisibleStructures(List<Player> players) {
        double maxRenderDistance = plugin.getConfigManager().getMaxRenderDistance();
        
        // 為每個玩家檢查並更新附近的結構
        for (Player player : players) {
            // TODO: 實現距離優化的渲染邏輯
        }
    }
    
    /**
     * 檢查實體是否屬於指定結構
     */
    public boolean isEntityOfStructure(Display entity, UUID structureId) {
        String entityStructureId = entity.getPersistentDataContainer().get(
            plugin.getNamespacedKey(STRUCTURE_ID_KEY),
            PersistentDataType.STRING
        );
        
        return entityStructureId != null && entityStructureId.equals(structureId.toString());
    }
}
