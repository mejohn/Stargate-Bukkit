package net.TheDgtl.Stargate;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.TheDgtl.Stargate.event.StargateAccessEvent;
import net.TheDgtl.Stargate.listeners.StarGateListener;
import net.TheDgtl.Stargate.thread.BlockPopulatorThread;
import net.TheDgtl.Stargate.thread.StarGateThread;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Stargate - A portal plugin for Bukkit
 * Copyright (C) 2011 Shaun (sturmeh)
 * Copyright (C) 2011 Dinnerbone
 * Copyright (C) 2011, 2012 Steven "Drakia" Scott <Contact@TheDgtl.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@SuppressWarnings("unused")
public class Stargate extends JavaPlugin {
	public static Logger log;
	private FileConfiguration newConfig;
	private PluginManager pm;
	public static Server server;
	public static Stargate stargate;
	//private static LangLoader lang;

    private static Stargate instance;

	private static String portalFolder;
	private static String gateFolder;
	//private static String langFolder;
	private static String defNetwork = "central";
	public static boolean destroyExplosion = false;
	public static int maxGates = 0;
	//private static String langName = "en";
	public static int activeTime = 10;
	public static int openTime = 10;
	public static boolean destMemory = false;
	public static boolean handleVehicles = true;
	public static boolean sortLists = false;
	public static boolean protectEntrance = false;
	public static boolean enableBungee = true;
	public static boolean verifyPortals = true;
	public static ChatColor signColor;

	// Temp workaround for snowmen, don't check gate entrance
	public static boolean ignoreEntrance = false;

	// Used for debug
	public static boolean debug = false;
	public static boolean permDebug = false;

	public static ConcurrentLinkedQueue<Portal> openList = new ConcurrentLinkedQueue<>();
	public static ConcurrentLinkedQueue<Portal> activeList = new ConcurrentLinkedQueue<>();

	// Used for populating gate open/closed material.
	public static Queue<BloxPopulator> blockPopulatorQueue = new LinkedList<>();

	// HashMap of player names for Bungee support
	public static Map<String, String> bungeeQueue = new HashMap<>();

    @Override
	public void onDisable() {
		Portal.closeAllGates();
		Portal.clearGates();
		getServer().getScheduler().cancelTasks(this);
	}

    @Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		pm = getServer().getPluginManager();
		newConfig = this.getConfig();
		log = Logger.getLogger("Minecraft");
		Stargate.server = getServer();
		Stargate.stargate = this;
        instance = this;

		// Set portalFile and gateFolder to the plugin folder as defaults.
		portalFolder = getDataFolder().getPath() + "/portals/";
		gateFolder = getDataFolder().getPath() + "/gates/";
		//langFolder = this.getDataFolder().getPath();
        //langFolder = getDataFolder().getPath().replaceAll("\\\\", "/");

		log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");

		// Register events before loading gates to stop weird things happening.
        StarGateListener SGL = new StarGateListener(this);

		this.loadConfig();


		// It is important to load languages here, as they are used during reloadGates()
		//lang = new LangLoader(langFolder, Stargate.langName);

		this.migrate();
		this.reloadGates();

		// Check to see if Economy is loaded yet.
		/*if (EconomyHandler.setupEconomy(pm)) {
			if (EconomyHandler.economy != null)
				log.info("[Stargate] Vault v" + EconomyHandler.vault.getDescription().getVersion() + " found");
        }*/

