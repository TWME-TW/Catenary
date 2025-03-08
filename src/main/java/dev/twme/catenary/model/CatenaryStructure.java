package dev.twme.catenary.model;

import dev.twme.catenary.math.Vector3D;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.UUID;

/**
 * 懸掛結構模型
 */
public class CatenaryStructure {
    
    private final UUID id;
    private final UUID ownerId;
    private String name;
    private Vector3D start;
    private Vector3D end;
    private List<Vector3D> points;
    private RenderItem renderItem;
    private double slack;
    private int segments;
    private double spacing;
    private boolean visible;
    private World world;
    
    /**
     * 建立懸掛結構
     */
    public CatenaryStructure(UUID ownerId, Location start, Location end, RenderItem renderItem, 
                             double slack, int segments) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = "懸掛結構-" + id.toString().substring(0, 8);
        this.start = new Vector3D(start);
        this.end = new Vector3D(end);
        this.renderItem = renderItem;
        this.slack = slack;
        this.segments = segments;
        this.spacing = 0.5;  // 預設間距
        this.visible = true;
        this.world = start.getWorld();
    }
    
    /**
     * 從保存資料建立懸掛結構
     */
    public CatenaryStructure(UUID id, UUID ownerId, String name, Vector3D start, Vector3D end, 
                             List<Vector3D> points, RenderItem renderItem, double slack, 
                             int segments, double spacing, boolean visible, World world) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.start = start;
        this.end = end;
        this.points = points;
        this.renderItem = renderItem;
        this.slack = slack;
        this.segments = segments;
        this.spacing = spacing;
        this.visible = visible;
        this.world = world;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector3D getStart() {
        return start;
    }

    public Vector3D getEnd() {
        return end;
    }

    public List<Vector3D> getPoints() {
        return points;
    }

    public void setPoints(List<Vector3D> points) {
        this.points = points;
    }

    public RenderItem getRenderItem() {
        return renderItem;
    }

    public void setRenderItem(RenderItem renderItem) {
        this.renderItem = renderItem;
    }

    public double getSlack() {
        return slack;
    }

    public void setSlack(double slack) {
        this.slack = slack;
    }

    public int getSegments() {
        return segments;
    }

    public void setSegments(int segments) {
        this.segments = segments;
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public World getWorld() {
        return world;
    }
    
    public Location getStartLocation() {
        return new Location(world, start.getX(), start.getY(), start.getZ());
    }
    
    public Location getEndLocation() {
        return new Location(world, end.getX(), end.getY(), end.getZ());
    }
}
