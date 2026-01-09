package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {

    private int[] treeIds = { 0, 1, 70, 306, 307, 308, 309, 310 };
    private int[] logIds = { 14, 632, 633, 634, 635, 636 };

    private enum State {
        CHOPPING, WALKING, BANKING
    }

    private State state = State.CHOPPING;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    private int treesChopped = 0;

    private int bankDepositedCount = 0;
    private int bankCurrentLogIndex = 0;

    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;

    private static final int SEERS_MAPLE_MIN_X = 480;
    private static final int SEERS_MAPLE_MAX_X = 550;
    private static final int SEERS_MAPLE_MIN_Y = 420;
    private static final int SEERS_MAPLE_MAX_Y = 480;
    private static final int SEERS_VILLAGE_BANK_X = 490;
    private static final int SEERS_VILLAGE_BANK_Y = 470;

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
        treesChopped = 0;
        state = State.CHOPPING;
        if (areaMinX == null) {
            areaMinX = SEERS_MAPLE_MIN_X;
            areaMaxX = SEERS_MAPLE_MAX_X;
            areaMinY = SEERS_MAPLE_MIN_Y;
            areaMaxY = SEERS_MAPLE_MAX_Y;
            gameMessage("Woodcutting bot started! Chopping all tree types.");
        } else {
            gameMessage("Woodcutting bot started! Area: " + areaMinX + "-" + areaMaxX + ", " + areaMinY + "-" + areaMaxY);
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
    
    @Override
    public int loop() {
        if (api.isBusy() || api.isMoving()) {
            return 50;
        }

        if (api.isInventoryFull() || bankDepositedCount > 0 || bankCurrentLogIndex > 0) {
            return bankLogs();
        }

        return chopTree();
    }
    
    private int chopTree() {
        GameObject tree = findTreeInArea();

        if (tree == null || tree.isRemoved()) {
            targetTree = null;
            gameMessage("No tree found, searching...");
            return searchForTree();
        }

        if (!isInArea(tree.getX(), tree.getY())) {
            targetTree = null;
            gameMessage("Tree outside area, searching...");
            return searchForTree();
        }

        int dist = api.distanceTo(tree);
        gameMessage("Tree found at " + tree.getX() + "," + tree.getY() + " (dist=" + dist + ")");

        if (dist > 2) {
            state = State.WALKING;
            targetTree = tree;
            api.walkTo(tree.getX(), tree.getY());
            return random(300, 500);
        }

        if (tree == null || tree.isRemoved()) {
            gameMessage("Tree gone before chop, searching...");
            targetTree = null;
            return searchForTree();
        }

        gameMessage("Chopping tree at " + tree.getX() + "," + tree.getY() + "!");
        state = State.CHOPPING;
        treesChopped++;
        
        api.walkTo(tree.getX(), tree.getY());
        
        targetTree = null;
        return random(100, 200);
    }

    private int searchForTree() {
        int currentX = api.getX();
        int currentY = api.getY();
        gameMessage("At " + currentX + "," + currentY + ", searching for tree...");

        if (currentX < areaMinX || currentX > areaMaxX ||
            currentY < areaMinY || currentY > areaMaxY) {
            gameMessage("Outside area, walking to start...");
            api.walkTo(areaMinX.intValue(), areaMinY.intValue());
            return random(300, 500);
        }
        
        int rangeX = areaMaxX - areaMinX;
        int rangeY = areaMaxY - areaMinY;
        
        int scanX = areaMinX + random(0, rangeX);
        int scanY = areaMinY + random(0, rangeY);
        
        gameMessage("Walking to scan position " + scanX + "," + scanY + "...");
        api.walkTo(scanX, scanY);
        return random(300, 500);
    }

    private GameObject findTreeInArea() {
        if (areaMinX != null && areaMaxX != null && areaMinY != null && areaMaxY != null) {
            return api.getNearestObjectInArea(treeIds, areaMinX.intValue(), areaMaxX.intValue(), areaMinY.intValue(), areaMaxY.intValue());
        }
        return api.getNearestObjectInLocalArea(treeIds, 200);
    }

    private int bankLogs() {
        int currentX = api.getX();
        int currentY = api.getY();

        if (currentX < SEERS_VILLAGE_BANK_X - 15 || currentX > SEERS_VILLAGE_BANK_X + 15 ||
            currentY < SEERS_VILLAGE_BANK_Y - 15 || currentY > SEERS_VILLAGE_BANK_Y + 15) {
            api.walkTo(SEERS_VILLAGE_BANK_X, SEERS_VILLAGE_BANK_Y);
            return random(300, 500);
        }

        if (!api.isBankOpen()) {
            api.openBank();
            return random(100, 200);
        }

        for (int id : logIds) {
            int count = api.getInventoryCount(id);
            if (count > 0) {
                api.depositItem(id, count);
                logsChopped += count;
                gameMessage("Banked " + count + " logs. Total: " + logsChopped);
                api.closeBank();
                bankDepositedCount = 0;
                bankCurrentLogIndex = 0;
                return random(100, 200);
            }
        }

        api.closeBank();
        bankDepositedCount = 0;
        bankCurrentLogIndex = 0;
        return random(100, 200);
    }
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public int getTreesChopped() {
        return treesChopped;
    }
    
    public State getState() {
        return state;
    }
}
