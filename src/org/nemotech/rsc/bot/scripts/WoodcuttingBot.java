package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

/**
 * A woodcutting bot that chops trees and banks logs.
 * 
 * Usage:
 * 1. Start the bot near some trees
 * 2. It will automatically find and chop trees
 * 3. When inventory is full, it banks all logs (uses ::bank)
 * 
 * Tree Object IDs:
 * - Regular Tree: 0, 1, 70
 * - Oak Tree: 306
 * - Willow Tree: 307
 * - Maple Tree: 308
 * - Yew Tree: 309
 * - Magic Tree: 310
 * 
 * Log Item IDs:
 * - Logs: 14
 * - Oak Logs: 632
 * - Willow Logs: 633
 * - Maple Logs: 634
 * - Yew Logs: 635
 * - Magic Logs: 636
 */
public class WoodcuttingBot extends Bot {
    
    // Default tree IDs (regular trees - multiple IDs for different tree graphics)
    private int[] treeIds = { 0, 1, 70 };
    
    // Default log IDs to bank
    private int[] logIds = { 14 };
    
    // Track what we're doing
    private enum State {
        IDLE,
        WALKING_TO_TREE,
        CHOPPING,
        BANKING
    }
    
    private State state = State.IDLE;
    private GameObject targetTree = null;
    private int logsChopped = 0;
    
    public WoodcuttingBot() {
        super("Woodcutting Bot");
    }
    
    /**
     * Creates a woodcutting bot with custom tree and log IDs.
     * @param treeIds Array of tree object IDs to chop
     * @param logIds Array of log item IDs to bank
     */
    public WoodcuttingBot(int[] treeIds, int[] logIds) {
        super("Woodcutting Bot");
        this.treeIds = treeIds;
        this.logIds = logIds;
    }
    
    /**
     * Sets the tree types to chop.
     */
    public void setTreeIds(int... ids) {
        this.treeIds = ids;
    }
    
    /**
     * Sets the log types to bank.
     */
    public void setLogIds(int... ids) {
        this.logIds = ids;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        logsChopped = 0;
        state = State.IDLE;
        gameMessage("Woodcutting bot started! Chopping tree IDs: " + arrayToString(treeIds));
        gameMessage("Logs will be banked when inventory is full.");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        // Close bank if open
        if (api.isBankOpen()) {
            api.closeBank();
        }
        gameMessage("Woodcutting bot stopped. Total logs banked: " + logsChopped);
    }
    
    private long lastStatusTime = 0;
    private int treesChopped = 0;
    
    @Override
    public int loop() {
        // Check if we need to sleep
        if (api.needsSleep()) {
            if (System.currentTimeMillis() - lastStatusTime > 10000) {
                gameMessage("@red@Fatigue full! Use ::sleep or a bed.");
                lastStatusTime = System.currentTimeMillis();
            }
            return 5000;
        }
        
        // Don't do anything if busy (chopping, walking, etc)
        if (api.isBusy()) {
            return random(300, 500);
        }
        
        // If we're walking, wait until we stop
        if (api.isMoving()) {
            return random(300, 500);
        }
        
        // If we're chopping, check if the tree is still there
        if (state == State.CHOPPING) {
            // Tree got cut down or we finished, go back to idle
            if (targetTree == null || targetTree.isRemoved()) {
                state = State.IDLE;
                targetTree = null;
            } else {
                // Still chopping, wait
                return random(500, 1000);
            }
        }
        
        // Handle banking logs when inventory is full
        if (api.isInventoryFull()) {
            state = State.BANKING;
            targetTree = null;
            return bankLogs();
        }
        
        // Close bank if it's open and we're not full
        if (api.isBankOpen()) {
            api.closeBank();;
            return random(300, 500);
        }
        
        // Find and chop a tree
        return chopTree();
    }
    
    private int chopTree() {
        // Find nearest tree
        GameObject tree = api.getNearestObject(treeIds);
        
        if (tree == null || tree.isRemoved()) {
            state = State.IDLE;
            targetTree = null;
            // Only log occasionally to reduce spam
            if (System.currentTimeMillis() - lastStatusTime > 5000) {
                log("Searching for trees... IDs: " + arrayToString(treeIds));
                lastStatusTime = System.currentTimeMillis();
            }
            return random(1000, 2000);
        }
        
        int dist = api.distanceTo(tree);
        
        // If too far, walk to it first
        if (dist > 2) {
            state = State.WALKING_TO_TREE;
            targetTree = tree;
            api.walkTo(tree.getX(), tree.getY());
            return random(600, 1000);
        }
        
        // We're close enough - chop the tree
        state = State.CHOPPING;
        targetTree = tree;
        api.interactObject(tree);
        treesChopped++;
        
        // Show status every 10 trees
        if (treesChopped % 10 == 0) {
            gameMessage("@gre@[Bot] Chopped " + treesChopped + " trees, banked " + logsChopped + " logs");
        }
        
        return random(2000, 3000);
    }
    
    private int bankLogs() {
        // Open bank if not already open
        if (!api.isBankOpen()) {
            api.openBank();
            return random(600, 800);
        }
        
        // Deposit all logs
        for (int logId : logIds) {
            int count = api.getInventoryCount(logId);
            if (count > 0) {
                api.depositItem(logId, count);
                logsChopped += count;
                return random(300, 500);
            }
        }
        
        // Done banking, close bank and continue
        api.closeBank();
        state = State.IDLE;
        return random(300, 500);
    }
    
    private int countLogs() {
        int count = 0;
        for (int logId : logIds) {
            count += api.getInventoryCount(logId);
        }
        return count;
    }
    
    private String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Gets the total logs chopped this session.
     */
    public int getLogsChopped() {
        return logsChopped;
    }
    
    /**
     * Gets the current bot state.
     */
    public State getState() {
        return state;
    }
}
