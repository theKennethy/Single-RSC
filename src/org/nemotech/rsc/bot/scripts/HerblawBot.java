package org.nemotech.rsc.bot.scripts;

import org.nemotech.rsc.bot.Bot;

/**
 * A herblaw bot that identifies herbs and makes potions.
 * 
 * Supported modes:
 * - IDENTIFY: Identify unidentified herbs for XP
 * - POTIONS: Make potions (herb + vial of water)
 * 
 * Usage:
 * 1. Have unid herbs or herbs + vials in your inventory
 * 2. Start the bot with the appropriate mode
 * 
 * Unidentified Herb IDs:
 * - Unid. herb (Guam): 165
 * - Unid. herb (Marrentill): 435
 * - Unid. herb (Tarromin): 436
 * - Unid. herb (Harralander): 437
 * - Unid. herb (Ranarr): 438
 * - Unid. herb (Irit): 439
 * - Unid. herb (Avantoe): 440
 * - Unid. herb (Kwuarm): 441
 * - Unid. herb (Cadantine): 442
 * - Unid. herb (Dwarf weed): 443
 * - Unid. herb (Torstol): 444
 * 
 * Note: Members only skill.
 */
public class HerblawBot extends Bot {
    
    private static final int VIAL_OF_WATER = 464;
    
    // Unidentified herb IDs
    private int[] unidHerbIds = { 165, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444 };
    
    // Identified herb IDs for potions
    private int[] herbIds = { 444, 443, 442, 441, 440, 439, 438, 437, 436, 435, 468 };
    
    public enum Mode {
        IDENTIFY,   // Identify herbs
        POTIONS     // Make potions
    }
    
    private Mode mode = Mode.IDENTIFY;
    
    private enum State {
        IDLE,
        PROCESSING,
        BANKING
    }
    
    private State state = State.IDLE;
    private int itemsProcessed = 0;
    
    public HerblawBot() {
        super("Herblaw Bot");
    }
    
    public HerblawBot(Mode mode) {
        super("Herblaw Bot");
        this.mode = mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        itemsProcessed = 0;
        state = State.IDLE;
        gameMessage("Herblaw bot started! Mode: " + mode);
        
        if (mode == Mode.IDENTIFY) {
            gameMessage("Will identify unidentified herbs.");
        } else {
            gameMessage("Will make potions. Need herbs + vials of water.");
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (api.isBankOpen()) {
            api.closeBank();
        }
        gameMessage("Herblaw bot stopped. Items processed: " + itemsProcessed);
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
        
        // Close bank if open
        if (api.isBankOpen()) {
            api.closeBank();
            return random(300, 500);
        }
        
        if (mode == Mode.IDENTIFY) {
            return identifyHerbs();
        } else {
            return makePotions();
        }
    }
    
    private int identifyHerbs() {
        // Find unid herb
        int herbIndex = -1;
        for (int id : unidHerbIds) {
            int idx = api.getInventoryIndex(id);
            if (idx >= 0) {
                herbIndex = idx;
                break;
            }
        }
        
        if (herbIndex < 0) {
            state = State.BANKING;
            return handleBankingIdentify();
        }
        
        // Identify the herb (use item action)
        state = State.PROCESSING;
        api.useItem(herbIndex);
        itemsProcessed++;
        
        return random(500, 800);
    }
    
    private int makePotions() {
        // Check for vial of water
        int vialIndex = api.getInventoryIndex(VIAL_OF_WATER);
        if (vialIndex < 0) {
            state = State.BANKING;
            return handleBankingPotions();
        }
        
        // Find identified herb
        int herbIndex = -1;
        for (int id : herbIds) {
            int idx = api.getInventoryIndex(id);
            if (idx >= 0) {
                herbIndex = idx;
                break;
            }
        }
        
        if (herbIndex < 0) {
            state = State.BANKING;
            return handleBankingPotions();
        }
        
        // Use herb on vial
        state = State.PROCESSING;
        api.useItemOnItem(herbIndex, vialIndex);
        itemsProcessed++;
        
        return random(1500, 2000);
    }
    
    private int handleBankingIdentify() {
        if (!api.isBankOpen()) {
            api.openBank();
            return random(600, 800);
        }
        
        // Deposit identified herbs
        api.depositAll();
        
        // Withdraw unid herbs
        for (int herbId : unidHerbIds) {
            int bankCount = api.getBankCount(herbId);
            if (bankCount > 0) {
                api.withdrawItem(herbId, Math.min(bankCount, 28));
                api.closeBank();
                state = State.IDLE;
                return random(600, 800);
            }
        }
        
        gameMessage("Out of unidentified herbs! Please add more to your bank.");
        api.closeBank();
        return random(5000, 6000);
    }
    
    private int handleBankingPotions() {
        if (!api.isBankOpen()) {
            api.openBank();
            return random(600, 800);
        }
        
        // Deposit potions
        api.depositAll();
        
        // Withdraw vials
        int vialCount = api.getBankCount(VIAL_OF_WATER);
        if (vialCount > 0) {
            api.withdrawItem(VIAL_OF_WATER, Math.min(vialCount, 14));
        }
        
        // Withdraw herbs
        for (int herbId : herbIds) {
            int bankCount = api.getBankCount(herbId);
            if (bankCount > 0) {
                api.withdrawItem(herbId, Math.min(bankCount, 14));
                api.closeBank();
                state = State.IDLE;
                return random(600, 800);
            }
        }
        
        gameMessage("Out of materials! Please add herbs and vials to your bank.");
        api.closeBank();
        return random(5000, 6000);
    }
    
    public int getItemsProcessed() {
        return itemsProcessed;
    }
    
    public State getState() {
        return state;
    }
    
    public Mode getMode() {
        return mode;
    }
}
