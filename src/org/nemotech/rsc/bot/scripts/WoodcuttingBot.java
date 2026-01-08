package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

public class WoodcuttingBot extends Bot {

    private int[] treeIds;
    private int[] logIds;
    private String treeType;
    private String location;

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

    private int[][] treeLocations;

    public Integer areaMinX = null;
    public Integer areaMaxX = null;
    public Integer areaMinY = null;
    public Integer areaMaxY = null;

    private static final int BANK_X_DEFAULT = 490;
    private static final int BANK_Y_DEFAULT = 470;

    private static final int[][] NORMAL_TREES_SEERS = {
        {522, 458}, {525, 455}, {520, 452}, {530, 450}, {535, 455},
        {540, 458}, {538, 462}, {532, 465}, {526, 468}, {520, 465},
        {515, 460}, {510, 455}, {505, 450}, {500, 445}, {495, 450},
        {490, 455}, {485, 460}, {490, 465}, {495, 470}, {500, 475}
    };

    private static final int[][] OAK_TREES_SEERS = {
        {480, 450}, {485, 455}, {490, 450}, {495, 455}, {500, 450},
        {505, 455}, {510, 450}, {515, 455}, {520, 450}, {525, 455}
    };

    private static final int[][] WILLOW_TREES_SEERS = {
        {520, 460}, {525, 465}, {530, 460}, {535, 465}, {540, 460},
        {545, 465}, {550, 460}, {555, 465}, {560, 460}, {565, 465}
    };

    private static final int[][] MAPLE_TREES_SEERS = {
        {522, 458}, {525, 455}, {520, 452}, {530, 450}, {535, 455},
        {540, 458}, {538, 462}, {532, 465}, {526, 468}, {520, 465},
        {515, 460}, {510, 455}, {505, 450}, {500, 445}, {495, 450},
        {490, 455}, {485, 460}, {490, 465}, {495, 470}, {500, 475}
    };

    private static final int[][] YEW_TREES_SEERS = {
        {480, 450}, {485, 445}, {490, 450}, {495, 445}, {500, 450},
        {505, 445}, {510, 450}, {515, 445}, {520, 450}, {525, 445}
    };

    private static final int[][] MAGIC_TREES_SEERS = {
        {480, 420}, {485, 425}, {490, 420}, {495, 425}, {500, 420},
        {505, 425}, {510, 420}, {515, 425}, {520, 420}, {525, 425}
    };

    private static final int[][] NORMAL_TREES_VARROCK = {
        {120, 500}, {125, 505}, {130, 500}, {135, 505}, {140, 500},
        {145, 505}, {150, 500}, {155, 505}, {120, 510}, {125, 515}
    };

    private static final int[][] OAK_TREES_VARROCK = {
        {100, 480}, {105, 485}, {110, 480}, {115, 485}, {120, 480},
        {125, 485}, {130, 480}, {135, 485}, {140, 480}, {145, 485}
    };

    private static final int[][] WILLOW_TREES_VARROCK = {
        {160, 500}, {165, 505}, {170, 500}, {175, 505}, {180, 500},
        {185, 505}, {190, 500}, {195, 505}, {200, 500}, {205, 505}
    };

    private static final int[][] MAPLE_TREES_VARROCK = {
        {150, 520}, {155, 525}, {160, 520}, {165, 525}, {170, 520},
        {175, 525}, {180, 520}, {185, 525}, {190, 520}, {195, 525}
    };

    private static final int[][] YEW_TREES_VARROCK = {
        {300, 550}, {305, 555}, {310, 550}, {315, 555}, {320, 550},
        {325, 555}, {330, 550}, {335, 555}, {340, 550}, {345, 555}
    };

    private static final int[][] MAGIC_TREES_VARROCK = {
        {400, 600}, {405, 605}, {410, 600}, {415, 605}, {420, 600},
        {425, 605}, {430, 600}, {435, 605}, {440, 600}, {445, 605}
    };

    private static final int[][] NORMAL_TREES_FALADOR = {
        {300, 540}, {305, 545}, {310, 540}, {315, 545}, {320, 540},
        {325, 545}, {330, 540}, {335, 545}, {340, 540}, {345, 545}
    };

    private static final int[][] OAK_TREES_FALADOR = {
        {280, 530}, {285, 535}, {290, 530}, {295, 535}, {300, 530},
        {305, 535}, {310, 530}, {315, 535}, {320, 530}, {325, 535}
    };

    private static final int[][] WILLOW_TREES_FALADOR = {
        {350, 560}, {355, 565}, {360, 560}, {365, 565}, {370, 560},
        {375, 565}, {380, 560}, {385, 565}, {390, 560}, {395, 565}
    };

