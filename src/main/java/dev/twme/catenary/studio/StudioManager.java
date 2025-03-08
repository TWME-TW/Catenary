package dev.twme.catenary.studio;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.math.CatenaryCalculator;
import dev.twme.catenary.math.Vector3D;
import dev.twme.catenary.model.CatenaryStructure;
import dev.twme.catenary.model.Preset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * 工作室管理器
 */
public class StudioManager {
    
    private final Catenary plugin;
    private final Map<UUID, SessionData> activeSessions = new HashMap<>();
    private final CatenaryCalculator calculator;
    
    public StudioManager(Catenary plugin) {
        this.plugin = plugin;
        this.calculator = new CatenaryCalculator();
    }
    
    /**
     * 開始建立會話
     */
    public void startCreateSession(Player player, Preset preset) {
        SessionData session = new SessionData();
        session.player = player;
        session.preset = preset;
        session.state = SessionState.WAITING_FOR_FIRST_POINT;
        activeSessions.put(player.getUniqueId(), session);
        
        // 給玩家提供工具
        giveToolItem(player);
    }
    
    /**
     * 給予玩家編輯工具
     */
    private void giveToolItem(Player player) {
        ItemStack toolItem = new ItemStack(Material.STICK);
        // 可以在這裡設置工具的顯示名稱和說明
        player.getInventory().addItem(toolItem);
        player.sendMessage("§a已給予你編輯工具，使用右鍵點擊方塊設定點位。");
    }
    
    /**
     * 處理玩家選擇第一個點
     */
    public void handleFirstPointSelection(Player player, Location location) {
        SessionData session = activeSessions.get(player.getUniqueId());
        if (session == null || session.state != SessionState.WAITING_FOR_FIRST_POINT) {
            return;
        }
        
        session.firstPoint = new Vector3D(location);
        session.world = location.getWorld();
        session.state = SessionState.WAITING_FOR_SECOND_POINT;
        
        player.sendMessage("§a已設定第一個點，請選擇第二個點。");
        
        // 顯示粒子效果
        showParticleEffect(location);
    }
    
    /**
     * 處理玩家選擇第二個點
     */
    public void handleSecondPointSelection(Player player, Location location) {
        SessionData session = activeSessions.get(player.getUniqueId());
        if (session == null || session.state != SessionState.WAITING_FOR_SECOND_POINT) {
            return;
        }
        
        // 確保在同一世界
        if (!location.getWorld().equals(session.world)) {
            player.sendMessage("§c兩點必須在同一個世界中！");
            return;
        }
        
        session.secondPoint = new Vector3D(location);
        session.state = SessionState.READY_TO_CREATE;
        
        player.sendMessage("§a已設定第二個點。");
        showParticleEffect(location);
        
        // 啟動預覽
        startPreview(player);
    }
    
