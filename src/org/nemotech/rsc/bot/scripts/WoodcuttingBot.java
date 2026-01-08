package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {
    
    private int[] treeIds = { 0, 1, 70 };
    private int[] logIds = { 14, 632, 633, 634, 635, 636 };
    
    private enum State {
        CHOPPING, WALKING, BANKING
    }
    
    private State state = State.CHOPPING;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    private int treesChopped = 0;
    private int emptyTreeSearchCount = 0;
    
    private int bankDepositedCount = 0;
    private int bankCurrentLogIndex = 0;
    
    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;

    public WoodcuttingBot() {
        super("Woodcutting Bot");
    }
    
    public WoodcuttingBot(int[] treeIds, int[] logIds) {
        super("Woodcutting Bot");
        this.treeIds = treeIds;
        this.logIds = logIds;
    }
    
    public void setTreeIds(int... ids) {
        this.treeIds = ids;
    }
    
    public void setLogIds(int... ids) {
        this.logIds = ids;
    }
    
    public void setAreaBounds(int minX, int maxX, int minY, int maxY) {
        this.areaMinX = minX;
        this.areaMaxX = maxX;
        this.areaMinY = minY;
        this.areaMaxY = maxY;
    }
    
    public void clearAreaBounds() {
        this.areaMinX = null;
        this.areaMaxX = null;
        this.areaMinY = null;
        this.areaMaxY = null;
    }
    
    public boolean isInArea(int x, int y) {
        if (areaMinX == null || areaMaxX == null || areaMinY == null || areaMaxY == null) {
            return true;
        }
        return x >= areaMinX && x <= areaMaxX && y >= areaMinY && y <= areaMaxY;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        logsChopped = 0;
        state = State.CHOPPING;
        gameMessage("Woodcutting bot started!");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (api.isBankOpen()) {
            api.closeBank();
        }
        gameMessage("Woodcutting bot stopped. Total logs banked: " + logsChopped);
    }
    
    @Override
    public int loop() {
        if (api.isBusy() || api.isMoving()) {
            return 50;
        }
        
        if (api.isInventoryFull()) {
            bankDepositedCount = 0;
            bankCurrentLogIndex = 0;
            return bankLogs();
        }
        
        if (bankDepositedCount > 0) {
            return bankLogs();
        }
        
        return chopTree();
    }
    
    private int chopTree() {
        GameObject tree;
        if (areaMinX != null && areaMaxX != null && areaMinY != null && areaMaxY != null) {
            tree = api.getNearestObjectInArea(treeIds, areaMinX.intValue(), areaMaxX.intValue(), areaMinY.intValue(), areaMaxY.intValue());
        } else {
            tree = api.getNearestObject(treeIds);
        }
        
        if (tree == null || tree.isRemoved()) {
            emptyTreeSearchCount++;
            targetTree = null;
            
            if (emptyTreeSearchCount > 10) {
                emptyTreeSearchCount = 0;
                int currentX = api.getX();
                int currentY = api.getY();
                int[] offsets = { -8, -6, -4, 4, 6, 8 };
                int randomOffsetX = offsets[random(0, offsets.length - 1)];
                int randomOffsetY = offsets[random(0, offsets.length - 1)];
                int newX = currentX + randomOffsetX;
                int newY = currentY + randomOffsetY;
                
                if (isInArea(newX, newY)) {
                    api.walkTo(newX, newY);
                } else if (areaMinX != null) {
                    int centerX = (areaMinX + areaMaxX) / 2;
                    int centerY = (areaMinY + areaMaxY) / 2;
                    api.walkTo(centerX, centerY);
                }
            }
            return random(200, 400);
        }
        
        emptyTreeSearchCount = 0;
        
        if (!isInArea(tree.getX(), tree.getY())) {
            targetTree = null;
            return random(200, 400);
        }
        
        int dist = api.distanceTo(tree);
        
        if (dist > 1) {
            state = State.WALKING;
            targetTree = tree;
            api.walkTo(tree.getX(), tree.getY());
            return random(300, 500);
        }
        
        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;
        
        return 800;
    }
    
    private int bankLogs() {
        if (!api.isBankOpen()) {
            api.openBank();
            return random(800, 1200);
        }
        
        int depositedThisCall = 0;
        
        for (int i = bankCurrentLogIndex; i < logIds.length; i++) {
            int logId = logIds[i];
            int count = api.getInventoryCount(logId);
            
            if (count > 0) {
                int toDeposit = count - bankDepositedCount;
                if (toDeposit > 0) {
                    api.depositItem(logId, 1);
                    bankDepositedCount++;
                    depositedThisCall++;
                    logsChopped++;
                    
                    if (bankDepositedCount < count) {
                        bankCurrentLogIndex = i;
                        return random(200, 400);
                    }
                }
                bankDepositedCount = 0;
                bankCurrentLogIndex = i + 1;
            }
        }
        
        if (depositedThisCall > 0) {
            gameMessage("Banked " + depositedThisCall + " logs. Total: " + logsChopped);
        }
        
        if (api.getInventorySize() == 0 || !api.isInventoryFull()) {
            api.closeBank();
            bankDepositedCount = 0;
            bankCurrentLogIndex = 0;
            return random(300, 500);
        }
        
        return random(200, 400);
    }
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public State getState() {
        return state;
    }
}
