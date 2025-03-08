package dev.twme.catenary.studio;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.math.CatenaryCalculator;
import dev.twme.catenary.math.Vector3D;
import dev.twme.catenary.model.CatenaryStructure;
import dev.twme.catenary.model.Preset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 工作室管理器
 */
public class StudioManager {
    
    private final Catenary plugin;
    private final CatenaryCalculator calculator;
    private final Map<UUID, SessionData> playerSessions = new HashMap<>();
    
    public StudioManager(Catenary plugin) {
        this.plugin = plugin;
        this.calculator = new CatenaryCalculator();
    }
    
    /**
     * 開始新的編輯會話
     */
    public void startNewSession(Player player, Location firstPoint) {
        UUID playerId = player.getUniqueId();
        SessionData session = new SessionData();
        session.firstPoint = firstPoint;
        playerSessions.put(playerId, session);
        
        // 顯示第一個點的標記
        showPointMarker(player, firstPoint, "第一點");
        
        player.sendMessage("§a第一點已設定，請設定第二點。");
    }
    
    /**
     * 設定會話的第二點
     */
    public void setSecondPoint(Player player, Location secondPoint) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.firstPoint == null) {
            player.sendMessage("§c請先設定第一點。");
            return;
        }
        
        if (!session.firstPoint.getWorld().equals(secondPoint.getWorld())) {
            player.sendMessage("§c兩個點必須在同一個世界。");
            return;
        }
        
        double distance = session.firstPoint.distance(secondPoint);
        if (distance < 1.0) {
            player.sendMessage("§c兩點距離太近，請選擇更遠的位置。");
            return;
        }
        
        session.secondPoint = secondPoint;
        
        // 顯示第二個點的標記
        showPointMarker(player, secondPoint, "第二點");
        
        // 準備預覽
        preparePreview(player, session);
        
        player.sendMessage("§a第二點已設定，請選擇預設樣式或調整參數。");
        
        // 顯示預設選擇界面 (這裡將在未來實現)
        // openPresetSelectionGui(player);
    }
    
    /**
     * 準備結構預覽
     */
    private void preparePreview(Player player, SessionData session) {
        // 使用預設參數建立預覽結構
        Preset defaultPreset = plugin.getPresetManager().getPreset("chain"); // 預設使用鏈條
        if (defaultPreset == null) {
            defaultPreset = plugin.getPresetManager().getRandomPreset();
        }
        
        if (defaultPreset == null) {
            player.sendMessage("§c找不到可用的預設樣式。");
            return;
        }
        
        // 建立臨時預覽結構
        CatenaryStructure previewStructure = new CatenaryStructure(
            player.getUniqueId(),
            session.firstPoint,
            session.secondPoint,
            defaultPreset.getRenderItem(),
            defaultPreset.getDefaultSlack(),
            defaultPreset.getDefaultSegments()
        );
        
        // 計算懸掛曲線點位
        List<Vector3D> points = calculator.calculatePoints(
            new Vector3D(session.firstPoint),
            new Vector3D(session.secondPoint),
            defaultPreset.getDefaultSlack(),
            defaultPreset.getDefaultSegments()
        );
        
        previewStructure.setPoints(points);
        session.previewStructure = previewStructure;
        
        // 渲染預覽結構
        plugin.getDisplayEntityManager().renderStructure(previewStructure);
        
        // 設定預覽的參數
        session.currentPreset = defaultPreset;
        session.slack = defaultPreset.getDefaultSlack();
        session.segments = defaultPreset.getDefaultSegments();
        session.spacing = defaultPreset.getDefaultSpacing();
    }
    
    /**
     * 更新預覽結構
     */
    public void updatePreview(Player player) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.previewStructure == null) {
            return;
        }
        
        // 更新結構屬性
        session.previewStructure.setSlack(session.slack);
        session.previewStructure.setSegments(session.segments);
        session.previewStructure.setSpacing(session.spacing);
        session.previewStructure.setRenderItem(session.currentPreset.getRenderItem());
        
        // 重新計算點位
        List<Vector3D> points = calculator.calculatePoints(
            new Vector3D(session.firstPoint),
            new Vector3D(session.secondPoint),
            session.slack,
            session.segments
        );
        
        session.previewStructure.setPoints(points);
        
        // 重新渲染結構
        plugin.getDisplayEntityManager().renderStructure(session.previewStructure);
    }
    
    /**
     * 最終確認並創建結構
     */
    public void finalizeStructure(Player player, String name) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.previewStructure == null) {
            player.sendMessage("§c沒有有效的預覽結構。");
            return;
        }
        
        CatenaryStructure structure = session.previewStructure;
        
        // 設定結構名稱
        if (name != null && !name.isEmpty()) {
            structure.setName(name);
        }
        
        // 保存結構
        plugin.getStructureManager().addStructure(structure);
        
        player.sendMessage("§a懸掛結構「" + structure.getName() + "」已建立！");
        
        // 清除會話
        playerSessions.remove(playerId);
    }
    
    /**
     * 取消編輯
     */
    public void cancelEditing(Player player) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session != null && session.previewStructure != null) {
            plugin.getDisplayEntityManager().removeStructureEntities(session.previewStructure.getId());
        }
        
        playerSessions.remove(playerId);
        player.sendMessage("§a已取消編輯。");
    }
    
    /**
     * 顯示點位標記
     */
    private void showPointMarker(Player player, Location location, String label) {
        // 使用粒子效果標記點位
        player.spawnParticle(Particle.END_ROD, location, 30, 0.1, 0.1, 0.1, 0.05);
        player.spawnParticle(Particle.GLOW, location, 5, 0.2, 0.2, 0.2, 0);
        
        // 顯示懸浮文字提示
        player.sendTitle("", "§a已設定" + label, 5, 30, 10);
    }
    
    /**
     * 設定預設樣式
     */
    public void setPreset(Player player, String presetId) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.previewStructure == null) {
            player.sendMessage("§c沒有活動中的預覽結構。");
            return;
        }
        
        Preset preset = plugin.getPresetManager().getPreset(presetId);
        if (preset == null) {
            player.sendMessage("§c找不到預設樣式: " + presetId);
            return;
        }
        
        // 檢查權限
        if (preset.isRequirePermission() && !player.hasPermission("catenary.preset." + preset.getId())) {
            player.sendMessage("§c你沒有使用此預設樣式的權限。");
            return;
        }
        
        // 更新會話中的預設和參數
        session.currentPreset = preset;
        session.slack = preset.getDefaultSlack();
        session.segments = preset.getDefaultSegments();
        session.spacing = preset.getDefaultSpacing();
        
        // 更新預覽結構
        session.previewStructure.setRenderItem(preset.getRenderItem());
        updatePreview(player);
        
        player.sendMessage("§a已套用預設樣式：" + preset.getName());
    }
    
    /**
     * 調整鬆緊度
     */
    public void adjustSlack(Player player, double amount) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.previewStructure == null) {
            return;
        }
        
        // 調整鬆緊度，確保在合理範圍內
        session.slack = Math.max(0.01, Math.min(1.0, session.slack + amount));
        updatePreview(player);
        
        player.sendMessage("§a鬆緊度：" + String.format("%.2f", session.slack));
    }
    
    /**
     * 調整分段數量
     */
    public void adjustSegments(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.previewStructure == null) {
            return;
        }
        
        // 調整分段數，確保在合理範圍內
        session.segments = Math.max(2, Math.min(50, session.segments + amount));
        updatePreview(player);
        
        player.sendMessage("§a分段數量：" + session.segments);
    }
    
    /**
     * 調整間距
     */
    public void adjustSpacing(Player player, double amount) {
        UUID playerId = player.getUniqueId();
        SessionData session = playerSessions.get(playerId);
        
        if (session == null || session.previewStructure == null) {
            return;
        }
        
        // 調整間距，確保在合理範圍內
        session.spacing = Math.max(0.1, Math.min(2.0, session.spacing + amount));
        updatePreview(player);
        
        player.sendMessage("§a間距：" + String.format("%.2f", session.spacing));
    }
    
    /**
     * 檢查玩家是否有活動會話
     */
    public boolean hasActiveSession(Player player) {
        return playerSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * 取得玩家的會話資料
     */
    public SessionData getPlayerSession(Player player) {
        return playerSessions.get(player.getUniqueId());
    }
    
    /**
     * 會話資料類別
     */
    public static class SessionData {
        public Location firstPoint;
        public Location secondPoint;
        public CatenaryStructure previewStructure;
        public Preset currentPreset;
        public double slack = 0.3;
        public int segments = 10;
        public double spacing = 0.5;
    }
}
