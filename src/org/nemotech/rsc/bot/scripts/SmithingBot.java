package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;
import org.nemotech.rsc.model.GameObject;

/**
 * A smithing bot that smiths bars into items at anvils.
 * 
 * Usage:
 * 1. Have bars and a hammer (ID 168) in your inventory
 * 2. Stand near an anvil (object ID 50)
 * 3. Start the bot - it will smith items
 * 
 * Bar IDs:
 * - Bronze bar: 169
 * - Iron bar: 170
 * - Steel bar: 171
 * - Mithril bar: 173
 * - Adamantite bar: 174
 * - Runite bar: 408
 * - Gold bar: 172
 * - Silver bar: 384
 * 
 * Note: This bot uses bars on the anvil. The smithing menu will appear
 * and you need to have previously selected what to make.
 */
public class SmithingBot extends Bot {
    
    private static final int ANVIL = 50;
    private static final int HAMMER = 168;
    
    // Bars to smith (priority order)
    private int[] barIds = { 169, 170, 171, 173, 174, 408 };
    
    private enum State {
        IDLE,
        SMITHING,
        BANKING
    }
    
    private State state = State.IDLE;
    private int itemsSmithed = 0;
    
    public SmithingBot() {
        super("Smithing Bot");
    }
    
    /**
     * Sets specific bar IDs to smith.
     */
    public void setBarIds(int... ids) {
        this.barIds = ids;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        itemsSmithed = 0;
        state = State.IDLE;
        
        // Check for hammer
        if (api.getInventoryIndex(HAMMER) < 0) {
            gameMessage("Warning: No hammer found in inventory!");
        }
        
        gameMessage("Smithing bot started! Will use bars on anvil.");
        gameMessage("Make sure to select what you want to smith in the menu.");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (api.isBankOpen()) {
            api.closeBank();
        }
        gameMessage("Smithing bot stopped. Items smithed: " + itemsSmithed);
    }
    
    @Override
    public int loop() {
        // Don't do anything if busy
        if (api.isBusy() || api.isMoving()) {
            return random(300, 500);
        }
        
        // Check for hammer
        if (api.getInventoryIndex(HAMMER) < 0) {
            gameMessage("No hammer! Please add one to your inventory.");
            return 5000;
        }
        
        // Find bars to smith
        int barIndex = -1;
        for (int id : barIds) {
            int idx = api.getInventoryIndex(id);
            if (idx >= 0) {
                barIndex = idx;
                break;
            }
        }
        
        // No bars - go bank
        if (barIndex < 0) {
            state = State.BANKING;
            return handleBanking();
        }
        
        // Close bank if open
        if (api.isBankOpen()) {
            api.closeBank();
            return random(300, 500);
        }
        
        // Find anvil
        GameObject anvil = api.getNearestObject(ANVIL);
        if (anvil == null) {
            log("No anvil found nearby!");
            return random(2000, 3000);
        }
        
        // Walk to anvil if too far
        if (api.distanceTo(anvil) > 1) {
            api.walkTo(anvil.getX(), anvil.getY());
            return random(600, 1000);
        }
        
        // Use bar on anvil
        state = State.SMITHING;
        api.useItemOnObject(barIndex, anvil);
        itemsSmithed++;
        
        return random(2000, 3000);
    }
    
    private int handleBanking() {
        if (!api.isBankOpen()) {
            api.openBank();
            return random(600, 800);
        }
        
        // Deposit smithed items (deposit all non-bars/hammer)
        api.depositAll();
        
        // Keep hammer
        if (api.getBankCount(HAMMER) > 0) {
            api.withdrawItem(HAMMER, 1);
        }
        
        // Try to withdraw bars
        for (int barId : barIds) {
            int bankCount = api.getBankCount(barId);
            if (bankCount > 0) {
                api.withdrawItem(barId, Math.min(bankCount, 27));
                api.closeBank();
                state = State.IDLE;
                return random(600, 800);
            }
        }
        
        gameMessage("Out of bars! Please add more to your bank.");
        api.closeBank();
        return random(5000, 6000);
    }
    
    public int getItemsSmithed() {
        return itemsSmithed;
    }
    
    public State getState() {
        return state;
    }
}
