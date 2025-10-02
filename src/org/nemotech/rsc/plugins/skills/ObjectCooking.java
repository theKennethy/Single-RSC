package org.nemotech.rsc.plugins.skills;

import static org.nemotech.rsc.plugins.Plugin.addItem;
import static org.nemotech.rsc.plugins.Plugin.getNearestNpc;
import static org.nemotech.rsc.plugins.Plugin.hasItem;
import static org.nemotech.rsc.plugins.Plugin.message;
import static org.nemotech.rsc.plugins.Plugin.npcTalk;
import static org.nemotech.rsc.plugins.Plugin.removeItem;
import static org.nemotech.rsc.plugins.Plugin.showBubble;
import static org.nemotech.rsc.plugins.Plugin.showMenu;

import java.util.Arrays;

import org.nemotech.rsc.plugins.Plugin;
import org.nemotech.rsc.event.SingleEvent;
import org.nemotech.rsc.util.Formulae;
import org.nemotech.rsc.external.EntityManager;
import org.nemotech.rsc.external.definition.extra.ItemCookingDef;
import org.nemotech.rsc.event.impl.BatchEvent;
import org.nemotech.rsc.client.sound.SoundEffect;
import org.nemotech.rsc.model.player.InvItem;
import org.nemotech.rsc.model.GameObject;
import org.nemotech.rsc.model.NPC;
import org.nemotech.rsc.model.player.Player;
import org.nemotech.rsc.model.World;
import org.nemotech.rsc.plugins.listeners.action.InvUseOnObjectListener;
import org.nemotech.rsc.plugins.listeners.executive.InvUseOnObjectExecutiveListener;

public class ObjectCooking implements InvUseOnObjectListener, InvUseOnObjectExecutiveListener {
    
    @Override
    public void onInvUseOnObject(GameObject object, InvItem item, Player owner) {
        if (owner.getQuestStage(Plugin.COOKS_ASSISTANT) != -1) {
            NPC cook = getNearestNpc(owner, 7, 20);
            if(cook == null) {
                handleCooking(item, owner, object);
                return;
            }
            if(cook != null) {
                cook.face(owner);
                owner.face(cook);
                npcTalk(owner, cook, "Hey! Who said you could use that?");
            }
            return;
        }
        handleCooking(item, owner, object);
        return;
    }

    private void handleCooking(final InvItem item, Player p,
            final GameObject object) {
        if(p.getLocation().onTutorialIsland() && item.getID() == 503 && p.getCache().hasKey("tutorial") && p.getCache().getInt("tutorial") >= 0  &&  p.getCache().getInt("tutorial") <= 31) {
            p.setBusy(true);
            showBubble(p, item);
            p.getSender().sendSound(SoundEffect.COOKING);
            p.message("You cook the meat on the stove...");
            if(p.getCache().hasKey("tutorial") && p.getCache().getInt("tutorial") == 25) {
                p.message("You accidentally burn the meat");
                p.getInventory().replace(503, 134);

                message(p, "sometimes you will burn food",
                        "As your cooking level increases this will happen less",
                        "Now speak to the cooking instructor again");
                p.getCache().set("tutorial", 30);
            } else if(p.getCache().hasKey("tutorial") && p.getCache().getInt("tutorial") == 30) {
                p.message("The meat is now nicely cooked");
                message(p, "Now speak to the cooking instructor again");
                p.getCache().set("tutorial", 31);
                p.getInventory().replace(503, 132);

            }
            p.setBusy(false);
            return;
        }
        else if(item.getID() == 177 && object.getID() == 435 && object.getX() == 618 && object.getY() == 3453) {
            if(p.getQuestStage(Plugin.THE_HAZEEL_CULT) == 3 && p.getCache().hasKey("evil_side")) {
                message(p, "you poor the poison into the hot pot",
                        "the poison desolves into the soup");
                removeItem(p, 177, 1);
                p.updateQuestStage(Plugin.THE_HAZEEL_CULT, 4);
            } else {
                p.message("nothing interesting happens");
            }
        }
        else if (item.getID() == 784) {
            cookMethod(p, 784, 785, "you warm the paste over the fire", "it thickens into a sticky goo");
        }
        else if (item.getID() == 622) { // Seaweed (Glass)
            cookMethod(p, 622, 624, "You put the seaweed on the "
                    + object.getGameObjectDef().getName().toLowerCase(), "The seaweed burns to ashes");
        } else {
            final ItemCookingDef cookingDef = EntityManager.getItemCookingDef(item.getID());
            if (cookingDef == null) {
                p.message("Nothing interesting happens");
                return;
            }
            if (p.getCurStat(7) < cookingDef.getReqLevel()) {
                p.message("You need a cooking level of " + cookingDef.getReqLevel() + " to cook " + item.getDef().getName().toLowerCase().substring(4));
                return;
            }
            if (!p.withinRange(object, 2)) { 
                return;
            }
            p.message(cookingOnMessage(p, item, object));
            showBubble(p, item);
            // Choose how many to cook
            int maxMake = p.getInventory().countId(item.getID());
            if (maxMake <= 0) {
                return;
            }
            String[] countOptions = new String[] {"Cook 1", "Cook 5", "Cook 10", "Cook All"};
            int choice = showMenu(p, countOptions);
            if (p.isBusy() || choice < 0 || choice > 3) {
                return;
            }
            final int makeCount = (choice == 3 ? maxMake : Integer.parseInt(countOptions[choice].replace("Cook ", "")));
            p.setBatchEvent(new BatchEvent(p, 1500, makeCount) {
                @Override
                public void action() {
                    InvItem cookedFood = new InvItem(cookingDef.getCookedId());
                    if (owner.getFatigue() >= 7500) {
                        owner.message("You are too tired to cook this fish");
                        interrupt();
                        return;
                    }
                    showBubble(owner, item);
                    owner.getSender().sendSound(SoundEffect.COOKING);
                    if (owner.getInventory().remove(item) > -1) {
                        if (!Formulae.burnFood(owner, item.getID(), owner.getCurStat(7)) || item.getID() == 591) {
                            owner.getInventory().add(cookedFood);
                            owner.message(cookedMessage(owner, cookedFood));
                            owner.incExp(7, cookingDef.getExp(), true);
                        } else {
                            owner.getInventory().add(new InvItem(cookingDef.getBurnedId()));
                            owner.message("You accidentally burn the " + cookedFood.getDef().getName().toLowerCase());
                        }
                    } else {
                        interrupt();
                    }
                }
            });
        }
    }

