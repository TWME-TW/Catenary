package dev.twme.catenary.model;

import dev.twme.catenary.math.Vector3D;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.UUID;

/**
 * 懸掛結構資料模型
 */
public class CatenaryStructure {
    private final UUID id;
    private final UUID ownerId;
    private String name;
    private final World world;
    private Vector3D start;
    private Vector3D end;
    private List<Vector3D> points;
    private double slack;
    private int segments;
    private double spacing;
    private RenderItem renderItem;
    private boolean visible;

    public CatenaryStructure(UUID id, UUID ownerId, String name, World world, Vector3D start, Vector3D end, 
                              double slack, int segments, double spacing, RenderItem renderItem) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.world = world;
        this.start = start;
        this.end = end;
        this.slack = slack;
        this.segments = segments;
        this.spacing = spacing;
        this.renderItem = renderItem;
        this.visible = true;
    }

    public CatenaryStructure(UUID ownerId, Location start, Location end, RenderItem renderItem, 
                             double slack, int segments) {
        this(UUID.randomUUID(), ownerId, "懸掛結構-" + UUID.randomUUID().toString().substring(0, 8), 
             start.getWorld(), new Vector3D(start), new Vector3D(end), slack, segments, 0.5, renderItem);
    }

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

    public World getWorld() {
        return world;
    }

    public Vector3D getStart() {
        return start;
    }

    public void setStart(Vector3D start) {
        this.start = start;
    }

    public Vector3D getEnd() {
        return end;
    }

    public void setEnd(Vector3D end) {
        this.end = end;
    }

    public List<Vector3D> getPoints() {
        return points;
    }

    public void setPoints(List<Vector3D> points) {
        this.points = points;
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

    public RenderItem getRenderItem() {
        return renderItem;
    }

    public void setRenderItem(RenderItem renderItem) {
        this.renderItem = renderItem;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Location getStartLocation() {
        return new Location(world, start.getX(), start.getY(), start.getZ());
    }

    public Location getEndLocation() {
        return new Location(world, end.getX(), end.getY(), end.getZ());
    }
}