    private static final int[][] MAPLE_TREES_FALADOR = {
        {320, 550}, {325, 555}, {330, 550}, {335, 555}, {340, 550},
        {345, 555}, {350, 550}, {355, 555}, {360, 550}, {365, 555}
    };

    private static final int[][] YEW_TREES_FALADOR = {
        {400, 600}, {405, 605}, {410, 600}, {415, 605}, {420, 600},
        {425, 605}, {430, 600}, {435, 605}, {440, 600}, {445, 605}
    };

    private static final int[][] MAGIC_TREES_FALADOR = {
        {450, 650}, {455, 655}, {460, 650}, {465, 655}, {470, 650},
        {475, 655}, {480, 650}, {485, 655}, {490, 650}, {495, 655}
    };

    private static final int[][] NORMAL_TREES_DRAYNOR = {
        {210, 620}, {215, 625}, {220, 620}, {225, 625}, {230, 620},
        {235, 625}, {240, 620}, {245, 625}, {210, 630}, {215, 635}
    };

    private static final int[][] OAK_TREES_DRAYNOR = {
        {200, 610}, {205, 615}, {210, 610}, {215, 615}, {220, 610},
        {225, 615}, {230, 610}, {235, 615}, {240, 610}, {245, 615}
    };

    private static final int[][] WILLOW_TREES_DRAYNOR = {
        {190, 640}, {195, 645}, {200, 640}, {205, 645}, {210, 640},
        {215, 645}, {220, 640}, {225, 645}, {230, 640}, {235, 645}
    };

    private static final int[][] MAPLE_TREES_DRAYNOR = {
        {250, 630}, {255, 635}, {260, 630}, {265, 635}, {270, 630},
        {275, 635}, {280, 630}, {285, 635}, {290, 630}, {295, 635}
    };

    private static final int[][] YEW_TREES_DRAYNOR = {
        {350, 650}, {355, 655}, {360, 650}, {365, 655}, {370, 650},
        {375, 655}, {380, 650}, {385, 655}, {390, 650}, {395, 655}
    };

    private static final int[][] MAGIC_TREES_DRAYNOR = {
        {400, 700}, {405, 705}, {410, 700}, {415, 705}, {420, 700},
        {425, 705}, {430, 700}, {435, 705}, {440, 700}, {445, 705}
    };

    private static final int[][] NORMAL_TREES_EDGEVILLE = {
        {210, 440}, {215, 445}, {220, 440}, {225, 445}, {230, 440},
        {235, 445}, {240, 440}, {245, 445}, {210, 450}, {215, 455}
    };

    private static final int[][] OAK_TREES_EDGEVILLE = {
        {200, 430}, {205, 435}, {210, 430}, {215, 435}, {220, 430},
        {225, 435}, {230, 430}, {235, 435}, {240, 430}, {245, 435}
    };

    private static final int[][] WILLOW_TREES_EDGEVILLE = {
        {250, 460}, {255, 465}, {260, 460}, {265, 465}, {270, 460},
        {275, 465}, {280, 460}, {285, 465}, {290, 460}, {295, 465}
    };

    private static final int[][] MAPLE_TREES_EDGEVILLE = {
        {220, 450}, {225, 455}, {230, 450}, {235, 455}, {240, 450},
        {245, 455}, {250, 450}, {255, 455}, {260, 450}, {265, 455}
    };

    private static final int[][] YEW_TREES_EDGEVILLE = {
        {300, 500}, {305, 505}, {310, 500}, {315, 505}, {320, 500},
        {325, 505}, {330, 500}, {335, 505}, {340, 500}, {345, 505}
    };

    private static final int[][] MAGIC_TREES_EDGEVILLE = {
        {400, 550}, {405, 555}, {410, 550}, {415, 555}, {420, 550},
        {425, 555}, {430, 550}, {435, 555}, {440, 550}, {445, 555}
    };

    public WoodcuttingBot() {
        super("Woodcutting Bot");
        this.treeType = "normal";
        this.location = "seers";
        this.treeIds = new int[] { 0, 1, 70 };
        this.logIds = new int[] { 14 };
        this.treeLocations = NORMAL_TREES_SEERS;
    }
    
    public WoodcuttingBot(String treeType, String location) {
        super("Woodcutting Bot");
        this.treeType = treeType;
        this.location = location != null ? location : "seers";
        setTreeAndLocation(this.treeType, this.location);
    }
    
    private void setTreeAndLocation(String type, String loc) {
        int[][][] locationData = getLocationData(loc);
        int[][] trees = locationData[0];
        int[] ids = locationData[1][0];
        int[] logs = locationData[1][1];
        
        this.treeLocations = trees;
        this.treeIds = ids;
        this.logIds = logs;
    }
    