    /**
     * 開始預覽懸掛結構
     */
    private void startPreview(Player player) {
        SessionData session = activeSessions.get(player.getUniqueId());
        
        // 設定預設的參數值
        session.slack = session.preset.getDefaultSlack();
        session.segments = session.preset.getDefaultSegments();
        session.spacing = session.preset.getDefaultSpacing();
        
        // 計算預覽點位
        List<Vector3D> previewPoints = calculator.calculatePoints(
            session.firstPoint,
            session.secondPoint,
            session.slack,
            session.segments
        );
        
        session.previewPoints = previewPoints;
        
        // 顯示預覽粒子
        new BukkitRunnable() {
            int counter = 0;
            
            @Override
            public void run() {
                // 檢查會話是否仍然活動
                if (!activeSessions.containsKey(player.getUniqueId()) ||
                    activeSessions.get(player.getUniqueId()).state != SessionState.READY_TO_CREATE) {
                    cancel();
                    return;
                }
                
                // 計算預覽方向 - 為了更好的視覺效果，使用較長的粒子線條
                List<Vector3D> currentPoints = activeSessions.get(player.getUniqueId()).previewPoints;
                
                // 顯示每個點位的粒子和方向
                for (int i = 0; i < currentPoints.size() - 1; i++) {
                    Vector3D current = currentPoints.get(i);
                    Vector3D next = currentPoints.get(i + 1);
                    Vector3D direction = next.subtract(current).normalize().multiply(0.1); // 短向量表示方向
                    
                    // 顯示點位粒子
                    player.spawnParticle(
                        Particle.END_ROD,
                        current.getX(), current.getY(), current.getZ(),
                        1, 0, 0, 0, 0
                    );
                    
                    // 顯示方向粒子
                    player.spawnParticle(
                        Particle.DUST,
                        current.getX(), current.getY(), current.getZ(),
                        0, (float)direction.getX(), (float)direction.getY(), (float)direction.getZ(),
                        0.1f
                    );
                }
                
                // 顯示最後一個點
                if (!currentPoints.isEmpty()) {
                    Vector3D last = currentPoints.get(currentPoints.size() - 1);
                    player.spawnParticle(
                        Particle.END_ROD,
                        last.getX(), last.getY(), last.getZ(),
                        1, 0, 0, 0, 0
                    );
                }
                
                counter++;
                if (counter >= 100) { // 5秒後停止預覽（假設每秒運行20次）
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // 提示玩家確認或調整
        player.sendMessage("§a預覽已顯示。請輸入 §e/catenary confirm §a確認建立，或使用 §e/catenary adjust <參數> <值> §a調整參數。");
        player.sendMessage("§a可調整的參數: §eslack§a (鬆緊度, 0-1), §esegments§a (分段數, 2-100), §espacing§a (間距, 0.1-10)");
    }
    
    /**
     * 調整鬆緊度參數
     */
    public void adjustSlack(Player player, double slack) {
        SessionData session = activeSessions.get(player.getUniqueId());
        if (session == null || session.state != SessionState.READY_TO_CREATE) {
            player.sendMessage("§c沒有可調整的懸掛結構！");
            return;
        }
        
        // 更新預覽
        session.slack = slack;
        updatePreview(session);
        player.sendMessage(String.format("§a已調整鬆緊度為: %.2f", slack));
    }
    
    /**
     * 調整分段數量
     */
    public void adjustSegments(Player player, int segments) {
        SessionData session = activeSessions.get(player.getUniqueId());
        if (session == null || session.state != SessionState.READY_TO_CREATE) {
            player.sendMessage("§c沒有可調整的懸掛結構！");
            return;
        }
        
        // 更新預覽
        session.segments = segments;
        updatePreview(session);
        player.sendMessage(String.format("§a已調整分段數為: %d", segments));
    }
    
    /**
     * 調整間距參數
     */
    public void adjustSpacing(Player player, double spacing) {
        SessionData session = activeSessions.get(player.getUniqueId());
        if (session == null || session.state != SessionState.READY_TO_CREATE) {
            player.sendMessage("§c沒有可調整的懸掛結構！");
            return;
        }
        
        // 更新間距值
        session.spacing = spacing;
        player.sendMessage(String.format("§a已調整間距為: %.2f", spacing));
    }
    
    /**
     * 更新預覽
     */
    private void updatePreview(SessionData session) {
        // 重新計算點位
        session.previewPoints = calculator.calculatePoints(
            session.firstPoint,
            session.secondPoint,
            session.slack,
            session.segments
        );
        
        // 顯示新的預覽
        Player player = session.player;
        showPreview(player, session.previewPoints);
    }
    
    /**
     * 顯示預覽
     */
    private void showPreview(Player player, List<Vector3D> points) {
        // 清除現有的粒子效果是不可能的，但我們可以用新的粒子覆蓋
        
        // 顯示新的粒子
        for (Vector3D point : points) {
            player.spawnParticle(
                Particle.END_ROD,
                point.getX(), point.getY(), point.getZ(),
                1, 0, 0, 0, 0
            );
        }
    }
    
    /**
     * 確認建立懸掛結構
     */
    public void confirmStructure(Player player) {
        SessionData session = activeSessions.get(player.getUniqueId());
        if (session == null || session.state != SessionState.READY_TO_CREATE) {
            player.sendMessage("§c沒有可確認的懸掛結構！");
            return;
        }
        
        // 建立結構物件
        CatenaryStructure structure = new CatenaryStructure(
            UUID.randomUUID(),
            player.getUniqueId(),
            "懸掛結構_" + System.currentTimeMillis(),
            session.world,
            session.firstPoint,
            session.secondPoint,
            session.slack,
            session.segments,
            session.spacing,
            session.preset.getRenderItem()
        );
        
        // 計算並設定點位
        structure.setPoints(calculator.calculatePoints(
            session.firstPoint,
            session.secondPoint,
            structure.getSlack(),
            structure.getSegments()
        ));
        
        // 保存結構
        plugin.getStructureManager().addStructure(structure);
        
        // 渲染結構
        plugin.getDisplayEntityManager().renderStructure(structure);
        
        player.sendMessage("§a懸掛結構已成功建立！");
        
        // 結束會話
        activeSessions.remove(player.getUniqueId());
    }
    
    /**
     * 取消編輯會話
     */
    public void cancelSession(Player player) {
        if (activeSessions.remove(player.getUniqueId()) != null) {
            player.sendMessage("§a已取消編輯會話。");
        }
    }
    
    /**
     * 檢查玩家是否有活動會話
     */
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * 顯示粒子效果
     */
    private void showParticleEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.HAPPY_VILLAGER,
            location.getX(), location.getY() + 1, location.getZ(), 
            20, 0.5, 0.5, 0.5, 0.1
        );
    }
    
    /**
     * 取得玩家的會話資料
     */
    public SessionData getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    /**
     * 會話狀態枚舉
     */
    public enum SessionState {
        WAITING_FOR_FIRST_POINT,
        WAITING_FOR_SECOND_POINT,
        READY_TO_CREATE,
        ADJUSTING_PARAMETERS
    }
    
    /**
     * 會話資料類別
     */
    public class SessionData {
        public Player player;
        public Preset preset;
        public SessionState state;
        public Vector3D firstPoint;
        public Vector3D secondPoint;
        public World world;
        public List<Vector3D> previewPoints;
        public double slack;
        public int segments;
        public double spacing;
    }
}
