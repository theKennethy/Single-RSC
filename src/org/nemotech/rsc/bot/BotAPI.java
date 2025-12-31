package org.nemotech.rsc.bot;

import org.nemotech.rsc.model.*;
import org.nemotech.rsc.model.player.*;
import org.nemotech.rsc.model.landscape.Path;
import org.nemotech.rsc.model.landscape.ActiveTile;
import org.nemotech.rsc.model.landscape.RegionManager;
import org.nemotech.rsc.external.EntityManager;
import org.nemotech.rsc.external.definition.*;
import org.nemotech.rsc.client.mudclient;
import org.nemotech.rsc.client.action.ActionManager;
import org.nemotech.rsc.client.action.impl.*;
import org.nemotech.rsc.util.EntityList;

import java.util.ArrayList;
import java.util.List;

/**
 * Bot API providing convenient methods for bot actions.
 * This is a singleton class that provides high-level methods for common bot operations.
 */
public class BotAPI {
    
    private static BotAPI instance;
    
    private BotAPI() {}
    
    public static BotAPI getInstance() {
        if (instance == null) {
            instance = new BotAPI();
        }
        return instance;
    }
    
    // ==================== PLAYER METHODS ====================
    
    /**
     * Gets the local player.
     */
    public Player getPlayer() {
        return World.getWorld().getPlayer();
    }
    
    /**
     * Gets the player's current X coordinate.
     */
    public int getX() {
        return getPlayer().getX();
    }
    
    /**
     * Gets the player's current Y coordinate.
     */
    public int getY() {
        return getPlayer().getY();
    }
    
    /**
     * Gets the player's current combat level.
     */
    public int getCombatLevel() {
        return getPlayer().getCombatLevel();
    }
    
    /**
     * Gets a skill level by index.
     * @param skill The skill index (0=Attack, 1=Defense, 2=Strength, 3=Hits, etc.)
     */
    public int getLevel(int skill) {
        return getPlayer().getMaxStat(skill);
    }
    
    /**
     * Gets the current level of a skill (can be boosted/drained).
     */
    public int getCurrentLevel(int skill) {
        return getPlayer().getCurStat(skill);
    }
    
    /**
     * Gets the experience for a skill.
     */
    public int getExperience(int skill) {
        return getPlayer().getExp(skill);
    }
    
    /**
     * Checks if the player is currently busy.
     */
    public boolean isBusy() {
        return getPlayer().isBusy();
    }
    
    /**
     * Checks if the player is in combat.
     */
    public boolean inCombat() {
        return getPlayer().inCombat();
    }
    
    /**
     * Alias for inCombat().
     */
    public boolean isInCombat() {
        return inCombat();
    }
    
    /**
     * Gets the current level of a skill (alias for getCurrentLevel).
     */
    public int getStatCurrent(int skill) {
        return getCurrentLevel(skill);
    }
    
    /**
     * Checks if the player is currently moving.
     */
    public boolean isMoving() {
        return !getPlayer().finishedPath();
    }
    
    /**
     * Gets the player's fatigue level (0-100).
     */
    public int getFatigue() {
        return getPlayer().getFatigue() / 750;
    }
    
    /**
     * Checks if the player needs to sleep.
     */
    public boolean needsSleep() {
        return getFatigue() >= 99;
    }
    
    // ==================== WALKING METHODS ====================
    
    /**
     * Walks to a specific location.
     * @param x The target X coordinate
     * @param y The target Y coordinate
     */
    public void walkTo(int x, int y) {
        Player player = getPlayer();
        if (player.isBusy()) {
            return;
        }
        Path path = new Path(player.getX(), player.getY(), x, y);
        player.setPath(path);
    }
    
    /**
     * Walks to a specific location with waypoints.
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param xOffsets X offsets for waypoints
     * @param yOffsets Y offsets for waypoints
     */
    public void walkPath(int startX, int startY, byte[] xOffsets, byte[] yOffsets) {
        Player player = getPlayer();
        if (player.isBusy()) {
            return;
        }
        ActionManager.get(WalkHandler.class).handleWalk(startX, startY, xOffsets, yOffsets, false);
    }
    
    /**
     * Calculates the distance between the player and a point.
     */
    public int distanceTo(int x, int y) {
        return Math.max(Math.abs(getX() - x), Math.abs(getY() - y));
    }
    
