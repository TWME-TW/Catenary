package dev.twme.catenary.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 定義懸掛結構的渲染物品
 */
public class RenderItem {

    private ItemStack item;
    private boolean isBlock;
    private float scale;
    private float rotationX;
    private float rotationY;
    private float rotationZ;
    
    public RenderItem(Material material, boolean isBlock) {
        this(new ItemStack(material), isBlock, 1.0f, 0, 0, 0);
    }
    
    public RenderItem(ItemStack item, boolean isBlock) {
        this(item, isBlock, 1.0f, 0, 0, 0);
    }
    
    public RenderItem(Material material, boolean isBlock, float scale, float rotX, float rotY, float rotZ) {
        this(new ItemStack(material), isBlock, scale, rotX, rotY, rotZ);
    }
    
    public RenderItem(ItemStack item, boolean isBlock, float scale, float rotX, float rotY, float rotZ) {
        this.item = item;
        this.isBlock = isBlock;
        this.scale = scale;
        this.rotationX = rotX;
        this.rotationY = rotY;
        this.rotationZ = rotZ;
    }
    
    public ItemStack getItem() {
        return item;
    }
    
    public void setItem(ItemStack item) {
        this.item = item;
    }
    
    public boolean isBlock() {
        return isBlock;
    }
    
    public void setBlock(boolean block) {
        isBlock = block;
    }
    
    public float getScale() {
        return scale;
    }
    
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public float getRotationX() {
        return rotationX;
    }
    
    public void setRotationX(float rotationX) {
        this.rotationX = rotationX;
    }
    
    public float getRotationY() {
        return rotationY;
    }
    
    public void setRotationY(float rotationY) {
        this.rotationY = rotationY;
    }
    
    public float getRotationZ() {
        return rotationZ;
    }
    
    public void setRotationZ(float rotationZ) {
        this.rotationZ = rotationZ;
    }
    
    /**
     * 取得轉換矩陣
     */
    public Transformation getTransformation() {
        // 計算四元數旋轉
        Quaternionf rotation = new Quaternionf()
            .rotateX((float) Math.toRadians(rotationX))
            .rotateY((float) Math.toRadians(rotationY))
            .rotateZ((float) Math.toRadians(rotationZ));
        
        // 設定比例向量
        Vector3f scaleVec = new Vector3f(scale, scale, scale);
        
        // 設定轉換矩陣
        return new Transformation(
            new Vector3f(0, 0, 0),  // 平移向量為零
            rotation,               // 旋轉四元數
            scaleVec,               // 比例向量
            new Quaternionf()       // 左乘旋轉四元數，通常為單位四元數
        );
    }
}
