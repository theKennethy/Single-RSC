package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {

    private int[] treeIds;
    private int[] logIds;
    private String treeType;

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

    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;

    private int[][] treeLocations;

    private static final int[][] NORMAL_TREES = {
        {522, 458}, {525, 455}, {520, 452}, {530, 450}, {535, 455},
        {540, 458}, {538, 462}, {532, 465}, {526, 468}, {520, 465},
        {515, 460}, {510, 455}, {505, 450}, {500, 445}, {495, 450},
        {490, 455}, {485, 460}, {490, 465}, {495, 470}, {500, 475}
    };

    private static final int[][] OAK_TREES = {
        {480, 450}, {485, 455}, {490, 450}, {495, 455}, {500, 450},
        {505, 455}, {510, 450}, {515, 455}, {520, 450}, {525, 455}
    };

    private static final int[][] WILLOW_TREES = {
        {520, 460}, {525, 465}, {530, 460}, {535, 465}, {540, 460},
        {545, 465}, {550, 460}, {555, 465}, {560, 460}, {565, 465}
    };

    private static final int[][] MAPLE_TREES = {
        {522, 458}, {525, 455}, {520, 452}, {530, 450}, {535, 455},
        {540, 458}, {538, 462}, {532, 465}, {526, 468}, {520, 465},
        {515, 460}, {510, 455}, {505, 450}, {500, 445}, {495, 450},
        {490, 455}, {485, 460}, {490, 465}, {495, 470}, {500, 475}
    };

    private static final int[][] YEW_TREES = {
        {480, 450}, {485, 445}, {490, 450}, {495, 445}, {500, 450},
        {505, 445}, {510, 450}, {515, 445}, {520, 450}, {525, 445}
    };

    private static final int[][] MAGIC_TREES = {
        {480, 420}, {485, 425}, {490, 420}, {495, 425}, {500, 420},
        {505, 425}, {510, 420}, {515, 425}, {520, 420}, {525, 425}
    };

    private static final int SEERS_VILLAGE_BANK_X = 490;
    private static final int SEERS_VILLAGE_BANK_Y = 470;

    public WoodcuttingBot() {
        super("Woodcutting Bot");
        this.treeType = "normal";
        this.treeIds = new int[] { 0, 1, 70 };
        this.logIds = new int[] { 14 };
        this.treeLocations = NORMAL_TREES;
    }
    
    public WoodcuttingBot(String treeType) {
        super("Woodcutting Bot");
        this.treeType = treeType;
        setTreeType(treeType);
    }
    
    public WoodcuttingBot(int[] treeIds, int[] logIds) {
        super("Woodcutting Bot");
        this.treeType = "normal";
        this.treeIds = treeIds;
        this.logIds = logIds;
        this.treeLocations = NORMAL_TREES;
    }
    
    private void setTreeType(String type) {
        switch (type.toLowerCase()) {
            case "oak":
                treeIds = new int[] { 306 };
                logIds = new int[] { 632 };
                treeLocations = OAK_TREES;
                break;
            case "willow":
                treeIds = new int[] { 307 };
                logIds = new int[] { 633 };
                treeLocations = WILLOW_TREES;
                break;
            case "maple":
                treeIds = new int[] { 308 };
                logIds = new int[] { 634 };
                treeLocations = MAPLE_TREES;
                break;
            case "yew":
                treeIds = new int[] { 309 };
                logIds = new int[] { 635 };
                treeLocations = YEW_TREES;
                break;
            case "magic":
                treeIds = new int[] { 310 };
                logIds = new int[] { 636 };
                treeLocations = MAGIC_TREES;
                break;
            case "normal":
            default:
                treeIds = new int[] { 0, 1, 70 };
                logIds = new int[] { 14 };
                treeLocations = NORMAL_TREES;
                type = "normal";
                break;
        }
        this.treeType = type;
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
    
    @Override
    public void onStart() {
        super.onStart();
        logsChopped = 0;
        treesChopped = 0;
        treeIndex = 0;
        state = State.CHOPPING;
        gameMessage("Woodcutting bot started! " + treeType + " trees - fixed pattern.");
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
        if (treeIndex >= treeLocations.length) {
            treeIndex = 0;
        }

        int targetX = treeLocations[treeIndex][0];
        int targetY = treeLocations[treeIndex][1];

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
        
        if (treeIndex >= treeLocations.length) {
            treeIndex = 0;
        }

        int targetX = treeLocations[treeIndex][0];
        int targetY = treeLocations[treeIndex][1];

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
    
    public String getTreeType() {
        return treeType;
    }
}