    /**
     * Calculates the distance between the player and an entity.
     */
    public int distanceTo(Entity entity) {
        return distanceTo(entity.getX(), entity.getY());
    }
    
    /**
     * Checks if the player is within range of a point.
     */
    public boolean isAt(int x, int y, int radius) {
        return distanceTo(x, y) <= radius;
    }
    
    /**
     * Checks if the player is at a specific location.
     */
    public boolean isAt(int x, int y) {
        return getX() == x && getY() == y;
    }
    
    // ==================== INVENTORY METHODS ====================
    
    /**
     * Gets the player's inventory.
     */
    public Inventory getInventory() {
        return getPlayer().getInventory();
    }
    
    /**
     * Counts items in inventory by ID.
     */
    public int getInventoryCount(int itemId) {
        return getInventory().countId(itemId);
    }
    
    /**
     * Checks if the inventory contains an item.
     */
    public boolean hasItem(int itemId) {
        return getInventoryCount(itemId) > 0;
    }
    
    /**
     * Checks if the inventory contains a specific amount of an item.
     */
    public boolean hasItem(int itemId, int amount) {
        return getInventoryCount(itemId) >= amount;
    }
    
    /**
     * Gets the total number of items in inventory.
     */
    public int getInventorySize() {
        return getInventory().size();
    }
    
    /**
     * Checks if inventory is full.
     */
    public boolean isInventoryFull() {
        return getInventory().full();
    }
    
    /**
     * Checks if inventory is empty.
     */
    public boolean isInventoryEmpty() {
        return getInventory().size() == 0;
    }
    
    /**
     * Gets an inventory item by index.
     */
    public InvItem getInventoryItem(int index) {
        return getInventory().get(index);
    }
    
