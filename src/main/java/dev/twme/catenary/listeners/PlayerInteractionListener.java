package dev.twme.catenary.listeners;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.studio.StudioManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 處理玩家互動事件
 */
public class PlayerInteractionListener implements Listener {
    
    private final Catenary plugin;
    
    public PlayerInteractionListener(Catenary plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 只處理主手的互動
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // 檢查是否手持特定工具
        if (!isCatenaryTool(player.getInventory().getItemInMainHand().getType())) {
            return;
        }
        
        // 檢查是否有使用權限
        if (!player.hasPermission("catenary.use")) {
            return;
        }
        
        // 檢查是否是右鍵點擊
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        // 取消事件，避免與方塊互動
        event.setCancelled(true);
        
        // 取得工作室管理器
        StudioManager studioManager = plugin.getStudioManager();
        
        // 檢查玩家是否有活動會話
        if (studioManager.hasActiveSession(player)) {
            // 已有會話，設定第二點
            StudioManager.SessionData session = studioManager.getPlayerSession(player);
            
            // 如果第一點已設定但第二點還沒，則設定第二點
            if (session.firstPoint != null && session.secondPoint == null) {
                // 根據點擊位置決定第二點
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    studioManager.setSecondPoint(player, event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
                } else {
                    // 如果點擊空氣，使用玩家視線所指向的位置
                    studioManager.setSecondPoint(player, player.getTargetBlockExact(5).getLocation().add(0.5, 0.5, 0.5));
                }
            }
        } else {
            // 沒有活動會話，開始新會話並設定第一點
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                studioManager.startNewSession(player, event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
            } else if (player.getTargetBlockExact(5) != null) {
                studioManager.startNewSession(player, player.getTargetBlockExact(5).getLocation().add(0.5, 0.5, 0.5));
            } else {
                player.sendMessage("§c請對著實體方塊點擊以設定點位。");
            }
        }
    }
    
    /**
     * 檢查物品是否是建立懸掛結構的工具
     */
    private boolean isCatenaryTool(Material material) {
        // 可以配置哪些物品可以用作工具
        return material == Material.STICK || 
               material == Material.BLAZE_ROD || 
               material == Material.CHAIN;
    }
}
