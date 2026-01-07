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
            System.out.println("BOT: Was chopping, finding new tree");
            state = State.IDLE;
            targetTree = null;
        }
        
        return chopTree();
    }
    
    private int chopTree() {
        GameObject tree = api.getNearestObject(treeIds);
        
        if (tree == null || tree.isRemoved()) {
            state = State.IDLE;
            targetTree = null;
            emptyTreeSearchCount++;
            
            if (emptyTreeSearchCount > 5) {
                gameMessage("No trees found nearby, searching...");
                emptyTreeSearchCount = 0;
            }
            return random(500, 1500);
        }
        
        emptyTreeSearchCount = 0;
        
        int dist = api.distanceTo(tree);
        
        if (dist > 2) {
            state = State.WALKING_TO_TREE;
            targetTree = tree;
            boolean walked = api.walkTo(tree.getX(), tree.getY());
            if (!walked) {
                state = State.IDLE;
                return random(100, 300);
            }
            return 10;
        }
        
        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;
        
        return 10;
    }
    
    private int bankLogs() {
        if (!api.isBankOpen()) {
            api.openBank();
            consecutiveBankFailures++;
            if (consecutiveBankFailures > 3) {
                gameMessage("@red@Bank command failed, continuing to chop...");
                consecutiveBankFailures = 0;
                state = State.IDLE;
                return random(500, 1000);
            }
            return random(800, 1200);
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
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public State getState() {
        return state;
    }
}
