package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {
    
    private int[] treeIds = { 0, 1, 70 };
    private int[] logIds = { 14 };
    
    private enum State {
        IDLE, WALKING_TO_TREE, CHOPPING, BANKING
    }
    
    private State state = State.IDLE;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    private int treesChopped = 0;
    private int emptyTreeSearchCount = 0;
    private int consecutiveBankFailures = 0;
    private int lastTreeX = 0;
    private int lastTreeY = 0;
    
    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;
    
    private long lastDebugTime = 0;
    private long stuckTime = 0;

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
        state = State.IDLE;
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
        boolean busy = api.isBusy();
        boolean moving = api.isMoving();
        int invCount = api.getInventorySize();
        int maxInv = 30;
        boolean invFull = invCount >= maxInv;
        boolean bankOpen = api.isBankOpen();

        long now = System.currentTimeMillis();
        if (now - lastDebugTime > 3000) {
            lastDebugTime = now;
            System.out.println("BOT: state=" + state + " busy=" + busy + " moving=" + moving + 
                " inv=" + invCount + "/" + maxInv + " bank=" + bankOpen);
        }

        if (busy || moving) {
            if (stuckTime == 0) {
                stuckTime = now;
            } else if (now - stuckTime > 30000) {
                System.out.println("BOT: Recovering from stuck state");
                stuckTime = 0;
                state = State.IDLE;
                return 10;
            }
            return 10;
        }
        stuckTime = 0;

        if (invFull) {
            System.out.println("BOT: Inventory full (" + invCount + "/" + maxInv + "), going to bank");
            state = State.BANKING;
            targetTree = null;
            return bankLogs();
        }
        
        if (bankOpen) {
            System.out.println("BOT: Closing bank");
            api.closeBank();
            return 10;
        }
        
        if (state == State.CHOPPING) {
            state = State.IDLE;
            targetTree = null;
        }
        
        return chopTree();
    }
    
    private int chopTree() {
        GameObject tree;
        if (areaMinX != null && areaMaxX != null && areaMinY != null && areaMaxY != null) {
            tree = api.getNearestObjectInArea(treeIds, areaMinX, areaMaxX, areaMinY, areaMaxY);
        } else {
            tree = api.getNearestObject(treeIds);
        }
        
        if (tree == null || tree.isRemoved()) {
            state = State.IDLE;
            targetTree = null;
            emptyTreeSearchCount++;
            
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
                } else {
                    int centerX = (areaMinX + areaMaxX) / 2;
                    int centerY = (areaMinY + areaMaxY) / 2;
                    api.walkTo(centerX, centerY);
                }
                return random(1000, 2000);
            }
            return random(800, 1200);
        }
        
        emptyTreeSearchCount = 0;
        
        if (!isInArea(tree.getX(), tree.getY())) {
            state = State.IDLE;
            targetTree = null;
            return random(200, 400);
        }
        
        int dist = api.distanceTo(tree);
        
        if (dist > 1) {
            state = State.WALKING_TO_TREE;
            targetTree = tree;
            api.walkTo(tree.getX(), tree.getY());
            return random(600, 1000);
        }
        
        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;
        
        return 500;
    }
    
    private int bankLogs() {
        if (!api.isBankOpen()) {
            api.openBank();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
            return 100;
        }
        
        int totalDeposited = 0;
        
        for (int logId : logIds) {
            int count = api.getInventoryCount(logId);
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    api.depositItem(logId, 1);
                    totalDeposited++;
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {}
                }
            }
        }
        
        if (totalDeposited > 0) {
            gameMessage("Banked " + totalDeposited + " logs.");
        }
        
        api.closeBank();
        state = State.IDLE;
        return 10;
    }
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public State getState() {
        return state;
    }
}
