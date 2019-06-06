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
import static net.TheDgtl.Stargate.Stargate.canAccessServer;
import static net.TheDgtl.Stargate.Stargate.canAccessWorld;
import static net.TheDgtl.Stargate.Stargate.openPortal;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 *
 * @author Frostalf
 */
public class StarGatePlayerListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // cancel portal and endgateway teleportation if it's from a Stargate entrance
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (!event.isCancelled()
                && (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY && World.Environment.THE_END == event.getFrom().getWorld().getEnvironment())
                && Portal.getByAdjacentEntrance(event.getFrom()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Check to see if the player actually moved
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Portal portal = Portal.getByEntrance(event.getTo());
        // No portal or not open
        if (portal == null || !portal.isOpen()) {
            return;
        }

        // Not open for this player
        if (!portal.isOpenFor(player)) {
            Stargate.sendMessage(player, Stargate.getString("denyMsg"));
            portal.teleport(player, portal, event);
            return;
        }

        Portal destination = portal.getDestination(player);
        if (!portal.isBungee() && destination == null) {
            return;
        }

        boolean deny = false;
        // Check if player has access to this server for Bungee gates
        if (portal.isBungee()) {
            if (!canAccessServer(player, portal.getNetwork())) {
                deny = true;
            }
        } else {
            // Check if player has access to this network
            if (!canAccessNetwork(player, portal.getNetwork())) {
                deny = true;
            }

            // Check if player has access to destination world
            if (!canAccessWorld(player, destination.getWorld().getName())) {
                deny = true;
            }
        }

        if (!canAccessPortal(player, portal, deny)) {
            Stargate.sendMessage(player, Stargate.getString("denyMsg"));
            portal.teleport(player, portal, event);
            portal.close(false);
            return;
        }

        Stargate.sendMessage(player, Stargate.getString("teleportMsg"), false);

        destination.teleport(player, portal, event);
        portal.close(false);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Right click
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (block instanceof WallSign) {
                Portal portal = Portal.getByBlock(block);
                if (portal == null) {
                    return;
                }
                // Cancel item use
                event.setUseItemInHand(Event.Result.DENY);
                event.setUseInteractedBlock(Event.Result.DENY);

                boolean deny = false;
                if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
                    deny = true;
                }

                if (!Stargate.canAccessPortal(player, portal, deny)) {
                    Stargate.sendMessage(player, Stargate.getString("denyMsg"));
                    return;
                }

                if ((!portal.isOpen()) && (!portal.isFixed())) {
                    portal.cycleDestination(player);
                }
                return;
            }

            // Implement right-click to toggle a stargate, gets around spawn protection problem.
            if ((block.getType() == Material.STONE_BUTTON)) {
                Portal portal = Portal.getByBlock(block);
                if (portal == null) {
                    return;
                }

                // Cancel item use
                event.setUseItemInHand(Event.Result.DENY);
                event.setUseInteractedBlock(Event.Result.DENY);

                boolean deny = false;
                if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
                    deny = true;
                }

                if (!Stargate.canAccessPortal(player, portal, deny)) {
                    Stargate.sendMessage(player, Stargate.getString("denyMsg"));
                    return;
                }

                openPortal(player, portal);
                if (portal.isOpenFor(player)) {
                    event.setUseInteractedBlock(Event.Result.ALLOW);
                }
            }
            return;
        }

        // Left click
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Check if we're scrolling a sign
            if (block instanceof WallSign) {
                Portal portal = Portal.getByBlock(block);
                if (portal == null) {
                    return;
                }

                event.setUseInteractedBlock(Event.Result.DENY);
                // Only cancel event in creative mode
                if (player.getGameMode().equals(GameMode.CREATIVE)) {
                    event.setCancelled(true);
                }

                boolean deny = false;
                if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
                    deny = true;
                }

                if (!Stargate.canAccessPortal(player, portal, deny)) {
                    Stargate.sendMessage(player, Stargate.getString("denyMsg"));
                    return;
                }

                if ((!portal.isOpen()) && (!portal.isFixed())) {
                    portal.cycleDestination(player, -1);
                }
            }
        }
    }
}
