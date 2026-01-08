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

    private int treeIndex = 0;
    private long lastSearchTime = 0;

    private static final int[][] SEERS_TREES = {
        {522, 458},
        {525, 455},
        {520, 452},
        {530, 450},
        {535, 455},
        {540, 458},
        {538, 462},
        {532, 465},
        {526, 468},
        {520, 465},
        {515, 460},
        {510, 455},
        {505, 450},
        {500, 445},
        {495, 450},
        {490, 455},
        {485, 460},
        {490, 465},
        {495, 470},
        {500, 475}
    };

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
        treeIndex = 0;
        state = State.CHOPPING;
        if (areaMinX == null) {
            areaMinX = SEERS_MAPLE_MIN_X;
            areaMaxX = SEERS_MAPLE_MAX_X;
            areaMinY = SEERS_MAPLE_MIN_Y;
            areaMaxY = SEERS_MAPLE_MAX_Y;
            gameMessage("Woodcutting bot started! Fixed tree order for Seers Village.");
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
        if (treeIndex >= SEERS_TREES.length) {
            treeIndex = 0;
        }

        int targetX = SEERS_TREES[treeIndex][0];
        int targetY = SEERS_TREES[treeIndex][1];

        GameObject tree = findTreeAtLocation(targetX, targetY);

        if (tree == null || tree.isRemoved()) {
            treeIndex++;
            return searchForTree();
        }

        int dist = api.distanceTo(tree);

        if (dist > 1) {
            state = State.WALKING;
            targetTree = tree;
            api.walkTo(targetX, targetY);
            return random(300, 500);
        }

        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;
        gameMessage("Chopping tree at " + targetX + ", " + targetY);

        treeIndex++;
        return random(100, 200);
    }

    private GameObject findTreeAtLocation(int x, int y) {
        return api.getNearestObjectInArea(treeIds, x - 1, x + 1, y - 1, y + 1);
    }

    private int searchForTree() {
        long now = System.currentTimeMillis();
        if (now - lastSearchTime < 50) {
            return 50;
        }
        lastSearchTime = now;
        
        if (treeIndex >= SEERS_TREES.length) {
            treeIndex = 0;
        }

        int targetX = SEERS_TREES[treeIndex][0];
        int targetY = SEERS_TREES[treeIndex][1];

        api.walkTo(targetX, targetY);
        return random(300, 500);
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
                treeIndex = 0;
                return random(100, 200);
            }
        }

        api.closeBank();
        bankDepositedCount = 0;
        bankCurrentLogIndex = 0;
        treeIndex = 0;
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