    private int[][][] getLocationData(String loc) {
        switch (loc.toLowerCase()) {
            case "varrock":
                return new int[][][] {
                    getTreesForType(treeType, "VARROCK"),
                    new int[][] { getTreeIdsForType(treeType), getLogIdsForType(treeType) }
                };
            case "falador":
                return new int[][][] {
                    getTreesForType(treeType, "FALADOR"),
                    new int[][] { getTreeIdsForType(treeType), getLogIdsForType(treeType) }
                };
            case "draynor":
                return new int[][][] {
                    getTreesForType(treeType, "DRAYNOR"),
                    new int[][] { getTreeIdsForType(treeType), getLogIdsForType(treeType) }
                };
            case "edgeville":
                return new int[][][] {
                    getTreesForType(treeType, "EDGEVILLE"),
                    new int[][] { getTreeIdsForType(treeType), getLogIdsForType(treeType) }
                };
            case "seers":
            case "seersvillage":
            default:
                return new int[][][] {
                    getTreesForType(treeType, "SEERS"),
                    new int[][] { getTreeIdsForType(treeType), getLogIdsForType(treeType) }
                };
        }
    }
    
    private int[][] getTreesForType(String type, String loc) {
        switch (type.toLowerCase()) {
            case "oak":
                switch (loc) {
                    case "VARROCK": return OAK_TREES_VARROCK;
                    case "FALADOR": return OAK_TREES_FALADOR;
                    case "DRAYNOR": return OAK_TREES_DRAYNOR;
                    case "EDGEVILLE": return OAK_TREES_EDGEVILLE;
                    default: return OAK_TREES_SEERS;
                }
            case "willow":
                switch (loc) {
                    case "VARROCK": return WILLOW_TREES_VARROCK;
                    case "FALADOR": return WILLOW_TREES_FALADOR;
                    case "DRAYNOR": return WILLOW_TREES_DRAYNOR;
                    case "EDGEVILLE": return WILLOW_TREES_EDGEVILLE;
                    default: return WILLOW_TREES_SEERS;
                }
            case "maple":
                switch (loc) {
                    case "VARROCK": return MAPLE_TREES_VARROCK;
                    case "FALADOR": return MAPLE_TREES_FALADOR;
                    case "DRAYNOR": return MAPLE_TREES_DRAYNOR;
                    case "EDGEVILLE": return MAPLE_TREES_EDGEVILLE;
                    default: return MAPLE_TREES_SEERS;
                }
            case "yew":
                switch (loc) {
                    case "VARROCK": return YEW_TREES_VARROCK;
                    case "FALADOR": return YEW_TREES_FALADOR;
                    case "DRAYNOR": return YEW_TREES_DRAYNOR;
                    case "EDGEVILLE": return YEW_TREES_EDGEVILLE;
                    default: return YEW_TREES_SEERS;
                }
            case "magic":
                switch (loc) {
                    case "VARROCK": return MAGIC_TREES_VARROCK;
                    case "FALADOR": return MAGIC_TREES_FALADOR;
                    case "DRAYNOR": return MAGIC_TREES_DRAYNOR;
                    case "EDGEVILLE": return MAGIC_TREES_EDGEVILLE;
                    default: return MAGIC_TREES_SEERS;
                }
            case "normal":
            default:
                switch (loc) {
                    case "VARROCK": return NORMAL_TREES_VARROCK;
                    case "FALADOR": return NORMAL_TREES_FALADOR;
                    case "DRAYNOR": return NORMAL_TREES_DRAYNOR;
                    case "EDGEVILLE": return NORMAL_TREES_EDGEVILLE;
                    default: return NORMAL_TREES_SEERS;
                }
        }
    }
    
    private int[] getTreeIdsForType(String type) {
        switch (type.toLowerCase()) {
            case "oak": return new int[] { 306 };
            case "willow": return new int[] { 307 };
            case "maple": return new int[] { 308 };
            case "yew": return new int[] { 309 };
            case "magic": return new int[] { 310 };
            case "normal": default: return new int[] { 0, 1, 70 };
        }
    }
    
    private int[] getLogIdsForType(String type) {
        switch (type.toLowerCase()) {
            case "oak": return new int[] { 632 };
            case "willow": return new int[] { 633 };
            case "maple": return new int[] { 634 };
            case "yew": return new int[] { 635 };
            case "magic": return new int[] { 636 };
            case "normal": default: return new int[] { 14 };
        }
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
        gameMessage("Woodcutting bot started! " + treeType + " trees at " + location + " - fixed pattern.");
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

        if (currentX < BANK_X_DEFAULT - 15 || currentX > BANK_X_DEFAULT + 15 ||
            currentY < BANK_Y_DEFAULT - 15 || currentY > BANK_Y_DEFAULT + 15) {
            api.walkTo(BANK_X_DEFAULT, BANK_Y_DEFAULT);
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
    
    public String getLocation() {
        return location;
    }
}
