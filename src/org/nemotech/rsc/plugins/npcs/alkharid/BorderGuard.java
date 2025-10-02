package org.nemotech.rsc.plugins.npcs.alkharid;

import static org.nemotech.rsc.plugins.Plugin.npcTalk;
import static org.nemotech.rsc.plugins.Plugin.playerTalk;
import static org.nemotech.rsc.plugins.Plugin.showMenu;
import org.nemotech.rsc.plugins.Plugin;
import org.nemotech.rsc.model.GameObject;
import org.nemotech.rsc.model.NPC;
import org.nemotech.rsc.model.player.Player;
import org.nemotech.rsc.plugins.listeners.action.ObjectActionListener;
import org.nemotech.rsc.plugins.listeners.action.TalkToNpcListener;
import org.nemotech.rsc.plugins.listeners.executive.ObjectActionExecutiveListener;
import org.nemotech.rsc.plugins.listeners.executive.TalkToNpcExecutiveListener;

public final class BorderGuard implements TalkToNpcExecutiveListener,
        TalkToNpcListener, ObjectActionExecutiveListener, ObjectActionListener {

    @Override
    public void onTalkToNpc(Player p, final NPC n) {
        if (p.getQuestStage(Plugin.PRINCE_ALI_RESCUE) == -1
                || p.getQuestStage(Plugin.PRINCE_ALI_RESCUE) == 3) {
            playerTalk(p, n, "Can I come through this gate?");
            npcTalk(p, n,
                    "You may pass for free, you are a friend of Al Kharid");
            p.message("The gate swings open");
            if (p.getX() > 91)
                p.teleport(90, 649, false);
            else
                p.teleport(93, 649, false);
            return;
        }
        playerTalk(p, n, "Can I come through this gate?");
        npcTalk(p, n, "You must pay a toll of 10 gold coins to pass");
        int option = showMenu(p, n, "No thankyou, I'll walk round",
                "Who does my money go to?", "yes ok");
        switch (option) {
        case 0: // no thanks
            npcTalk(p, n, "Ok suit yourself");
            break;
        case 1: // who does money go to
            npcTalk(p, n, "The money goes to the city of Al Kharid");
            break;
        case 2:
            if (p.getInventory().remove(10, 10) > -1) {
                p.message("You pay the guard");
                npcTalk(p, n, "You may pass");
                p.message("The gate swings open");
                if (p.getX() > 91)
                    p.teleport(90, 649, false);
                else
                    p.teleport(93, 649, false);
            } else {
                npcTalk(p, n,
                        "Oh dear I don't actually seem to have enough money");
            }
            break;
        }
    }

    @Override
    public boolean blockTalkToNpc(Player p, NPC n) {
        return n.getID() == 161;
    }

    @Override
    public void onObjectAction(GameObject obj, String command, Player player) {
        if (obj.getID() == 180 && command.equals("open")) {
            player.message("You need to talk to the border guard");
        }
    }

    @Override
    public boolean blockObjectAction(GameObject obj, String command,
            Player player) {
        return obj.getID() == 180 && command.equals("open");
    }
}
