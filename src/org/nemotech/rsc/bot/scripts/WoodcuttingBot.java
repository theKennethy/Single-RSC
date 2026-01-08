package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {

    private int[] treeIds = { 0, 1, 70 };
    private int[] logIds = { 14 };

    private enum State {
        CHOPPING, WALKING, BANKING, SEARCHING
    }

    private State state = State.CHOPPING;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    private int treesChopped = 0;

    private int bankDepositedCount = 0;
    private int bankCurrentLogIndex = 0;
    private int lastSearchX = -1;
    private int lastSearchY = -1;
    private long lastSearchTime = 0;

    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;

    private static final int DEFAULT_AREA_SIZE = 200;
    private static final int SEERS_VILLAGE_X = 500;
    private static final int SEERS_VILLAGE_Y = 450;
    private static final int SEARCH_RADIUS = 60;

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

        if (state == State.SEARCHING) {
            long timeSinceSearch = System.currentTimeMillis() - lastSearchTime;
            if (timeSinceSearch < 1000) {
                return 100;
            }
            state = State.CHOPPING;
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
        state = State.SEARCHING;
        lastSearchX = api.getX();
        lastSearchY = api.getY();
        lastSearchTime = System.currentTimeMillis();

        int currentX = api.getX();
        int currentY = api.getY();
        int distFromSeers = api.distanceTo(SEERS_VILLAGE_X, SEERS_VILLAGE_Y);

        if (distFromSeers > SEARCH_RADIUS) {
            int walkX = SEERS_VILLAGE_X + random(-20, 20);
            int walkY = SEERS_VILLAGE_Y + random(-20, 20);
            api.walkTo(walkX, walkY);
            return random(1500, 2000);
        }

        if (areaMinX == null || areaMaxX == null || areaMinY == null || areaMaxY == null) {
            int[] offsets = { -50, -40, -30, -20, 20, 30, 40, 50 };
            int newX = api.getX() + offsets[random(0, offsets.length - 1)];
            int newY = api.getY() + offsets[random(0, offsets.length - 1)];
            api.walkTo(newX, newY);
            return random(1000, 1500);
        }

        int newX = areaMinX + random(0, areaMaxX - areaMinX);
        int newY = areaMinY + random(0, areaMaxY - areaMinY);
        api.walkTo(newX, newY);
        return random(1000, 1500);
    }

    private GameObject findTreeInArea() {
        if (areaMinX != null && areaMaxX != null && areaMinY != null && areaMaxY != null) {
            return api.getNearestObjectInArea(treeIds, areaMinX.intValue(), areaMaxX.intValue(), areaMinY.intValue(), areaMaxY.intValue());
        }
        return api.getNearestObjectInLocalArea(treeIds, 200);
    }
    private int bankLogs() {
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
                return random(200, 400);
            }
        }

        api.closeBank();
        return random(200, 400);
    }
    
    public int getLogsChopped() {
        return logsChopped;
    }
    
    public State getState() {
        return state;
    }
}
