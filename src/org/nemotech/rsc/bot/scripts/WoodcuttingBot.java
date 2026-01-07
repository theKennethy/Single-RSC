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
        autoDetectArea();
    }
    
    public WoodcuttingBot(int[] treeIds, int[] logIds) {
        super("Woodcutting Bot");
        this.treeIds = treeIds;
        this.logIds = logIds;
        autoDetectArea();
    }
    
    private void autoDetectArea() {
        int x = api.getX();
        int y = api.getY();
        
        if (x >= 370 && x <= 470 && y >= 530 && y <= 680) {
            setAreaByLocation("seers");
        } else if (x >= 280 && x <= 340 && y >= 510 && y <= 580) {
            setAreaByLocation("falador");
        } else if (x >= 100 && x <= 160 && y >= 480 && y <= 550) {
            setAreaByLocation("varrock");
        } else if (x >= 190 && x <= 240 && y >= 600 && y <= 660) {
            setAreaByLocation("draynor");
        } else if (x >= 250 && x <= 290 && y >= 620 && y <= 670) {
            setAreaByLocation("portsarim");
        } else if (x >= 350 && x <= 400 && y >= 660 && y <= 710) {
            setAreaByLocation("karamja");
        } else if (x >= 70 && x <= 120 && y >= 660 && y <= 720) {
            setAreaByLocation("alkharid");
        } else if (x >= 100 && x <= 160 && y >= 620 && y <= 680) {
            setAreaByLocation("lumbridge");
        } else if (x >= 190 && x <= 250 && y >= 420 && y <= 480) {
            setAreaByLocation("edgeville");
        } else if (x >= 350 && x <= 400 && y >= 470 && y <= 530) {
            setAreaByLocation("taverly");
        } else if (x >= 210 && x <= 260 && y >= 490 && y <= 540) {
            setAreaByLocation("barbarian");
        } else if (x >= 300 && x <= 350 && y >= 640 && y <= 690) {
            setAreaByLocation("rimmington");
        } else if (x >= 420 && x <= 470 && y >= 480 && y <= 530) {
            setAreaByLocation("catherby");
        } else if (x >= 520 && x <= 580 && y >= 560 && y <= 620) {
            setAreaByLocation("ardougne");
        } else if (x >= 560 && x <= 620 && y >= 720 && y <= 780) {
            setAreaByLocation("yanille");
        } else if (x >= 100 && x <= 160 && y >= 3490 && y <= 3550) {
            setAreaByLocation("lostcity");
        } else if (x >= 680 && x <= 740 && y >= 500 && y <= 560) {
            setAreaByLocation("gnome");
        } else if (x >= 190 && x <= 250 && y >= 720 && y <= 770) {
            setAreaByLocation("tutorial");
        } else {
            setAreaByLocation("seers");
        }
    }
    
    public void setAreaByLocation(String location) {
        switch (location.toLowerCase()) {
            case "varrock":
                areaMinX = 100; areaMaxX = 160; areaMinY = 480; areaMaxY = 550; break;
            case "falador":
                areaMinX = 280; areaMaxX = 340; areaMinY = 510; areaMaxY = 580; break;
            case "draynor":
                areaMinX = 190; areaMaxX = 240; areaMinY = 600; areaMaxY = 660; break;
            case "portsarim":
                areaMinX = 250; areaMaxX = 290; areaMinY = 620; areaMaxY = 670; break;
            case "karamja":
                areaMinX = 350; areaMaxX = 400; areaMinY = 660; areaMaxY = 710; break;
            case "alkharid":
                areaMinX = 70; areaMaxX = 120; areaMinY = 660; areaMaxY = 720; break;
            case "lumbridge":
                areaMinX = 100; areaMaxX = 160; areaMinY = 620; areaMaxY = 680; break;
            case "edgeville":
                areaMinX = 190; areaMaxX = 250; areaMinY = 420; areaMaxY = 480; break;
            case "taverly":
                areaMinX = 350; areaMaxX = 400; areaMinY = 470; areaMaxY = 530; break;
            case "seers":
            case "seersvillage":
                areaMinX = 370; areaMaxX = 470; areaMinY = 530; areaMaxY = 680; break;
            case "barbarian":
                areaMinX = 210; areaMaxX = 260; areaMinY = 490; areaMaxY = 540; break;
            case "rimmington":
                areaMinX = 300; areaMaxX = 350; areaMinY = 640; areaMaxY = 690; break;
            case "catherby":
                areaMinX = 420; areaMaxX = 470; areaMinY = 480; areaMaxY = 530; break;
            case "ardougne":
                areaMinX = 520; areaMaxX = 580; areaMinY = 560; areaMaxY = 620; break;
            case "yanille":
                areaMinX = 560; areaMaxX = 620; areaMinY = 720; areaMaxY = 780; break;
            case "lostcity":
                areaMinX = 100; areaMaxX = 160; areaMinY = 3490; areaMaxY = 3550; break;
            case "gnome":
                areaMinX = 680; areaMaxX = 740; areaMinY = 500; areaMaxY = 560; break;
            case "tutorial":
                areaMinX = 190; areaMaxX = 250; areaMinY = 720; areaMaxY = 770; break;
            default:
                areaMinX = 370; areaMaxX = 470; areaMinY = 530; areaMaxY = 680; break;
        }
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
            
            if (emptyTreeSearchCount > 2) {
                emptyTreeSearchCount = 0;
                if (wanderEnabled) {
                    gameMessage("No trees found nearby, wandering to find more...");
                    wanderToFindTrees();
                } else {
                    gameMessage("No trees found nearby in area. Enable wander to search wider areas.");
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
                boolean walked = api.walkTo(bankX, bankY);
                if (!walked) {
                    System.out.println("BOT: Walk failed, using ::bank");
                }
                return random(500, 1000);
            }
            
            api.openBank();
            return random(500, 1000);
        }
        
        for (int logId : logIds) {
            int count = api.getInventoryCount(logId);
            if (count > 0) {
                api.depositItem(logId, count);
                logsChopped += count;
            }
        }
        
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
