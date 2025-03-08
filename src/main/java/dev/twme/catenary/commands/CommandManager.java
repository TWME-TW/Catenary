package dev.twme.catenary.commands;

import dev.twme.catenary.Catenary;
import dev.twme.catenary.model.CatenaryStructure;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 指令管理器
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final Catenary plugin;
    
    public CommandManager(Catenary plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 註冊指令
     */
    public void registerCommands() {
        plugin.getCommand("catenary").setExecutor(this);
        plugin.getCommand("catenary").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此指令只能由玩家執行。");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 檢查基本權限
        if (!player.hasPermission("catenary.use")) {
            player.sendMessage("§c你沒有使用此插件的權限。");
            return true;
        }
        
        // 處理子指令
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "edit":
                handleEditCommand(player, args);
                break;
            case "remove":
                handleRemoveCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "presets":
                handlePresetsCommand(player);
                break;
            case "admin":
                handleAdminCommand(player, args);
                break;
            case "help":
                sendHelp(player);
                break;
            case "cancel":
                handleCancelCommand(player);
                break;
            case "confirm":
                handleConfirmCommand(player);
                break;
            case "adjust":
                handleAdjustCommand(player, args);
                break;
            default:
                player.sendMessage("§c未知的指令！請使用 /catenary help 獲取幫助。");
                break;
        }
        
        return true;
    }
    
    /**
     * 處理 create 指令
     */
    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("catenary.create")) {
            player.sendMessage("§c你沒有建立懸掛結構的權限。");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§c用法: /catenary create <預設名稱>");
            return;
        }
        
        String presetId = args[1].toLowerCase();
        
        // 檢查預設是否存在
        var preset = plugin.getPresetManager().getPreset(presetId);
        if (preset == null) {
            player.sendMessage("§c找不到名為 '" + presetId + "' 的預設。");
            return;
        }
        
        // 檢查是否需要特殊權限
        if (preset.isRequirePermission() && !player.hasPermission("catenary.preset." + presetId)) {
            player.sendMessage("§c你沒有使用此預設的權限。");
            return;
        }
        
        // 檢查是否已有活動會話
        if (plugin.getStudioManager().hasActiveSession(player)) {
            player.sendMessage("§c你已經在建立懸掛結構，請先完成或取消當前的編輯。");
            return;
        }
        
        // 建立新的編輯會話
        plugin.getStudioManager().startCreateSession(player, preset);
        
        player.sendMessage("§a已選擇預設: " + preset.getName());
        player.sendMessage("§a請使用工具右鍵點擊以設定第一個點。");
        player.sendMessage("§a輸入 /catenary cancel 隨時取消。");
    }
    
    /**
     * 處理 edit 指令
     */
    private void handleEditCommand(Player player, String[] args) {
        if (!player.hasPermission("catenary.edit")) {
            player.sendMessage("§c你沒有編輯懸掛結構的權限。");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§c用法: /catenary edit <結構ID或名稱>");
            return;
        }
        
        String identifier = args[1];
        
        // 這裡會需要 StructureManager 來查找結構
        player.sendMessage("§a正在尋找結構 '" + identifier + "'...");
        player.sendMessage("§c編輯功能尚未實現。");
    }
    
    /**
     * 處理 remove 指令
     */
    private void handleRemoveCommand(Player player, String[] args) {
        if (!player.hasPermission("catenary.remove")) {
            player.sendMessage("§c你沒有移除懸掛結構的權限。");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§c用法: /catenary remove <結構ID或名稱>");
            return;
        }
        
        String identifier = args[1];
        
        // 嘗試通過 ID 查找結構
        UUID structureId = null;
        try {
            structureId = UUID.fromString(identifier);
        } catch (IllegalArgumentException ignored) {
            // 如果不是有效的UUID，則通過名稱搜尋
        }
        
        CatenaryStructure structure = null;
        
        if (structureId != null) {
            structure = plugin.getStructureManager().getStructure(structureId);
        } else {
            // 通過名稱模糊搜尋
            List<CatenaryStructure> foundStructures = 
                plugin.getStructureManager().findPlayerStructuresByName(player.getUniqueId(), identifier);
            
            if (foundStructures.size() == 1) {
                structure = foundStructures.get(0);
            } else if (foundStructures.size() > 1) {
                player.sendMessage("§e找到多個匹配的結構，請使用更具體的名稱或ID:");
                for (CatenaryStructure s : foundStructures) {
                    player.sendMessage(String.format("§a- %s §7[ID: %s]", s.getName(), s.getId().toString().substring(0, 8)));
                }
                return;
            }
        }
        
        // 檢查是否找到結構
        if (structure == null) {
            player.sendMessage("§c找不到匹配的結構: " + identifier);
            return;
        }
        
        // 檢查擁有權
        if (!structure.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("catenary.admin")) {
            player.sendMessage("§c你不是此結構的擁有者，無法移除。");
            return;
        }
        
        // 移除結構
        plugin.getStructureManager().removeStructure(structure.getId());
        player.sendMessage("§a成功移除懸掛結構: " + structure.getName());
    }
    
    /**
     * 處理 list 指令
     */
    private void handleListCommand(Player player) {
        if (!player.hasPermission("catenary.list")) {
            player.sendMessage("§c你沒有列出懸掛結構的權限。");
            return;
        }
        
        // 列出玩家的結構
        List<CatenaryStructure> playerStructures = plugin.getStructureManager().getPlayerStructures(player.getUniqueId());
        
        if (playerStructures.isEmpty()) {
            player.sendMessage("§e你沒有任何懸掛結構。");
            return;
        }
        
        player.sendMessage("§a你的懸掛結構列表 (" + playerStructures.size() + "):");
        for (CatenaryStructure structure : playerStructures) {
            player.sendMessage(String.format("§a- %s §7[ID: %s] §f位於 (%.1f, %.1f, %.1f) 至 (%.1f, %.1f, %.1f)",
                structure.getName(),
                structure.getId().toString().substring(0, 8),
                structure.getStart().getX(),
                structure.getStart().getY(),
                structure.getStart().getZ(),
                structure.getEnd().getX(),
                structure.getEnd().getY(),
                structure.getEnd().getZ()
            ));
        }
    }
    
    /**
     * 處理 presets 指令
     */
    private void handlePresetsCommand(Player player) {
        if (!player.hasPermission("catenary.presets")) {
            player.sendMessage("§c你沒有查看預設的權限。");
            return;
        }
        
        player.sendMessage("§a可用的預設列表:");
        for (var preset : plugin.getPresetManager().getAllPresets()) {
            boolean hasAccess = !preset.isRequirePermission() || player.hasPermission("catenary.preset." + preset.getId());
            
            String status = hasAccess ? "§a" : "§c";
            player.sendMessage(status + "- " + preset.getName() + " §7(" + preset.getId() + ")§r: " + preset.getDescription());
        }
    }
    
    /**
     * 處理 admin 指令
     */
    private void handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("catenary.admin")) {
            player.sendMessage("§c你沒有管理員權限。");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§c用法: /catenary admin <reload|cleanup|export>");
            return;
        }
        
        String adminCommand = args[1].toLowerCase();
        
        switch (adminCommand) {
            case "reload":
                plugin.getConfigManager().reloadConfig();
                plugin.getPresetManager().loadPresets();
                player.sendMessage("§a插件配置已重新載入。");
                break;
                
            case "cleanup":
                player.sendMessage("§a清理無效實體中...");
                plugin.getStructureManager().cleanupInvalidEntities();
                player.sendMessage("§a已完成清理。");
                break;
                
            case "export":
                if (args.length < 4) {
                    player.sendMessage("§c用法: /catenary admin export <結構ID> <匯出名稱>");
                    return;
                }
                // TODO: 實作匯出功能
                player.sendMessage("§a匯出結構中...");
                player.sendMessage("§c匯出功能尚未實現。");
                break;
                
            default:
                player.sendMessage("§c未知的管理員指令！可用指令: reload, cleanup, export");
        }
    }
    
    /**
     * 處理 cancel 指令
     */
    private void handleCancelCommand(Player player) {
        if (!plugin.getStudioManager().hasActiveSession(player)) {
            player.sendMessage("§c你沒有進行中的懸掛結構編輯。");
            return;
        }
        
        plugin.getStudioManager().cancelSession(player);
        player.sendMessage("§a已取消當前的懸掛結構編輯。");
    }
    
    /**
     * 處理確認指令
     */
    private void handleConfirmCommand(Player player) {
        if (!plugin.getStudioManager().hasActiveSession(player)) {
            player.sendMessage("§c你沒有進行中的懸掛結構編輯。");
            return;
        }
        
        plugin.getStudioManager().confirmStructure(player);
    }

    /**
     * 處理調整指令
     */
    private void handleAdjustCommand(Player player, String[] args) {
        if (!plugin.getStudioManager().hasActiveSession(player)) {
            player.sendMessage("§c你沒有進行中的懸掛結構編輯。");
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage("§c用法: /catenary adjust <參數> <值>");
            player.sendMessage("§c可用參數: slack, segments, spacing");
            return;
        }
        
        String parameter = args[1].toLowerCase();
        String valueStr = args[2];
        
        try {
            switch (parameter) {
                case "slack":
                    double slack = Double.parseDouble(valueStr);
                    if (slack < 0 || slack > 1) {
                        player.sendMessage("§c鬆緊度必須在 0-1 之間。");
                        return;
                    }
                    plugin.getStudioManager().adjustSlack(player, slack);
                    break;
                    
                case "segments":
                    int segments = Integer.parseInt(valueStr);
                    if (segments < 2 || segments > 100) {
                        player.sendMessage("§c分段數必須在 2-100 之間。");
                        return;
                    }
                    plugin.getStudioManager().adjustSegments(player, segments);
                    break;
                    
                case "spacing":
                    double spacing = Double.parseDouble(valueStr);
                    if (spacing < 0.1 || spacing > 10) {
                        player.sendMessage("§c間距必須在 0.1-10 之間。");
                        return;
                    }
                    plugin.getStudioManager().adjustSpacing(player, spacing);
                    break;
                    
                default:
                    player.sendMessage("§c未知參數，可用參數: slack, segments, spacing");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c無效的數值: " + valueStr);
        }
    }

    /**
     * 發送幫助訊息
     */
    private void sendHelp(Player player) {
        player.sendMessage("§6===== Catenary 懸掛結構插件 =====");
        player.sendMessage("§f/catenary create <預設> §7- 開始建立新的懸掛結構");
        player.sendMessage("§f/catenary confirm §7- 確認建立懸掛結構");
        player.sendMessage("§f/catenary cancel §7- 取消建立過程");
        player.sendMessage("§f/catenary adjust <參數> <值> §7- 調整懸掛參數");
        player.sendMessage("§f/catenary edit <ID> §7- 編輯現有結構");
        player.sendMessage("§f/catenary remove <ID> §7- 移除結構");
        player.sendMessage("§f/catenary list §7- 列出你的懸掛結構");
        player.sendMessage("§f/catenary presets §7- 顯示可用的預設清單");
        player.sendMessage("§a註： 實體會自動順著曲線方向旋轉，並根據曲率自動調整密度分佈");
        
        if (player.hasPermission("catenary.admin")) {
            player.sendMessage("§f/catenary admin reload §7- 重新載入配置");
            player.sendMessage("§f/catenary admin cleanup §7- 清理無效實體");
            player.sendMessage("§f/catenary admin export <ID> <名稱> §7- 匯出結構為新預設");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一層子指令補全
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                "create", "edit", "remove", "list", "presets", "help", "cancel", "confirm", "adjust"
            ));
            
            if (sender.hasPermission("catenary.admin")) {
                subCommands.add("admin");
            }
            
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            // 第二層子指令補全
            switch (args[0].toLowerCase()) {
                case "create":
                    // 返回所有預設的 ID
                    return filterCompletions(
                        plugin.getPresetManager().getAllPresets().stream()
                            .map(preset -> preset.getId())
                            .collect(Collectors.toList()),
                        args[1]
                    );
                    
                case "adjust":
                    return filterCompletions(Arrays.asList("slack", "segments", "spacing"), args[1]);
                    
                case "admin":
                    if (sender.hasPermission("catenary.admin")) {
                        return filterCompletions(Arrays.asList("reload", "cleanup", "export"), args[1]);
                    }
                    break;
            }
        }
        
        return completions;
    }
    
    /**
     * 根據輸入過濾可用的補全選項
     */
    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
            .collect(Collectors.toList());
    }
}
