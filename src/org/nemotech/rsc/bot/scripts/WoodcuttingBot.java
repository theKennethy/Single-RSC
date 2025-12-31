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
    
    @Override
    public int loop() {
        // Check if we need to sleep
        if (api.needsSleep()) {
            gameMessage("Fatigue is full! Please sleep.");
            return 5000;
        }
        
        // Don't do anything if busy
        if (api.isBusy() || api.isMoving()) {
            return random(300, 500);
        }
        
        // Handle banking logs when inventory is full
        if (api.isInventoryFull()) {
            state = State.BANKING;
            return bankLogs();
        }
        
        // Close bank if it's open and we're not full
        if (api.isBankOpen()) {
            api.closeBank();
            return random(300, 500);
        }
        
        // Find and chop a tree
        return chopTree();
    }
    
    private int chopTree() {
        // Find nearest tree
        targetTree = api.getNearestObject(treeIds);
        
        if (targetTree == null) {
            log("No trees found nearby! Looking for IDs: " + arrayToString(treeIds));
            return random(2000, 3000);
        }
        
        // Debug: show which tree was found
        gameMessage("Found tree ID " + targetTree.getID() + " at (" + targetTree.getX() + ", " + targetTree.getY() + ")");
        
        // Check if we're close enough to interact
        if (api.distanceTo(targetTree) > 1) {
            state = State.WALKING_TO_TREE;
            api.walkTo(targetTree.getX(), targetTree.getY());
            return random(600, 1000);
        }
        
        // Chop the tree
        state = State.CHOPPING;
        api.interactObject(targetTree);
        
        return random(1500, 2500);
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
