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
    
    private int patrolSearchCount = 0;
    private int lastPatrolX = -1;
    private int lastPatrolY = -1;
    private double patrolAngle = 0;
    
    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;
    
    private static final int DEFAULT_AREA_SIZE = 200;

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
        int startX = api.getX();
        int startY = api.getY();
        areaMinX = startX - DEFAULT_AREA_SIZE / 2;
        areaMaxX = startX + DEFAULT_AREA_SIZE / 2;
        areaMinY = startY - DEFAULT_AREA_SIZE / 2;
        areaMaxY = startY + DEFAULT_AREA_SIZE / 2;
        gameMessage("Woodcutting bot started! Area: " + areaMinX + "-" + areaMaxX + ", " + areaMinY + "-" + areaMaxY);
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
        
        if (areaMinX != null && !isInArea(api.getX(), api.getY())) {
            int centerX = (areaMinX + areaMaxX) / 2;
            int centerY = (areaMinY + areaMaxY) / 2;
            api.walkTo(centerX, centerY);
            return random(500, 800);
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
        GameObject tree = findTreeInArea();
        
        if (tree == null || tree.isRemoved()) {
            emptyTreeSearchCount++;
            targetTree = null;
            
            if (emptyTreeSearchCount > 3) {
                emptyTreeSearchCount = 0;
                return patrolArea();
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
    
    private GameObject findTreeInArea() {
        if (areaMinX != null && areaMaxX != null && areaMinY != null && areaMaxY != null) {
            return api.getNearestObjectInArea(treeIds, areaMinX.intValue(), areaMaxX.intValue(), areaMinY.intValue(), areaMaxY.intValue());
        }
        return api.getNearestObjectInLocalArea(treeIds, 25);
    }
    

    private int patrolArea() {
        int currentX = api.getX();
        int currentY = api.getY();
        
        if (areaMinX == null || areaMaxX == null || areaMinY == null || areaMaxY == null) {
            int[] offsets = { -10, -8, -6, 6, 8, 10 };
            int newX = currentX + offsets[random(0, offsets.length - 1)];
            int newY = currentY + offsets[random(0, offsets.length - 1)];
            api.walkTo(newX, newY);
            return random(500, 800);
        }
        
        int areaWidth = areaMaxX - areaMinX;
        int areaHeight = areaMaxY - areaMinY;
        
        if (areaWidth <= 0 || areaHeight <= 0) {
            int centerX = (areaMinX + areaMaxX) / 2;
            int centerY = (areaMinY + areaMaxY) / 2;
            api.walkTo(centerX, centerY);
            return random(500, 800);
        }
        
        patrolAngle += 1;
        if (patrolAngle > 8) {
            patrolAngle = 0;
        }
        
        int newX, newY;
        
        if (patrolAngle < 2) {
            newX = currentX + 8;
            newY = currentY;
        } else if (patrolAngle < 4) {
            newX = currentX;
            newY = currentY + 8;
        } else if (patrolAngle < 6) {
            newX = currentX - 8;
            newY = currentY;
        } else {
            newX = currentX;
            newY = currentY - 8;
        }
        
        if (newX < areaMinX) newX = areaMinX;
        if (newX > areaMaxX) newX = areaMaxX;
        if (newY < areaMinY) newY = areaMinY;
        if (newY > areaMaxY) newY = areaMaxY;
        
        lastPatrolX = newX;
        lastPatrolY = newY;
        
        api.walkTo(newX, newY);
        return random(600, 1000);
    }
    private int bankLogs() {
        if (!api.isBankOpen()) {
            api.openBank();
            return random(800, 1200);
        }

        for (int i = bankCurrentLogIndex; i < logIds.length; i++) {
            int logId = logIds[i];
            int count = api.getInventoryCount(logId);

            while (count > 0 && api.getInventoryCount(logId) > 0) {
                api.depositItem(logId, 1);
                logsChopped++;
                count--;
                bankDepositedCount++;
            }

            if (count == 0 && api.getInventoryCount(logId) == 0) {
                bankDepositedCount = 0;
                bankCurrentLogIndex = i + 1;
            }
        }

        boolean inventoryEmpty = true;
        for (int id : logIds) {
            if (api.getInventoryCount(id) > 0) {
                inventoryEmpty = false;
                break;
            }
        }

        if (inventoryEmpty) {
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