    /**
     * Gets the first inventory item with the given ID.
     */
    public InvItem getInventoryItemById(int itemId) {
        for (InvItem item : getInventory().getItems()) {
            if (item.getID() == itemId) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Gets the index of an item in inventory.
     */
    public int getInventoryIndex(int itemId) {
        return getInventory().getLastIndexById(itemId);
    }
    
    /**
     * Drops an item from inventory.
     */
    public void dropItem(int index) {
        if (isBusy() || index < 0 || index >= getInventorySize()) {
            return;
        }
        InvItem item = getInventoryItem(index);
        if (item != null) {
            ActionManager.get(DropHandler.class).handleDrop(index);
        }
    }
    
    /**
     * Drops the first occurrence of an item by ID.
     */
    public void dropItemById(int itemId) {
        int index = getInventoryIndex(itemId);
        if (index >= 0) {
            dropItem(index);
        }
    }
    
    /**
     * Uses an inventory item (like eating food).
     */
    public void useItem(int index) {
        if (isBusy() || index < 0 || index >= getInventorySize()) {
            return;
        }
        ActionManager.get(InventoryActionHandler.class).handleInventoryAction(index);
    }
    
    /**
     * Uses an item by ID.
     */
    public void useItemById(int itemId) {
        int index = getInventoryIndex(itemId);
        if (index >= 0) {
            useItem(index);
        }
    }
    
    /**
     * Uses one inventory item on another (like tinderbox on logs).
     */
    public void useItemOnItem(int firstIndex, int secondIndex) {
        if (isBusy() || firstIndex < 0 || secondIndex < 0) return;
        if (firstIndex >= getInventorySize() || secondIndex >= getInventorySize()) return;
        ActionManager.get(InventoryUseOnItemHandler.class).handleInventoryUseOnItem(firstIndex, secondIndex);
    }
    
    // ==================== BANKING METHODS ====================
    
    /**
     * Opens the bank interface (uses ::bank command functionality).
     */
    public void openBank() {
        getPlayer().getSender().showBank();
    }
    
    /**
     * Checks if the bank is currently open.
     */
    public boolean isBankOpen() {
        return mudclient.getInstance().showDialogBank;
    }
    
    /**
     * Closes the bank interface.
     */
    public void closeBank() {
        mudclient.getInstance().showDialogBank = false;
    }
    
    /**
     * Deposits an item into the bank by item ID.
     * @param itemId The ID of the item to deposit
     * @param amount The amount to deposit
     */
    public void depositItem(int itemId, int amount) {
        if (!isBankOpen()) {
            openBank();
        }
        ActionManager.get(BankHandler.class).handleDeposit(itemId, amount);
    }
    
    /**
     * Deposits all of a specific item into the bank.
     * @param itemId The ID of the item to deposit
     */
    public void depositAll(int itemId) {
        int count = getInventoryCount(itemId);
        if (count > 0) {
            depositItem(itemId, count);
        }
    }
    
    /**
     * Deposits all items matching the given IDs.
     * @param itemIds Array of item IDs to deposit
     */
    public void depositAll(int... itemIds) {
        for (int itemId : itemIds) {
            depositAll(itemId);
        }
    }
    
    /**
     * Withdraws an item from the bank.
     * @param itemId The ID of the item to withdraw
     * @param amount The amount to withdraw
     */
    public void withdrawItem(int itemId, int amount) {
        if (!isBankOpen()) {
            openBank();
        }
        ActionManager.get(BankHandler.class).handleWithdrawl(itemId, amount);
    }
    
    /**
     * Withdraws all of a specific item from the bank.
     * @param itemId The ID of the item to withdraw
     */
    public void withdrawAll(int itemId) {
        int count = getBankCount(itemId);
        if (count > 0) {
            withdrawItem(itemId, count);
        }
    }
    
    /**
     * Gets the player's bank.
     */
    public org.nemotech.rsc.model.player.Bank getBank() {
        return getPlayer().getBank();
    }
    
    /**
     * Counts items in bank by ID.
     */
    public int getBankCount(int itemId) {
        return getBank().countId(itemId);
    }
    
    // ==================== NPC METHODS ====================
    
    /**
     * Gets all NPCs in the game.
     */
    public EntityList<NPC> getNpcs() {
        return World.getWorld().getNpcs();
    }
    
    /**
     * Finds the nearest NPC by ID.
     */
    public NPC getNearestNpc(int... ids) {
        NPC nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        
        for (NPC npc : getNpcs()) {
            if (npc == null || npc.isRemoved()) continue;
            
            for (int id : ids) {
                if (npc.getID() == id) {
                    int dist = distanceTo(npc);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = npc;
                    }
                    break;
                }
            }
        }
        return nearest;
    }
    
    /**
     * Alias for getNearestNpc with capital NPC.
     */
    public NPC getNearestNPC(int... ids) {
        return getNearestNpc(ids);
    }
    
    /**
     * Finds the nearest attackable NPC by ID.
     */
    public NPC getNearestAttackableNpc(int... ids) {
        NPC nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        
        for (NPC npc : getNpcs()) {
            if (npc == null || npc.isRemoved() || npc.inCombat() || npc.isBusy()) continue;
            
            for (int id : ids) {
                if (npc.getID() == id) {
                    int dist = distanceTo(npc);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = npc;
                    }
                    break;
                }
            }
        }
        return nearest;
    }
    