    @Override
    public boolean blockInvUseOnObject(GameObject obj, InvItem item, Player player) {
        int[] ids = new int[]{ 97, 11, 119, 435, 491};
        Arrays.sort(ids);
        if ((item.getID() == 622 || item.getID() == 784) && Arrays.binarySearch(ids, obj.getID()) >= 0) {
            return true;
        }
        if(item.getID() == 177 && obj.getID() == 435 && obj.getX() == 618 && obj.getY() == 3453) {
            return true;
        }
        final ItemCookingDef cookingDef = EntityManager.getItemCookingDef(item.getID());
        return cookingDef != null && Arrays.binarySearch(ids, obj.getID()) >= 0;
    }

    public void cookMethod(final Player p, final int itemID, final int product, String... messages) {
        int maxMake = p.getInventory().countId(itemID);
        if (maxMake <= 0) {
            p.message("You don't have all the ingredients");
            return;
        }
        String[] opts = new String[] {"Make 1", "Make 5", "Make 10", "Make All"};
        int choice = showMenu(p, opts);
        if (p.isBusy() || choice < 0 || choice > 3) return;
        int batches = (choice == 3 ? maxMake : Integer.parseInt(opts[choice].replace("Make ", "")));
        p.setBatchEvent(new BatchEvent(p, 650, batches) {
            @Override
            public void action() {
                if(hasItem(p, itemID, 1)) {
                    showBubble(p, new InvItem(itemID));
                    p.getSender().sendSound(SoundEffect.COOKING);
                    removeItem(p, itemID, 1);
                    addItem(p, product, 1);
                } else {
                    interrupt();
                }
            }
        });
        if (messages != null && messages.length > 0) {
            message(p, messages);
        }
    }
    
    private String cookingOnMessage(Player p, InvItem item, GameObject object) {
        String message = "You cook the " + item.getDef().getName().toLowerCase().substring(4) + " on the " + (object.getID() == 97 ? "fire" : "stove");
        if(item.getID() == 504) {
            message = "You cook the meat on the stove...";
        }
        return message;
        
    }
    
    private String cookedMessage(Player p, InvItem cookedFood) {
        String message = "The " + cookedFood.getDef().getName().toLowerCase() + " is now nicely cooked";
        if(cookedFood.getID() == 132) {
            message = "The meat is now nicely cooked";
        }
        return message;
        
    }
}
