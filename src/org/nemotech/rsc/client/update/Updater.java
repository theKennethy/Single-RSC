package org.nemotech.rsc.client.update;

import org.nemotech.rsc.client.mudclient;
import org.nemotech.rsc.model.player.Player;
import org.nemotech.rsc.model.World;

public abstract class Updater {
    
    public mudclient mc;
    
    public Player player = World.getWorld().getPlayer();
    
    protected mudclient getMC() {
        if (mc == null) {
            mc = mudclient.getInstance();
        }
        return mc;
    }
    
    public void handlePositionUpdate(Player player) {}
    
    public void handleGraphicsUpdate(Player player) {}
    
}