		getServer().getScheduler().scheduleSyncRepeatingTask(this, new StarGateThread(), 0L, 100L);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new BlockPopulatorThread(), 0L, 1L);
	}

	public void loadConfig() {
		reloadConfig();
		newConfig = this.getConfig();
		// Copy default values if required
		newConfig.options().copyDefaults(true);

		// Load values into variables
		portalFolder = newConfig.getString("portal-folder");
		gateFolder = newConfig.getString("gate-folder");
		defNetwork = newConfig.getString("default-gate-network").trim();
		destroyExplosion = newConfig.getBoolean("destroyexplosion");
		maxGates = newConfig.getInt("maxgates");
		//langName = newConfig.getString("lang");
		destMemory = newConfig.getBoolean("destMemory");
		ignoreEntrance = newConfig.getBoolean("ignoreEntrance");
		handleVehicles = newConfig.getBoolean("handleVehicles");
		sortLists = newConfig.getBoolean("sortLists");
		protectEntrance = newConfig.getBoolean("protectEntrance");
		verifyPortals = newConfig.getBoolean("verifyPortals");
		// Sign color
		String sc = newConfig.getString("signColor");
		try {
			signColor = ChatColor.valueOf(sc.toUpperCase());
		} catch (Exception ignore) {
			log.warning("[Stargate] You have specified an invalid color in your config.yml. Defaulting to BLACK");
			signColor = ChatColor.BLACK;
		}
		// Debug
		debug = true;
		permDebug = true;
		// Economy
		/*EconomyHandler.economyEnabled = newConfig.getBoolean("useeconomy");
		EconomyHandler.createCost = newConfig.getInt("createcost");
		EconomyHandler.destroyCost = newConfig.getInt("destroycost");
		EconomyHandler.useCost = newConfig.getInt("usecost");
		EconomyHandler.toOwner = newConfig.getBoolean("toowner");
		EconomyHandler.chargeFreeDestination = newConfig.getBoolean("chargefreedestination");
		EconomyHandler.freeGatesGreen = newConfig.getBoolean("freegatesgreen");*/

		this.saveConfig();
	}

	public void reloadGates() {
		// Close all gates prior to reloading
		for (Portal p : openList) {
			p.close(true);
		}

		Gate.loadGates(gateFolder);
		log.info("[Stargate] Loaded " + Gate.getGateCount() + " gate layouts");
		for (World world : getServer().getWorlds()) {
			Portal.loadAllGates(world);
		}
	}

	private void migrate() {
		// Only migrate if new file doesn't exist.
		File newPortalDir = new File(portalFolder);
		if (!newPortalDir.exists()) {
			newPortalDir.mkdirs();
		}
		File newFile = new File(portalFolder, getServer().getWorlds().get(0).getName() + ".db");
		if (!newFile.exists()) {
			newFile.getParentFile().mkdirs();
		}
	}

	public static void debug(String rout, String msg) {
		if (Stargate.debug) {
			log.info("[Stargate::" + rout + "] " + msg);
		} else {
			log.log(Level.FINEST, "[Stargate::" + rout + "] " + msg);
		}
	}

	public static void sendMessage(CommandSender player, String message) {
		sendMessage(player, message, true);
	}

	public static void sendMessage(CommandSender player, String message, boolean error) {
		if (message.isEmpty()) return;
		message = message.replaceAll("(&([a-f0-9]))", "\u00A7$2");
		if (error)
			player.sendMessage(ChatColor.RED + Stargate.getString("prefix") + ChatColor.WHITE + message);
		else
			player.sendMessage(ChatColor.GREEN + Stargate.getString("prefix") + ChatColor.WHITE + message);
	}

	public static void setLine(Sign sign, int index, String text) {
		sign.setLine(index, Stargate.signColor + text);
	}

	public static String getSaveLocation() {
		return portalFolder;
	}

	public static String getGateFolder() {
		return gateFolder;
	}

	public static String getDefaultNetwork() {
		return defNetwork;
	}

	public static String getString(String name) {
		if (name.contentEquals("prefix")) {
			return "Stargate: ";
		}
		return name;
	}

	public static void openPortal(Player player, Portal portal) {
		Portal destination = portal.getDestination();

		// Always-open gate -- Do nothing
		if (portal.isAlwaysOn()) {
			return;
		}

		// Random gate -- Do nothing
		if (portal.isRandom())
			return;

		// Invalid destination
		if ((destination == null) || (destination == portal)) {
			Stargate.sendMessage(player, "Invalid destination");
			return;
		}

		// Gate is already open
		if (portal.isOpen()) {
			// Close if this player opened the gate
			if (portal.getActivePlayer() == player) {
				portal.close(false);
			}
			return;
		}

		// Gate that someone else is using -- Deny access
		if ((!portal.isFixed()) && portal.isActive() &&  (portal.getActivePlayer() != player)) {
			Stargate.sendMessage(player, "Someone else is using that gate right now");
			return;
		}

		// Destination blocked
		if ((destination.isOpen()) && (!destination.isAlwaysOn())) {
			Stargate.sendMessage(player, "Destination is blocked");
			return;
		}

		// Open gate
		portal.open(player, false);
	}

	/*
	 * Check whether the player has the given permissions.
	 */
	public static boolean hasPerm(Player player, String perm) {
		return true;
	}

	/*
	 * Check a deep permission, this will check to see if the permissions is defined for this use
	 * If using Permissions it will return the same as hasPerm
	 * If using SuperPerms will return true if the node isn't defined
	 * Or the value of the node if it is
	 */
	public static boolean hasPermDeep(Player player, String perm) {
		return true;
	}

	/*
	 * Check whether player can teleport to dest world
	 */
	public static boolean canAccessWorld(Player player, String world) {
		return true;
	}

	/*
	 * Check whether player can use network
	 */
	public static boolean canAccessNetwork(Player player, String network) {
		return true;
	}

	/*
	 * Check whether the player can access this server
	 */
	public static boolean canAccessServer(Player player, String server) {
		return true;
	}

	/*
	 * Call the StargateAccessPortal event, used for other plugins to bypass Permissions checks
	 */
	public static boolean canAccessPortal(Player player, Portal portal, boolean deny) {
		return true;
	}

	/*
	 * Return true if the portal is free for the player
	 */
	public static boolean isFree(Player player, Portal src, Portal dest) {
		return dest != null;
	}

	/*
	 * Check whether the player can see this gate (Hidden property check)
	 */
	public static boolean canSee(Player player, Portal portal) {
		return true;
	}

	/*
	 * Check if the player can use this private gate
	 */
	public static boolean canPrivate(Player player, Portal portal) {
		return true;
	}

	/*
	 * Check if the player has access to {option}
	 */
	public static boolean canOption(Player player, String option) {
		return true;
	}

	/*
	 * Check if the player can create gates on {network}
	 */
	public static boolean canCreate(Player player, String network) {
		return true;

	}

	/*
	 * Check if the player can create a personal gate
	 */
	public static boolean canCreatePersonal(Player player) {
		return true;
	}

	/*
	 * Check if the player can create this gate layout
	 */
	public static boolean canCreateGate(Player player, String gate) {
		return true;
	}

	/*
	 * Check if the player can destroy this gate
	 */
	public static boolean canDestroy(Player player, Portal portal) {
		return true;
	}
	
	public static int getCreateCost(Player player, Gate gate) {
		return 0;
	}
	
	private Plugin checkPlugin(String p) {
		Plugin plugin = pm.getPlugin(p);
		return checkPlugin(plugin);
	}

	private Plugin checkPlugin(Plugin plugin) {
		if (plugin != null && plugin.isEnabled()) {
			log.info("[Stargate] Found " + plugin.getDescription().getName() + " (v" + plugin.getDescription().getVersion() + ")");
			return plugin;
		}
		return null;
	}

	/*
	 * Parse a given text string and replace the variables
	 */
	public static String replaceVars(String format, String[] search, String[] replace) {
		if (search.length != replace.length) return "";
		for (int i = 0; i < search.length; i++) {
			format = format.replace(search[i], replace[i]);
		}
		return format;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String cmd = command.getName();
		if (cmd.equalsIgnoreCase("sg")) {
			if (args.length != 1) return false;
			if (args[0].equalsIgnoreCase("about")) {
				sender.sendMessage("Stargate Plugin created by Drakia");
				return true;
			}
			if (sender instanceof Player) {
				Player p = (Player)sender;
				if (!hasPerm(p, "stargate.admin") && !hasPerm(p, "stargate.admin.reload")) {
					sendMessage(sender, "Permission Denied");
					return true;
				}
			}
			if (args[0].equalsIgnoreCase("reload")) {
				// Deactivate portals
				for (Portal p : activeList) {
					p.deactivate();
				}
				// Close portals
				for (Portal p : openList) {
					p.close(true);
				}
				// Clear all lists
				activeList.clear();
				openList.clear();
				Portal.clearGates();
				Gate.clearGates();

				loadConfig();
				reloadGates();
				
				sendMessage(sender, "Stargate reloaded");
				return true;
			}
			return false;
		}
		return false;
	}

    public static Stargate getInstance() {
        return instance;
    }

	public static String getSignString(String operator) {
		// TODO Auto-generated method stub
		if(operator.contentEquals("signRightClick")) {
			return "Right click";
		} else if(operator.contentEquals("signToUse")) {
			return "to use gate";
		} else if(operator.contentEquals("signRandom")) {
			return "Random";
		} else if(operator.contentEquals("signDisconnected")) {
			return "Disconnected";
		}
		return operator;
	}
}
