package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;
import org.nemotech.rsc.model.NPC;

public class WoodcuttingBot extends Bot {
    
    private int[] treeIds = { 0, 1, 70 };
    private int[] logIds = { 14 };
    
    private static final int[] AXE_IDS = { 87, 12, 88, 203, 204, 405, 428, 594 };
    
    private static final int[] BANK_NPC_IDS = { 95, 224, 542, 773, 2048, 2049, 2050, 4934, 8068, 19444 };
    
    private enum State {
        IDLE, WALKING_TO_TREE, CHOPPING, WALKING_TO_BANK, BANKING
    }
    
    private State state = State.IDLE;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    private int treesChopped = 0;
    private int emptyTreeSearchCount = 0;
    private int consecutiveBankFailures = 0;
    private int consecutiveWalkFailures = 0;
    
    private Integer bankX = null;
    private Integer bankY = null;
    private boolean hasBankLocation = false;
    private boolean wanderEnabled = false;
    
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
    
    public void setBankLocation(int x, int y) {
        this.bankX = x;
        this.bankY = y;
        this.hasBankLocation = true;
    }
    
    public void clearBankLocation() {
        this.bankX = null;
        this.bankY = null;
        this.hasBankLocation = false;
    }
    
    public void setWanderEnabled(boolean enabled) {
        this.wanderEnabled = enabled;
    }
    
