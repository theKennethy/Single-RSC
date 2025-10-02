package org.nemotech.rsc.plugins.items;

import org.nemotech.rsc.client.sound.SoundEffect;
import static org.nemotech.rsc.plugins.Plugin.message;
import static org.nemotech.rsc.plugins.Plugin.playerTalk;
import static org.nemotech.rsc.plugins.Plugin.sleep;
import static org.nemotech.rsc.plugins.Plugin.resetGnomeCooking;
import org.nemotech.rsc.model.player.InvItem;
import org.nemotech.rsc.model.player.Player;
import org.nemotech.rsc.plugins.listeners.action.InvActionListener;
import org.nemotech.rsc.plugins.listeners.executive.InvActionExecutiveListener;

public class Eating implements InvActionListener, InvActionExecutiveListener {

    @Override
    public boolean blockInvAction(InvItem item, Player p) {
        if(item.isEdible()) {
            return true;
        }
        return false;
    }

    @Override
    public void onInvAction(InvItem item, Player p) {
        if (item.isEdible()) {
            if (p.cantConsume()) {
                return;
            }
            p.setConsumeTimer(325);
            p.getSender().sendSound(SoundEffect.EAT);
            if (item.getID() == 228 || item.getID() == 18) {
                p.message("You eat the " + item.getDef().getName()
                        + ". Yuck!");
            } else if(item.getID() == 907 || item.getID() == 950) {
                message(p, "You eat the choc bomb");
                p.message("it tastes great");
            } else if(item.getID() == 908 || item.getID() == 951) {
                message(p, "You eat the veg ball");
                p.message("it tastes quite good");
            } else if(item.getID() == 909 || item.getID() == 952) {
                message(p, "You eat the " + item.getDef().getName().toLowerCase());
                playerTalk(p,null, "yuck");
                p.message("that was awful");
            } else if(item.getID() == 910 || item.getID() == 953) {
                message(p, "You eat the tangled toads legs");
                p.message("it tastes.....slimey");
            } else if(item.getID() == 1061) {
                message(p, "You eat the " + item.getDef().getName().toLowerCase());
                playerTalk(p,null, "Ow! I nearly broke a tooth!");
                p.message("You feel strangely heavier and more tired");
            } else if(item.getID() == 873) {
                p.message("You eat the leaves..chewy but tasty");
            } else if(item.getID() == 855) {
                p.message("You eat the lemon. Yuck!");
            } else if(item.getID() == 856 || item.getID() == 860) {
                p.message("You eat the " + item.getDef().getName().toLowerCase() + " ..they're very sour");
            } else if(item.getID() == 765) {
                p.message("You eat the berrys..quite tasty");
            } else if(item.getID() == 863) {
                p.message("You eat the lime ..it's quite sour");
            } else if(item.getID() == 865 || item.getID() == 865) {
                p.message("You eat the " + item.getDef().getName().toLowerCase() + "..they're quite sour");
            } else if(item.getID() == 858) {
                p.message("You eat the orange slices ...yum");
            } else if(item.getID() == 859) {
                p.message("You eat the orange cubes ...yum");
            } else if(item.getID() == 861) {
                p.message("You eat the pineapple ...yum");
            } else if(item.getID() == 862) {
                p.message("You eat the pineapple chunks ..yum");
            } else if(item.getID() == 871) {
                p.message("You eat the cream..you get some on your nose");
            } else if(item.getID() == 885) {
                message(p, 1200, "You eat the gnome bowl");
                p.message("it's pretty tastless");
                resetGnomeCooking(p);
            } else if(item.getID() == 900) {
                p.message("You eat the gnome crunchies");
                resetGnomeCooking(p);
            } else if(item.getID() == 901 || item.getID() == 944) {
                message(p, "You eat the cheese and tomato batta");
                p.message("it's quite tasty");
            } else if(item.getID() == 902 || item.getID() == 945 || item.getID() == 904 || item.getID() == 947) {
                message(p, "You eat the " + item.getDef().getName().toLowerCase());
                p.message("it's a bit chewy");
            } else if(item.getID() == 905 || item.getID() == 948 || item.getID() == 906 || item.getID() == 949) {
                message(p, "You eat the " + item.getDef().getName().toLowerCase());
                p.message("it's tastes pretty good");
            } else if(item.getID() == 911 || item.getID() == 954 || item.getID() == 914 || item.getID() == 957) {
                message(p, "You eat the " + item.getDef().getName().toLowerCase());
                p.message("they're very tasty");
            } else if(item.getID() == 912 || item.getID() == 955 || item.getID() == 913 || item.getID() == 956) {
                message(p, "You eat the " + item.getDef().getName().toLowerCase());
                p.message("they're a bit chewy");
            } else
                p.message("You eat the "
                        + item.getDef().getName().toLowerCase());
            
            final boolean heals = p.getCurStat(3) < p.getMaxStat(3);
            if (heals) {
                int newHp = p.getCurStat(3) + item.eatingHeals();
                if (newHp > p.getMaxStat(3)) {
                    newHp = p.getMaxStat(3);
                }
                p.setCurStat(3, newHp);
            }
            sleep(325);
            if (heals) {
                p.message("It heals some health");
            }
            p.getInventory().remove(item);
            switch (item.getID()) {
            case 326: // Meat pizza
                p.getInventory().add(new InvItem(328));
                break;
            case 327: // Anchovie pizza
                p.getInventory().add(new InvItem(329));
                break;
            case 330: // Cake
                p.getInventory().add(new InvItem(333));
                break;
            case 333: // Partical cake
                p.getInventory().add(new InvItem(335));
                break;
            case 332: // Choc cake
                p.getInventory().add(new InvItem(334));
                break;
            case 334: // Partical choc cake
                p.getInventory().add(new InvItem(336));
                break;
            case 257: // Apple pie
                p.getInventory().add(new InvItem(263));
                break;
            case 261: // Half apple pie
                p.getInventory().add(new InvItem(251));
                break;
            case 258: // Redberry pie
                p.getInventory().add(new InvItem(262));
                break;
            case 262: // Half redberry pie
                p.getInventory().add(new InvItem(251));
                break;
            case 259: // Meat pie
                p.getInventory().add(new InvItem(261));
                break;
            case 263: // Half meat pie
                p.getInventory().add(new InvItem(251));
                break;
            }
        }
    }
}
