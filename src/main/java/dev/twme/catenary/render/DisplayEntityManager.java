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
import org.joml.Quaternionf;

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
        if (points == null || points.isEmpty() || points.size() < 2) {
            return;
        }
        
        List<Display> entities = new ArrayList<>();
        RenderItem renderItem = structure.getRenderItem();
        World world = structure.getWorld();
        
        // 自動計算密度和間距
        AutoDensityResult densityResult = calculateAutoDensity(points, structure);
        List<RenderPoint> renderPoints = densityResult.renderPoints;
        
        // 為每個渲染點建立顯示實體
        for (RenderPoint renderPoint : renderPoints) {
            Display displayEntity = createDisplayEntity(
                world, 
                renderPoint.position, 
                renderItem,
                structure.getId(),
                renderPoint.rotation
            );
            
            if (displayEntity != null) {
                entities.add(displayEntity);
            }
        }
        
        // 儲存這個結構的所有實體
        structureEntities.put(structure.getId(), entities);
    }
    
    /**
     * 計算自動密度和旋轉
     */
    private AutoDensityResult calculateAutoDensity(List<Vector3D> points, CatenaryStructure structure) {
        List<RenderPoint> renderPoints = new ArrayList<>();
        double baseSpacing = structure.getSpacing();
        
        // 計算曲線總長度和最小/最大曲率
        double totalLength = 0;
        double maxCurvature = 0;
        double minCurvature = Double.MAX_VALUE;
        
        List<Double> segmentLengths = new ArrayList<>();
        List<Vector3D> directions = new ArrayList<>();
        
        // 計算每個線段的長度和方向
        for (int i = 1; i < points.size(); i++) {
            Vector3D prev = points.get(i - 1);
            Vector3D curr = points.get(i);
            
            double length = prev.distance(curr);
            totalLength += length;
            segmentLengths.add(length);
            
            Vector3D direction = curr.subtract(prev).normalize();
            directions.add(direction);
        }
        
        // 計算每個內部點的曲率（通過相鄰方向向量的變化估算）
        List<Double> curvatures = new ArrayList<>();
        for (int i = 0; i < directions.size() - 1; i++) {
            Vector3D dir1 = directions.get(i);
            Vector3D dir2 = directions.get(i + 1);
            
            // 使用相鄰方向向量的夾角作為曲率估算
            double dotProduct = dir1.dot(dir2);
            dotProduct = Math.min(1.0, Math.max(-1.0, dotProduct)); // 確保在 [-1, 1] 範圍內
            double angle = Math.acos(dotProduct);
            double curvature = angle / segmentLengths.get(i + 1);
            
            curvatures.add(curvature);
            maxCurvature = Math.max(maxCurvature, curvature);
            minCurvature = Math.min(minCurvature, curvature);
        }
        
        // 為首尾點添加曲率
        if (curvatures.size() > 0) {
            curvatures.add(0, curvatures.get(0));
            curvatures.add(curvatures.get(curvatures.size() - 1));
        } else {
            // 直線情況
            curvatures.add(0.0);
            curvatures.add(0.0);
        }
        
        // 曲率範圍太小，使用均勻間距
        double curvatureRange = maxCurvature - minCurvature;
        boolean useUniformSpacing = curvatureRange < 0.01 || Double.isNaN(curvatureRange);
        
        // 根據總長度和配置的基本間距估算實體數量
        int estimatedEntityCount = Math.max(10, (int)(totalLength / baseSpacing));
        
        // 生成渲染點
        double remainingDistance = 0;
        Vector3D prevPoint = points.get(0);
        Quaternionf prevRotation = calculateRotation(directions.get(0));
        
        // 為第一個點添加實體
        renderPoints.add(new RenderPoint(prevPoint, prevRotation));
        
        for (int i = 1; i < points.size(); i++) {
            Vector3D currentPoint = points.get(i);
            Vector3D direction = directions.get(i - 1);
            double segmentLength = segmentLengths.get(i - 1);
            double curvature = curvatures.get(i);
            
            // 根據曲率調整間距
            double adaptiveSpacing;
            if (useUniformSpacing) {
                adaptiveSpacing = baseSpacing;
            } else {
                // 曲率大的地方，間距小（更多實體）
                double factor = 1.0 - 0.7 * (curvature - minCurvature) / (maxCurvature - minCurvature + 0.0001);
                adaptiveSpacing = baseSpacing * Math.max(0.3, factor);
            }
            
            // 計算此線段的實體放置
            double distanceAlongSegment = remainingDistance;
            while (distanceAlongSegment < segmentLength) {
                // 計算位置
                Vector3D position = prevPoint.add(direction.multiply(distanceAlongSegment));
                
                // 計算旋轉
                Quaternionf rotation;
                if (i < points.size() - 1) {
                    // 內部點使用插值旋轉
                    double t = distanceAlongSegment / segmentLength;
                    Vector3D nextDirection = directions.get(Math.min(i, directions.size() - 1));
                    Vector3D interpolatedDir = interpolateDirection(direction, nextDirection, t);
                    rotation = calculateRotation(interpolatedDir);
                } else {
                    // 最後一個點使用最後的方向
                    rotation = calculateRotation(direction);
                }
                
                // 添加渲染點
                renderPoints.add(new RenderPoint(position, rotation));
                
                // 移動到下一個間距
                distanceAlongSegment += adaptiveSpacing;
            }
            
            remainingDistance = distanceAlongSegment - segmentLength;
            prevPoint = currentPoint;
        }
        
        // 確保最後一個點有實體
        if (renderPoints.get(renderPoints.size() - 1).position.distance(points.get(points.size() - 1)) > 0.01) {
            Vector3D lastDirection = directions.get(directions.size() - 1);
            Quaternionf lastRotation = calculateRotation(lastDirection);
            renderPoints.add(new RenderPoint(points.get(points.size() - 1), lastRotation));
        }
        
        return new AutoDensityResult(renderPoints, estimatedEntityCount);
    }
    
    /**
     * 插值兩個方向向量
     */
    private Vector3D interpolateDirection(Vector3D dir1, Vector3D dir2, double t) {
        Vector3D result = dir1.multiply(1 - t).add(dir2.multiply(t));
        return result.normalize();
    }
    
    /**
     * 根據方向向量計算四元數旋轉
     */
    private Quaternionf calculateRotation(Vector3D direction) {
        // 如果方向是垂直向上或向下的，特殊處理
        if (Math.abs(direction.getY()) > 0.99) {
            return new Quaternionf().rotationXYZ(
                (float)(direction.getY() > 0 ? Math.PI / 2 : -Math.PI / 2),
                0f, 
                0f
            );
        }
        
        // 計算Y軸旋轉角度（偏航）
        double yaw = Math.atan2(direction.getX(), direction.getZ());
        
        // 計算X軸旋轉角度（俯仰）
        double pitch = Math.asin(direction.getY());
        
        // 創建四元數
        return new Quaternionf().rotationXYZ(
            (float)pitch,
            (float)yaw, 
            0f
        );
    }
    
    /**
     * 建立顯示實體
     */
    private Display createDisplayEntity(World world, Vector3D position, RenderItem renderItem, UUID structureId, Quaternionf rotation) {
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
        
        // 基本變換
        Transformation baseTransform = renderItem.getTransformation();
        
        // 結合自定義旋轉和基本變換
        Transformation combinedTransform = new Transformation(
            baseTransform.getTranslation(),
            rotation,                               // 使用曲線方向的旋轉
            baseTransform.getScale(),
            baseTransform.getLeftRotation()
        );
        
        // 設定轉換資訊
        display.setTransformation(combinedTransform);
        
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
    
    /**
     * 渲染點資料類別
     */
    private static class RenderPoint {
        public final Vector3D position;
        public final Quaternionf rotation;
        
        public RenderPoint(Vector3D position, Quaternionf rotation) {
            this.position = position;
            this.rotation = rotation;
        }
    }
    
    /**
     * 自動密度計算結果
     */
    private static class AutoDensityResult {
        public final List<RenderPoint> renderPoints;
        public final int estimatedEntityCount;
        
        public AutoDensityResult(List<RenderPoint> renderPoints, int estimatedEntityCount) {
            this.renderPoints = renderPoints;
            this.estimatedEntityCount = estimatedEntityCount;
        }
    }
}
