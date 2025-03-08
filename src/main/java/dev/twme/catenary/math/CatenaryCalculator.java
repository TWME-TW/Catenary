package dev.twme.catenary.math;

import java.util.ArrayList;
import java.util.List;

/**
 * 懸掛曲線計算器
 */
public class CatenaryCalculator {

    /**
     * 計算兩點之間的懸掛曲線點位
     *
     * @param start 起始點
     * @param end 結束點
     * @param slack 鬆緊度參數 (0-1)，0 為直線，1 為最大鬆弛
     * @param segments 曲線分段數量
     * @return 計算出的曲線上的點位列表
     */
    public List<Vector3D> calculatePoints(Vector3D start, Vector3D end, double slack, int segments) {
        List<Vector3D> points = new ArrayList<>();
        
        // 至少需要2個點
        if (segments < 2) {
            segments = 2;
        }
        
        // 計算兩點之間的水平距離
        Vector3D horizontal = new Vector3D(end.getX() - start.getX(), 0, end.getZ() - start.getZ());
        double horizontalDistance = horizontal.length();
        
        // 計算高度差
        double heightDifference = end.getY() - start.getY();
        
        // 計算懸掛參數 a，控制曲線形狀
        double a = horizontalDistance * slack * 0.5;
        if (a < 0.01) a = 0.01; // 避免除以零或極小值
        
        // 計算懸掛參數
        double cosA = horizontal.getX() / horizontalDistance;
        double sinA = horizontal.getZ() / horizontalDistance;
        
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double x = t * horizontalDistance;
            
            // 使用懸掛曲線方程式: y = a * cosh(x/a) - a
            double catY = a * Math.cosh((x - horizontalDistance * 0.5) / a) - a;
            
            // 調整y以匹配起點和終點
            double y = catY + start.getY() + t * heightDifference;
            
            // 計算在3D空間中的點位置
            double worldX = start.getX() + x * cosA;
            double worldZ = start.getZ() + x * sinA;
            
            points.add(new Vector3D(worldX, y, worldZ));
        }
        
        return points;
    }
    
    /**
     * 使用倒推法計算懸掛曲線的參數
     * 
     * @param start 起始點
     * @param end 結束點
     * @param middleY 曲線中點的Y坐標
     * @param segments 曲線分段數量
     * @return 計算出的曲線上的點位列表
     */
    public List<Vector3D> calculatePointsWithMiddlePoint(Vector3D start, Vector3D end, double middleY, int segments) {
        // 使用二分法尋找合適的slack值使中點高度符合要求
        double minSlack = 0.001;
        double maxSlack = 1.0;
        double slack = 0.5;
        double tolerance = 0.01;
        
        List<Vector3D> bestPoints = null;
        double bestDifference = Double.MAX_VALUE;
        
        for (int iteration = 0; iteration < 20; iteration++) {
            List<Vector3D> points = calculatePoints(start, end, slack, segments);
            int middleIndex = points.size() / 2;
            double currentMiddleY = points.get(middleIndex).getY();
            double difference = Math.abs(currentMiddleY - middleY);
            
            // 更新最佳結果
            if (difference < bestDifference) {
                bestDifference = difference;
                bestPoints = points;
            }
            
            // 達到足夠精度則退出
            if (difference < tolerance) {
                break;
            }
            
            // 二分法調整slack
            if (currentMiddleY > middleY) {
                minSlack = slack;
            } else {
                maxSlack = slack;
            }
            slack = (minSlack + maxSlack) / 2;
        }
        
        return bestPoints;
    }
}
