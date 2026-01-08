package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {

    private int[] treeIds = { 0, 1, 70 };
    private int[] logIds = { 14 };

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

        if (api.isInventoryFull() && bankDepositedCount == 0 && bankCurrentLogIndex == 0) {
            bankDepositedCount = 0;
            bankCurrentLogIndex = 0;
            return bankLogs();
        }

        if (bankDepositedCount > 0 || bankCurrentLogIndex > 0) {
            return bankLogs();
        }

        if (api.isInventoryFull()) {
            bankDepositedCount = 0;
            bankCurrentLogIndex = 0;
            return bankLogs();
        }

        return chopTree();
    }
    
    private int chopTree() {
        GameObject tree = findTreeInArea();

        if (tree == null || tree.isRemoved()) {
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
        if (areaMinX == null || areaMaxX == null || areaMinY == null || areaMaxY == null) {
            int[] offsets = { -20, -15, -10, 10, 15, 20 };
            int newX = api.getX() + offsets[random(0, offsets.length - 1)];
            int newY = api.getY() + offsets[random(0, offsets.length - 1)];
            api.walkTo(newX, newY);
            return random(500, 800);
        }

        int newX = areaMinX + random(0, areaMaxX - areaMinX);
        int newY = areaMinY + random(0, areaMaxY - areaMinY);
        api.walkTo(newX, newY);
        return random(600, 1000);
    }

    private GameObject findTreeInArea() {
        if (areaMinX != null && areaMaxX != null && areaMinY != null && areaMaxY != null) {
            return api.getNearestObjectInArea(treeIds, areaMinX.intValue(), areaMaxX.intValue(), areaMinY.intValue(), areaMaxY.intValue());
        }
        return api.getNearestObjectInLocalArea(treeIds, 50);
    }
    private int bankLogs() {
        if (!api.isBankOpen()) {
            api.openBank();
            return random(800, 1200);
        }

        int totalDepositedThisCall = 0;
        for (int i = bankCurrentLogIndex; i < logIds.length; i++) {
            int logId = logIds[i];
            int count = api.getInventoryCount(logId);

            while (count > 0 && api.getInventoryCount(logId) > 0) {
                api.depositItem(logId, 1);
                logsChopped++;
                count--;
                totalDepositedThisCall++;
                if (api.isBusy()) {
                    return random(100, 200);
                }
            }

            if (api.getInventoryCount(logId) == 0) {
                bankCurrentLogIndex = i + 1;
            }
        }

        boolean allLogsDeposited = true;
        for (int id : logIds) {
            if (api.getInventoryCount(id) > 0) {
                allLogsDeposited = false;
                break;
            }
        }

        if (allLogsDeposited) {
            api.closeBank();
            bankCurrentLogIndex = 0;
            return random(300, 500);
        }

        return random(100, 200);
    }
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public State getState() {
        return state;
    }
}
