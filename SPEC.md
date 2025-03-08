# Catenary Paper 插件開發規格

## 1. 資料包分析

分析了 Catenary 資料包，這是一個用於建立懸掛式結構（如鏈條、燈籠、電纜等）的工具。主要功能包括：

### 1.1 核心功能

- **懸掛結構生成**：在兩點之間建立懸垂曲線(catenary)形狀的結構
- **多種預設模板**：包含 16+ 種預設結構（鏈條、燈籠、玻璃片、電纜等）
- **工作室模式**：用於建立和自訂懸掛結構的界面
- **進度與配方系統**：解鎖不同的懸掛結構設計

### 1.2 技術實現方式

- 使用 `item_display` 和 `block_display` 實體來渲染懸掛物
- 使用標記實體來管理結構點位
- 使用 NBT 資料存儲設計參數
- 使用戰利品表管理配方和預設值

## 2. 插件架構設計

### 2.1 核心模組

1. **懸掛曲線計算引擎**
   - 實現懸掛曲線數學公式
   - 處理結構點位生成與空間分布
   - 支援多種懸掛參數（鬆緊度、重力影響等）

2. **渲染系統**
   - 管理 `item_display` 和 `block_display` 實體
   - 處理不同物品的旋轉與位置
   - 優化大量實體的顯示效能

3. **工作室界面**
   - 使用 Paper 的物品界面 API
   - 可視化選項調整器
   - 即時預覽功能

4. **配方與預設系統**
   - 儲存與讀取預設配置
   - 使用者自訂配方管理
   - 兼容插件配方解鎖系統

### 2.2 類別結構

```
com.github.username.catenary
├── CatenaryPlugin.java              // 主插件類別
├── api/                            // API 介面
├── config/                         // 配置系統
├── math/                           // 數學計算
│   ├── CatenaryCalculator.java     // 懸掛曲線計算器
│   └── Vector3D.java               // 3D 向量運算
├── model/                          // 資料模型
│   ├── CatenaryStructure.java      // 懸掛結構模型
│   ├── RenderItem.java             // 渲染物品定義
│   └── Preset.java                 // 預設配置
├── render/                         // 渲染系統
│   ├── DisplayEntityManager.java   // 顯示實體管理
│   ├── ItemRenderer.java           // 物品渲染器
│   └── BlockRenderer.java          // 方塊渲染器
├── studio/                         // 工作室界面
│   ├── StudioManager.java          // 工作室管理器
│   ├── gui/                        // GUI 組件
│   └── interaction/                // 使用者互動處理
└── util/                           // 工具類別
    ├── NBTUtil.java                // NBT 資料工具
    └── ParticleUtil.java           // 粒子效果工具
```

## 3. 功能實現計畫

### 3.1 懸掛曲線計算

實現公式：`y = a * cosh(x/a) - a`，其中 `a` 為鬆緊度參數。

```java
public List<Vector3D> calculatePoints(Vector3D start, Vector3D end, double slack, int segments) {
    List<Vector3D> points = new ArrayList<>();
    // 計算兩點之間的懸掛曲線點位
    // ...
    return points;
}
```

### 3.2 實體渲染系統

```java
public void renderStructure(CatenaryStructure structure) {
    List<Vector3D> points = structure.getPoints();
    RenderItem renderItem = structure.getRenderItem();
    
    // 清除現有顯示實體
    clearDisplayEntities(structure.getId());
    
    // 在每個點位生成顯示實體
    for (Vector3D point : points) {
        createDisplayEntity(point, renderItem, structure.getId());
    }
}
```

### 3.3 使用者互動流程

1. 玩家選擇錨點工具
2. 設置第一個錨點（右鍵點擊）
3. 設置第二個錨點（右鍵點擊）
4. 開啟工作室界面，調整參數
5. 套用變更，生成懸掛結構

### 3.4 配方和預設系統

```java
public class PresetManager {
    private Map<String, Preset> presets = new HashMap<>();
    
    public void loadPresets() {
        // 載入內建預設
        loadBuiltinPresets();
        
        // 載入使用者自訂預設
        loadCustomPresets();
    }
    
    public Preset getRandomPreset() {
        // 隨機選擇一個預設
        // ...
    }
    
    // 其他方法...
}
```

## 4. 效能優化策略

1. **分批次渲染** - 處理大型結構時分批次建立顯示實體
2. **距離剪裁** - 過遠的懸掛結構暫停更新或降低細節
3. **實體池化** - 重複使用顯示實體而非反覆建立/刪除
4. **行為延遲** - 對於非重要更新使用延遲排程執行
5. **區塊加載管理** - 只在加載的區塊中更新懸掛結構

## 5. 指令系統

### 5.1 基本指令

```
/catenary create <preset> - 開始建立新的懸掛結構
/catenary edit <id> - 編輯現有結構
/catenary remove <id> - 移除結構
/catenary list - 列出附近的懸掛結構
/catenary presets - 顯示可用的預設清單
```

### 5.2 管理員指令

```
/catenary admin reload - 重新載入配置
/catenary admin cleanup - 清理無效實體
/catenary admin export <id> <name> - 匯出結構為新預設
```

## 6. 相容性考量

- 支援 1.19+ 版本
- 兼容 WorldEdit 和 WorldGuard 進行權限管理
- 提供 API 供其他插件擴展

## 7. 未來擴展

- 動態懸掛物（支援動畫和物理）
- 更多種類的預設結構
- 支援多點連接（如網絡或網格）
- 使用者社群預設分享系統

## 8. 實作時程

1. 核心計算引擎 - 2 週
2. 渲染系統 - 2 週
3. 使用者界面 - 3 週
4. 指令和互動系統 - 1 週
5. 預設和配置系統 - 1 週
6. 測試與除錯 - 3 週

總計：約 12 週開發週期
