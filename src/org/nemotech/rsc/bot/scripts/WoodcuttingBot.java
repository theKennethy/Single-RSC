package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {

    private int[] treeIds = { 0, 1, 70, 306, 307, 308, 309, 310 };
    private int[] logIds = { 14, 632, 633, 634, 635, 636 };

    private enum State {
        CHOPPING, WALKING, BANKING, WAITING_RESPAWN
    }

    private State state = State.CHOPPING;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    private int treesChopped = 0;

    private int bankDepositedCount = 0;
    private int bankCurrentLogIndex = 0;

    private int roundX = 0;
    private int roundY = 0;
    private long respawnStartTime = 0;
    private long lastSearchTime = 0;

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
    private static final int RESPAWN_WAIT_TIME = 5000;

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
        roundX = 0;
        roundY = 0;
        state = State.CHOPPING;
        if (areaMinX == null) {
            areaMinX = SEERS_MAPLE_MIN_X;
            areaMaxX = SEERS_MAPLE_MAX_X;
            areaMinY = SEERS_MAPLE_MIN_Y;
            areaMaxY = SEERS_MAPLE_MAX_Y;
            gameMessage("Woodcutting bot started! Fixed rounds pattern for all trees.");
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

        if (state == State.WAITING_RESPAWN) {
            long timeWaiting = System.currentTimeMillis() - respawnStartTime;
            if (timeWaiting < RESPAWN_WAIT_TIME) {
                return 500;
            }
            state = State.CHOPPING;
            targetTree = null;
        }

        if (api.isInventoryFull() || bankDepositedCount > 0 || bankCurrentLogIndex > 0) {
            return bankLogs();
        }

        return chopTree();
    }
    
    private int chopTree() {
        GameObject tree = findTreeInArea();

        if (tree == null || tree.isRemoved()) {
            if (targetTree != null && !targetTree.isRemoved()) {
                state = State.WAITING_RESPAWN;
                respawnStartTime = System.currentTimeMillis();
                targetTree = null;
                return 500;
            }
            targetTree = null;
            return searchForTree();
        }

        if (!isInArea(tree.getX(), tree.getY())) {
            targetTree = null;
            return searchForTree();
        }

        int dist = api.distanceTo(tree);

        if (dist > 1) {
            state = State.WALKING;
            targetTree = tree;
            api.walkTo(tree.getX(), tree.getY());
            return random(500, 800);
        }

        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;

        targetTree = null;
        return random(200, 400);
    }

    private int searchForTree() {
        long now = System.currentTimeMillis();
        if (now - lastSearchTime < 1000) {
            return 100;
        }
        lastSearchTime = now;
        
        int currentX = api.getX();
        int currentY = api.getY();

        if (currentX < areaMinX || currentX > areaMaxX ||
            currentY < areaMinY || currentY > areaMaxY) {
            api.walkTo(areaMinX, areaMinY);
            roundX = 0;
            roundY = 0;
            return random(1500, 2000);
        }

        int width = areaMaxX - areaMinX;
        int height = areaMaxY - areaMinY;

        int stepX = Math.max(1, width / 4);
        int stepY = Math.max(1, height / 4);

        int walkX = areaMinX + (roundX * stepX);
        int walkY = areaMinY + (roundY * stepY);

        api.walkTo(walkX, walkY);

        roundX++;
        if (roundX > 4) {
            roundX = 0;
            roundY++;
            if (roundY > 4) {
                roundY = 0;
            }
        }

        return random(1000, 1500);
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
            return random(1500, 2000);
        }

        if (!api.isBankOpen()) {
            api.openBank();
            return random(500, 800);
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
                roundX = 0;
                roundY = 0;
                return random(200, 400);
            }
        }

        api.closeBank();
        bankDepositedCount = 0;
        bankCurrentLogIndex = 0;
        roundX = 0;
        roundY = 0;
        return random(200, 400);
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
