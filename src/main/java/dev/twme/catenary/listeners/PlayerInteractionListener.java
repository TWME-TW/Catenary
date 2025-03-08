package dev.twme.catenary.listeners;

import dev.twme.catenary.Catenary;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * 玩家互動監聽器
 */
public class PlayerInteractionListener implements Listener {

    private final Catenary plugin;
    
    public PlayerInteractionListener(Catenary plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 確保是右手操作
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        // 僅處理右鍵點擊
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        // 檢查玩家是否持有編輯工具 (木棍)
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.STICK) return;
        
        // 檢查玩家是否有活動會話
        if (!plugin.getStudioManager().hasActiveSession(event.getPlayer())) return;
        
        // 防止方塊放置
        event.setCancelled(true);
        
        // 處理點擊事件
        var studioManager = plugin.getStudioManager();
        var session = plugin.getStudioManager().getSession(event.getPlayer().getUniqueId());
        
        if (session == null) return;
        
        // 計算玩家點擊的精確位置（方塊的面上）
        Block clickedBlock = event.getClickedBlock();
        BlockFace clickedFace = event.getBlockFace();
        Location clickedLocation = clickedBlock.getLocation().clone();
        
        // 調整位置到方塊的面中心
        clickedLocation.add(0.5, 0.5, 0.5); // 先移到方塊中心
        
        // 根據點擊的面調整位置
        switch (clickedFace) {
            case UP:
                clickedLocation.add(0, 0.5, 0);
                break;
            case DOWN:
                clickedLocation.add(0, -0.5, 0);
                break;
            case NORTH:
                clickedLocation.add(0, 0, -0.5);
                break;
            case SOUTH:
                clickedLocation.add(0, 0, 0.5);
                break;
            case EAST:
                clickedLocation.add(0.5, 0, 0);
                break;
            case WEST:
                clickedLocation.add(-0.5, 0, 0);
                break;
        }
        
        switch (session.state) {
            case WAITING_FOR_FIRST_POINT:
                studioManager.handleFirstPointSelection(event.getPlayer(), clickedLocation);
                break;
                
            case WAITING_FOR_SECOND_POINT:
                studioManager.handleSecondPointSelection(event.getPlayer(), clickedLocation);
                break;
                
            default:
                // 其他狀態不處理點擊事件
                break;
        }
    }
}
