package dev.twme.catenary.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * 懸掛結構預設配置
 */
public class Preset {
    
    private final String id;
    private final String name;
    private final String description;
    private final RenderItem renderItem;
    private final double defaultSlack;
    private final int defaultSegments;
    private final double defaultSpacing;
    private final ItemStack displayIcon;
    private final boolean requirePermission;
    
    public Preset(String id, String name, String description, RenderItem renderItem, 
                 double defaultSlack, int defaultSegments, double defaultSpacing, 
                 ItemStack displayIcon, boolean requirePermission) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.renderItem = renderItem;
        this.defaultSlack = defaultSlack;
        this.defaultSegments = defaultSegments;
        this.defaultSpacing = defaultSpacing;
        this.displayIcon = displayIcon;
        this.requirePermission = requirePermission;
    }
    
    /**
     * 從配置節區讀取預設
     */
    public static Preset fromConfig(String id, ConfigurationSection section) {
        String name = section.getString("name", id);
        String description = section.getString("description", "");
        
        Material material = Material.getMaterial(section.getString("material", "CHAIN"));
        if (material == null) material = Material.CHAIN;
        
        boolean isBlock = section.getBoolean("isBlock", false);
        float scale = (float) section.getDouble("scale", 1.0);
        float rotX = (float) section.getDouble("rotation.x", 0);
        float rotY = (float) section.getDouble("rotation.y", 0);
        float rotZ = (float) section.getDouble("rotation.z", 0);
        
        RenderItem renderItem = new RenderItem(material, isBlock, scale, rotX, rotY, rotZ);
        
        double slack = section.getDouble("slack", 0.3);
        int segments = section.getInt("segments", 10);
        double spacing = section.getDouble("spacing", 0.5);
        
        Material iconMaterial = Material.getMaterial(section.getString("icon", material.name()));
        if (iconMaterial == null) iconMaterial = material;
        ItemStack icon = new ItemStack(iconMaterial);
        
        boolean requirePermission = section.getBoolean("requirePermission", false);
        
        return new Preset(id, name, description, renderItem, slack, segments, spacing, icon, requirePermission);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public RenderItem getRenderItem() {
        return renderItem;
    }

    public double getDefaultSlack() {
        return defaultSlack;
    }

    public int getDefaultSegments() {
        return defaultSegments;
    }

    public double getDefaultSpacing() {
        return defaultSpacing;
    }

    public ItemStack getDisplayIcon() {
        return displayIcon;
    }

    public boolean isRequirePermission() {
        return requirePermission;
    }
}