    /**
     * Finds NPCs within a certain radius.
     */
    public List<NPC> getNpcsInRadius(int radius, int... ids) {
        List<NPC> result = new ArrayList<>();
        
        for (NPC npc : getNpcs()) {
            if (npc == null || npc.isRemoved()) continue;
            if (distanceTo(npc) > radius) continue;
            
            if (ids.length == 0) {
                result.add(npc);
            } else {
                for (int id : ids) {
                    if (npc.getID() == id) {
                        result.add(npc);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Talks to an NPC.
     */
    public void talkToNpc(NPC npc) {
        if (npc == null || isBusy()) return;
        ActionManager.get(NPCHandler.class).handleTalk(npc.getIndex());
    }
    
    /**
     * Attacks an NPC.
     */
    public void attackNpc(NPC npc) {
        if (npc == null || isBusy() || npc.inCombat()) return;
        ActionManager.get(NPCHandler.class).handleAttack(npc.getIndex());
    }
    
    /**
     * Uses an item on an NPC.
     */
    public void useItemOnNpc(int itemIndex, NPC npc) {
        if (npc == null || isBusy() || itemIndex < 0) return;
        ActionManager.get(InventoryUseOnNPCHandler.class).handleInventoryUseOnNPC(npc.getIndex(), itemIndex);
    }
    
    /**
     * Pickpockets/thieve an NPC (executes NPC command like "pickpocket").
     */
    public void thieveNPC(NPC npc) {
        if (npc == null || isBusy()) return;
        ActionManager.get(NPCHandler.class).handleCommand(npc.getIndex());
    }
    
    // ==================== OBJECT METHODS ====================
    
    /**
     * Gets a game object at a specific location.
     */
    public GameObject getObjectAt(int x, int y) {
        ActiveTile tile = World.getWorld().getTile(x, y);
        if (tile != null && tile.hasGameObject()) {
            return tile.getGameObject();
        }
        return null;
    }
    
    /**
     * Finds the nearest game object by ID.
     * Uses RegionManager to get all local objects for better reliability.
     */
    public GameObject getNearestObject(int... ids) {
        GameObject nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        
        // Use RegionManager to get all local objects - this is more reliable
        for (GameObject obj : RegionManager.getLocalObjects(getPlayer())) {
            if (obj == null || obj.isRemoved()) continue;
            
            for (int id : ids) {
                if (obj.getID() == id) {
                    int dist = distanceTo(obj);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = obj;
                    }
                    break;
                }
            }
        }
        return nearest;
    }
    
    /**
     * Interacts with a game object (primary action).
     */
    public void interactObject(GameObject obj) {
        if (obj == null || isBusy()) return;
        ActionManager.get(ObjectActionHandler.class).handleObjectAction(obj.getX(), obj.getY(), true);
    }
    
    /**
     * Uses an item on a game object.
     */
    public void useItemOnObject(int itemIndex, GameObject obj) {
        if (obj == null || isBusy() || itemIndex < 0) return;
        ActionManager.get(InventoryUseOnObjectHandler.class).handleInventoryUseOnObject(itemIndex, obj.getX(), obj.getY());
    }
    
    // ==================== GROUND ITEM METHODS ====================
    
    /**
     * Finds the nearest ground item by ID.
     */
    public Item getNearestGroundItem(int... ids) {
        Item nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        int searchRadius = 20;
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                int x = getX() + dx;
                int y = getY() + dy;
                
                ActiveTile tile = World.getWorld().getTile(x, y);
                if (tile == null) continue;
                
                for (Item item : tile.getItems()) {
                    if (item == null || item.isRemoved()) continue;
                    
                    for (int id : ids) {
                        if (item.getID() == id) {
                            int dist = distanceTo(item);
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearest = item;
                            }
                            break;
                        }
                    }
                }
            }
        }
        return nearest;
    }
    
    /**
     * Picks up a ground item.
     */
    public void pickupItem(Item item) {
        if (item == null || isBusy() || isInventoryFull()) return;
        ActionManager.get(PickupHandler.class).handlePickup(item.getX(), item.getY(), item.getID());
    }
    
    // ==================== COMBAT METHODS ====================
    
    /**
     * Sets the combat style.
     * @param style 0=Controlled, 1=Aggressive, 2=Accurate, 3=Defensive
     */
    public void setCombatStyle(int style) {
        if (style < 0 || style > 3) return;
        getPlayer().setCombatStyle(style);
    }
    
    /**
     * Gets the current combat style.
     */
    public int getCombatStyle() {
        return getPlayer().getCombatStyle();
    }
    
    /**
     * Checks if in wilderness.
     */
    public boolean inWilderness() {
        return getPlayer().getLocation().inWilderness();
    }
    
    /**
     * Gets the current wilderness level.
     */
    public int getWildernessLevel() {
        return getPlayer().getLocation().wildernessLevel();
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets an item definition by ID.
     */
    public ItemDef getItemDef(int itemId) {
        return EntityManager.getItem(itemId);
    }
    
    /**
     * Gets an NPC definition by ID.
     */
    public NPCDef getNpcDef(int npcId) {
        return EntityManager.getNPC(npcId);
    }
    
    /**
     * Gets a game object definition by ID.
     */
    public GameObjectDef getObjectDef(int objectId) {
        return EntityManager.getGameObjectDef(objectId);
    }
    
    /**
     * Sends a message to the player.
     */
    public void sendMessage(String message) {
        getPlayer().getSender().sendMessage(message);
    }
    
    /**
     * Generates a random number between min and max (inclusive).
     */
    public int random(int min, int max) {
        return min + (int)(Math.random() * (max - min + 1));
    }
    
    /**
     * Sleeps for a specified number of milliseconds (non-blocking, returns delay).
     * Use this return value as your loop return value.
     */
    public int sleep(int ms) {
        return ms;
    }
    
    /**
     * Sleeps for a random duration between min and max.
     */
    public int sleep(int min, int max) {
        return random(min, max);
    }
}