    public boolean isWanderEnabled() {
        return wanderEnabled;
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
        
        if (!hasAxe()) {
            gameMessage("@red@WARNING: No axe found! You need an axe to chop trees.");
        }
        
        if (hasBankLocation) {
            gameMessage("Bank location set to: (" + bankX + ", " + bankY + ")");
        } else {
            gameMessage("No bank location set - will attempt to use nearest banker.");
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (api.isBankOpen()) {
            api.closeBank();
        }
        gameMessage("Woodcutting bot stopped. Total logs banked: " + logsChopped);
    }
    
    private boolean hasAxe() {
        for (int axeId : AXE_IDS) {
            if (api.hasItem(axeId)) {
                return true;
            }
        }
        return false;
    }
    
    private NPC findNearestBanker() {
        return api.getNearestNpc(BANK_NPC_IDS);
    }
    
    @Override
    public int loop() {
        boolean busy = api.isBusy();
        boolean moving = api.isMoving();
        int invCount = api.getInventorySize();
        boolean invFull = api.isInventoryFull();
        boolean bankOpen = api.isBankOpen();

        long now = System.currentTimeMillis();
        if (now - lastDebugTime > 3000) {
            lastDebugTime = now;
            System.out.println("BOT: state=" + state + " busy=" + busy + " moving=" + moving + 
                " inv=" + invCount + " bank=" + bankOpen);
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

        if (api.handleFatigue()) {
            gameMessage("Rested at a bed - fatigue reset.");
            return random(500, 1000);
        }

        if (invFull) {
            System.out.println("BOT: Inventory full, going to bank");
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
            if (targetTree != null && !targetTree.isRemoved()) {
                int dist = api.distanceTo(targetTree);
                if (dist <= 2) {
                    state = State.CHOPPING;
                    api.interactObject(targetTree);
                    treesChopped++;
                    return 10;
                }
            }
            state = State.IDLE;
            targetTree = null;
        }
        
        return chopTree();
    }
    
    private int chopTree() {
        if (!hasAxe()) {
            gameMessage("@red@No axe found! Stopping bot.");
            stop();
            return 0;
        }
        
        GameObject tree = api.getNearestObject(treeIds);
        
        if (tree == null || tree.isRemoved()) {
            state = State.IDLE;
            targetTree = null;
            emptyTreeSearchCount++;
            
            if (emptyTreeSearchCount > 2) {
                emptyTreeSearchCount = 0;
                if (wanderEnabled) {
                    gameMessage("No trees found nearby, wandering to find more...");
                    wanderToFindTrees();
                } else {
                    gameMessage("No trees found nearby. Enable wander to search wider areas.");
                }
            }
            return random(500, 1500);
        }
        
        emptyTreeSearchCount = 0;
        
        if (targetTree != null && targetTree.equals(tree) && tree.isRemoved()) {
            state = State.IDLE;
            targetTree = null;
            return random(100, 300);
        }
        
        int dist = api.distanceTo(tree);
        
        if (dist > 2) {
            state = State.WALKING_TO_TREE;
            if (!tree.equals(targetTree)) {
                targetTree = tree;
                consecutiveWalkFailures = 0;
            }
            boolean walked = api.walkTo(tree.getX(), tree.getY());
            if (!walked) {
                consecutiveWalkFailures++;
                if (consecutiveWalkFailures > 3) {
                    gameMessage("Can't reach tree at (" + tree.getX() + ", " + tree.getY() + "), finding another...");
                    targetTree = null;
                    consecutiveWalkFailures = 0;
                    state = State.IDLE;
                    return random(500, 1000);
                }
                state = State.IDLE;
                return random(200, 500);
            }
            consecutiveWalkFailures = 0;
            return 10;
        }
        
        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;
        consecutiveWalkFailures = 0;
        
        return 10;
    }
    
    private void wanderToFindTrees() {
        if (!wanderEnabled) {
            return;
        }
        
        int currentX = api.getX();
        int currentY = api.getY();
        
        int[] offsets = { -20, -15, -10, 10, 15, 20 };
        int randomOffsetX = offsets[random(0, offsets.length - 1)];
        int randomOffsetY = offsets[random(0, offsets.length - 1)];
        
        int newX = currentX + randomOffsetX;
        int newY = currentY + randomOffsetY;
        
        if (isInArea(newX, newY)) {
            api.walkTo(newX, newY);
        } else {
            int centerX = (areaMinX + areaMaxX) / 2;
            int centerY = (areaMinY + areaMaxY) / 2;
            int randX = areaMinX + random(0, areaMaxX - areaMinX);
            int randY = areaMinY + random(0, areaMaxY - areaMinY);
            api.walkTo(randX, randY);
        }
    }
    
    private int bankLogs() {
        if (!api.isBankOpen()) {
            if (hasBankLocation && !isAtBank()) {
                state = State.WALKING_TO_BANK;
                System.out.println("BOT: Walking to bank at (" + bankX + ", " + bankY + ")");
                boolean walked = api.walkTo(bankX, bankY);
                if (!walked) {
                    System.out.println("BOT: Failed to walk to bank, trying nearest banker");
                    state = State.BANKING;
                }
                return random(500, 1000);
            }
            
            NPC banker = findNearestBanker();
            if (banker != null && api.distanceTo(banker) <= 8) {
                api.talkToNpc(banker);
                consecutiveBankFailures++;
                if (consecutiveBankFailures > 5) {
                    gameMessage("@red@Bank interaction failed, dropping logs...");
                    return dropLogsAndContinue();
                }
                return random(1500, 2500);
            }
            
            if (!hasBankLocation && banker == null) {
                gameMessage("@red@No bank or banker found! Dropping logs...");
                return dropLogsAndContinue();
            }
            
            api.openBank();
            consecutiveBankFailures++;
            if (consecutiveBankFailures > 5) {
                gameMessage("@red@Bank command failed, dropping logs...");
                return dropLogsAndContinue();
            }
            return random(1500, 2500);
        }
        
        for (int logId : logIds) {
            int count = api.getInventoryCount(logId);
            if (count > 0) {
                api.depositItem(logId, count);
                logsChopped += count;
            }
        }
        
        consecutiveBankFailures = 0;
        api.closeBank();
        state = State.IDLE;
        return 10;
    }
    
    private boolean isAtBank() {
        if (!hasBankLocation) {
            return api.isBankOpen();
        }
        return api.distanceTo(bankX, bankY) <= 3;
    }
    
    private int dropLogsAndContinue() {
        for (int logId : logIds) {
            int count = api.getInventoryCount(logId);
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    api.dropItemById(logId);
                }
            }
        }
        consecutiveBankFailures = 0;
        state = State.IDLE;
        return random(500, 1000);
    }
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public State getState() {
        return state;
    }
}
