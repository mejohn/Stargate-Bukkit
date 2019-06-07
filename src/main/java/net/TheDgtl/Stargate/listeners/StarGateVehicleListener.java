/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.TheDgtl.Stargate.listeners;

import net.TheDgtl.Stargate.Portal;
import net.TheDgtl.Stargate.Stargate;
import static net.TheDgtl.Stargate.Stargate.canAccessNetwork;
import static net.TheDgtl.Stargate.Stargate.canAccessPortal;
import static net.TheDgtl.Stargate.Stargate.canAccessWorld;
import static net.TheDgtl.Stargate.Stargate.handleVehicles;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/**
 *
 * @author Frostalf
 */
public class StarGateVehicleListener implements Listener {

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!handleVehicles) {
            return;
        }
        List<Entity> passengers = event.getVehicle().getPassengers();
        Vehicle vehicle = event.getVehicle();

        Portal portal = Portal.getByEntrance(event.getTo());
        if (portal == null || !portal.isOpen()) {
            return;
        }
        
        for(Entity passenger : passengers) {
	        if (passenger instanceof Player) {
	            Player player = (Player) passenger;
	            if (!portal.isOpenFor(player)) {
	                Stargate.sendMessage(player, Stargate.getString("You don't have permission to use this gate"));
	                return;
	            }
	
	            Portal dest = portal.getDestination(player);
	            if (dest == null) {
	                return;
	            }
	            boolean deny = false;
	            // Check if player has access to this network
	            if (!canAccessNetwork(player, portal.getNetwork())) {
	                deny = true;
	            }
	
	            // Check if player has access to destination world
	            if (!canAccessWorld(player, dest.getWorld().getName())) {
	                deny = true;
	            }
	
	            if (!canAccessPortal(player, portal, deny)) {
	                Stargate.sendMessage(player, Stargate.getString("You don't have permission to use this gate"));
	                portal.close(false);
	                return;
	            }

	
	            Stargate.sendMessage(player, "Teleported from " + portal.getName() + " to " + portal.getDestinationName(), false);
	            dest.teleport(vehicle);
	            portal.close(false);
	        } else {
	            Portal dest = portal.getDestination();
	            if (dest == null) {
	                return;
	            }
	            dest.teleport(vehicle);
	        }
	    }
    }

}
