package dev.twme.catenary;

import dev.twme.catenary.commands.CommandManager;
import dev.twme.catenary.config.ConfigManager;
import dev.twme.catenary.listeners.PlayerInteractionListener;
import dev.twme.catenary.render.DisplayEntityManager;
import dev.twme.catenary.storage.StructureManager;
import dev.twme.catenary.studio.StudioManager;
import dev.twme.catenary.model.PresetManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Catenary extends JavaPlugin {
    
    private static Catenary instance;
    private ConfigManager configManager;
    private PresetManager presetManager;
    private DisplayEntityManager displayEntityManager;
    private StudioManager studioManager;
    private CommandManager commandManager;
    private StructureManager structureManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化預設管理器
        presetManager = new PresetManager(this);
        presetManager.loadPresets();
        
        // 初始化顯示實體管理器
        displayEntityManager = new DisplayEntityManager(this);
        
        // 初始化工作室管理器
        studioManager = new StudioManager(this);
        
        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new PlayerInteractionListener(this), this);
        
        // 設定指令
        commandManager = new CommandManager(this);
        commandManager.registerCommands();

        //  結構管理器
        structureManager = new StructureManager(this);
        
        getLogger().info("Catenary 插件已啟用");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // 清理所有顯示實體
        if (displayEntityManager != null) {
            displayEntityManager.cleanupAllEntities();
        }
        
        getLogger().info("Catenary 插件已停用");
    }
    
    /**
     * 取得具名空間鍵
     */
    public NamespacedKey getNamespacedKey(String key) {
        return new NamespacedKey(this, key);
    }
    
    public static Catenary getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public PresetManager getPresetManager() {
        return presetManager;
    }
    
    public DisplayEntityManager getDisplayEntityManager() {
        return displayEntityManager;
    }
    
    public StudioManager getStudioManager() {
        return studioManager;
    }
    
    public CommandManager getCommandManager() {
        return commandManager;
    }

    public StructureManager getStructureManager() {
        return structureManager;
    }
}
