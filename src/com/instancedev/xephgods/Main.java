package com.instancedev.xephgods;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	public static Economy econ = null;
	public static boolean economy = true;

	HashMap<String, Cuboid> god_cube = new HashMap<String, Cuboid>();

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);

		this.getConfig().options().header("These are default rewards:");
		this.getConfig().addDefault("config.use_economy", true);
		this.getConfig().addDefault("config.use_itemreward", true);
		this.getConfig().addDefault("config.use_cmdreward", true);
		this.getConfig().addDefault("config.econ_reward", 10);
		this.getConfig().addDefault("config.item_reward", "10:1*2");
		this.getConfig().addDefault("config.cmd_reward", "say Thanks for praying, <player>!");

		this.getConfig().options().copyDefaults(true);
		this.saveConfig();

		if (!setupEconomy()) {
			getLogger().severe(String.format("[%s] - No Economy (Vault) dependency found! Disabling Economy.", getDescription().getName()));
			economy = false;
		}

		// load all cuboids
		if (getConfig().isSet("gods.")) {
			for (String cgod : getConfig().getConfigurationSection("gods.").getKeys(false)) {
				String godname = cgod;
				if (getConfig().isSet("gods." + godname + ".bounds.low") && getConfig().isSet("gods." + godname + ".bounds.high")) {
					Cuboid c = new Cuboid(this.getComponentForArena(godname, "bounds.low"), this.getComponentForArena(godname, "bounds.high"));
					god_cube.put(godname, c);
				}
			}
		}

	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]) {
		if (cmd.getName().equalsIgnoreCase("xeph")) {
			if (args.length > 0) {
				String godname = args[0];
				if (sender instanceof Player) {
					Player p = (Player) sender;
					ItemStack stick = new ItemStack(Material.STICK);
					ItemMeta stickmeta = stick.getItemMeta();
					stickmeta.setDisplayName(godname);
					stick.setItemMeta(stickmeta);
					p.getInventory().addItem(stick);
					p.updateInventory();
				}
			}
		}
		return true;
	}

	public void pray(Player p, String godname) {
		if (getConfig().isSet("gods." + godname)) {
			SimpleDateFormat sdfToDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			StringBuilder test = new StringBuilder(sdfToDate.format(new Date()));
			getConfig().set(p.getName() + ".hoursleft", test.toString());
			this.saveConfig();

			if (getConfig().getBoolean("gods." + godname + ".use_economy") && economy) {
				econ.depositPlayer(p.getName(), getConfig().getDouble("gods." + godname + ".econ_reward"));
			}
			if (getConfig().getBoolean("gods." + godname + ".use_itemreward")) {
				for (ItemStack i : parseItems(getConfig().getString("gods." + godname + ".item_reward"))) {
					p.getInventory().addItem(i);
				}
				p.updateInventory();
			}
			if (getConfig().getBoolean("gods." + godname + ".use_cmdreward")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfig().getString("gods." + godname + ".cmd_reward").replaceAll("<player>", p.getName()));
			}
			getServer().broadcastMessage(ChatColor.DARK_GREEN + p.getName() + ChatColor.GREEN + " just prayed to " + ChatColor.DARK_GREEN + godname + ChatColor.GREEN + "!");
		} else {
			p.sendMessage(ChatColor.RED + godname + " couldn't be found!");
		}
	}

	String god = ChatColor.DARK_RED + Character.toString((char) 0x00AB) + "God" + Character.toString((char) 0x00BB);

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.hasBlock()) {
			if (event.getClickedBlock().getType() == Material.SIGN || event.getClickedBlock().getType() == Material.SIGN_POST) {
				Sign s = (Sign) event.getClickedBlock().getState();
				if (s.getLine(0).equalsIgnoreCase(god)) {
					String godname = s.getLine(1);
					if (!getConfig().isSet(event.getPlayer().getName() + ".hoursleft")) {
						pray(event.getPlayer(), godname);
					} else {
						if (checkHours(event.getPlayer())) {
							pray(event.getPlayer(), godname);
						}
					}
				}
			} else {
				if (event.hasItem()) {
					String godname = event.getItem().getItemMeta().getDisplayName();
					if (getConfig().isSet("gods." + godname) && event.getPlayer().hasPermission("xephgods.*")) {
						if (getConfig().isSet("gods." + godname + ".bounds.low")) {
							this.saveComponentForArena(godname, "bounds.high", event.getClickedBlock().getLocation());
							Cuboid c = new Cuboid(this.getComponentForArena(godname, "bounds.low"), this.getComponentForArena(godname, "bounds.high"));
							god_cube.put(godname, c);
						} else if (getConfig().isSet("gods." + godname + ".bounds.high")) {
							this.saveComponentForArena(godname, "bounds.low", event.getClickedBlock().getLocation());
							Cuboid c = new Cuboid(this.getComponentForArena(godname, "bounds.low"), this.getComponentForArena(godname, "bounds.high"));
							god_cube.put(godname, c);
						} else {
							this.saveComponentForArena(godname, "bounds.low", event.getClickedBlock().getLocation());
						}
						event.getPlayer().sendMessage(ChatColor.GREEN + "Successfully set boundary.");
					}
				}
			}
		}
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		if (event.getLine(0) != null) {
			if (event.getLine(0).equalsIgnoreCase("xephgod") && event.getPlayer().hasPermission("xephgods.*")) {
				event.setLine(0, god);
				String godname = event.getLine(1);
				this.getConfig().set("gods." + godname + ".use_economy", this.getConfig().getBoolean("config.use_economy"));
				this.getConfig().set("gods." + godname + ".use_itemreward", this.getConfig().getBoolean("config.use_itemreward"));
				this.getConfig().set("gods." + godname + ".use_cmdreward", this.getConfig().getBoolean("config.use_cmdreward"));
				this.getConfig().set("gods." + godname + ".econ_reward", this.getConfig().getInt("config.econ_reward"));
				this.getConfig().set("gods." + godname + ".item_reward", this.getConfig().getString("config.item_reward"));
				this.getConfig().set("gods." + godname + ".cmd_reward", this.getConfig().getString("config.cmd_reward"));
				this.saveConfig();
				event.getPlayer().sendMessage(ChatColor.GREEN + "Successfully created " + godname + "!");
			}
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		for (String godname : god_cube.keySet()) {
			Cuboid c = god_cube.get(godname);
			if (c.containsLocWithoutY(event.getBlock().getLocation())) {
				getServer().broadcastMessage(ChatColor.DARK_RED + godname + ChatColor.RED + " got angry because " + ChatColor.DARK_RED + event.getPlayer().getName() + ChatColor.RED + " tried destroying his temple!");
				Wolf w = (Wolf) event.getBlock().getWorld().spawnEntity(event.getBlock().getLocation().clone().add(0D, 1D, 0D), EntityType.WOLF);
				w.setAngry(true);
				w.setCustomName(ChatColor.RED + "Hellhound");
			}
		}
	}

	/***
	 * Checks if player is able to use the action again
	 * 
	 * @param p
	 *            Player to check
	 * @return returns true if last use 24 hours ago, false if not
	 */
	public boolean checkHours(Player p) {
		SimpleDateFormat sdfToDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date datecurrent = new Date();
		String daysdate = getConfig().getString(p.getName() + ".hoursleft");
		// p.sendMessage(daysdate);
		Date date1 = null;
		try {
			date1 = sdfToDate.parse(daysdate);
			System.out.println(date1);
		} catch (ParseException ex2) {
			ex2.printStackTrace();
		}
		Integer between = this.hoursBetween(datecurrent, date1);
		getLogger().info(Integer.toString(between));
		if (between > 23 || between < -23) {
			return true;
		} else {
			return false;
		}
	}

	public int hoursBetween(Date d1, Date d2) {
		long differenceMilliSeconds = d2.getTime() - d1.getTime();
		long hours = differenceMilliSeconds / 1000 / 60 / 60;
		return (int) hours;
	}

	// example items: 351:6#ALL_DAMAGE:2#KNOCKBACK:2*1=NAME:LORE;267*1;3*64;3*64
	@SuppressWarnings("unused")
	public ArrayList<ItemStack> parseItems(String rawitems) {
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();

		try {
			String[] a = rawitems.split(";");

			for (String b : a) {
				int nameindex = b.indexOf("=");
				String[] c = b.split("\\*");
				String itemid = c[0];
				String itemdata = "0";
				String[] enchantments_ = itemid.split("#");
				String[] enchantments = new String[enchantments_.length - 1];
				if (enchantments_.length > 1) {
					for (int i = 1; i < enchantments_.length; i++) {
						enchantments[i - 1] = enchantments_[i];
					}
				}
				itemid = enchantments_[0];
				String[] d = itemid.split(":");
				if (d.length > 1) {
					itemid = d[0];
					itemdata = d[1];
				}
				String itemamount = c[1];
				if (nameindex > -1) {
					itemamount = c[1].substring(0, c[1].indexOf("="));
				}
				ItemStack nitem = new ItemStack(Integer.parseInt(itemid), Integer.parseInt(itemamount), (short) Integer.parseInt(itemdata));
				ItemMeta m = nitem.getItemMeta();
				for (String enchant : enchantments) {

					String[] e = enchant.split(":");
					String ench = e[0];
					String lv = "1";
					if (e.length > 1) {
						lv = e[1];
					}
					if (Enchantment.getByName(ench) != null) {
						m.addEnchant(Enchantment.getByName(ench), Integer.parseInt(lv), true);
					}
				}

				if (nameindex > -1) {
					String namelore = b.substring(nameindex + 1);
					String name = "";
					String lore = "";
					int i = namelore.indexOf(":");
					if (i > -1) {
						name = namelore.substring(0, i);
						lore = namelore.substring(i + 1);
					} else {
						name = namelore;
					}
					m.setDisplayName(name);
					m.setLore(Arrays.asList(lore));
				}
				nitem.setItemMeta(m);
				ret.add(nitem);
			}
			if (ret == null) {
				getLogger().severe("Found invalid class in config!");
			}
		} catch (Exception e) {
			ret.add(new ItemStack(Material.STAINED_GLASS_PANE));
			System.out.println("Failed to load class items: " + e.getMessage() + " at [1] " + e.getStackTrace()[1].getLineNumber() + " [0] " + e.getStackTrace()[0].getLineNumber());
			ItemStack rose = new ItemStack(Material.RED_ROSE);
			ItemMeta im = rose.getItemMeta();
			im.setDisplayName(ChatColor.RED + "Sowwy, failed to load class.");
			rose.setItemMeta(im);
			ret.add(rose);
		}

		return ret;
	}

	public void saveComponentForArena(String godname, String component, Location comploc) {
		String base = "gods." + godname + "." + component;
		getConfig().set(base + ".world", comploc.getWorld().getName());
		getConfig().set(base + ".location.x", comploc.getX());
		getConfig().set(base + ".location.y", comploc.getY());
		getConfig().set(base + ".location.z", comploc.getZ());
		getConfig().set(base + ".location.yaw", comploc.getYaw());
		getConfig().set(base + ".location.pitch", comploc.getPitch());
		saveConfig();
	}

	public Location getComponentForArena(String godname, String component) {
		if (getConfig().isSet("gods." + godname)) {
			String base = "gods." + godname + "." + component;
			return new Location(Bukkit.getWorld(getConfig().getString(base + ".world")), getConfig().getDouble(base + ".location.x"), getConfig().getDouble(base + ".location.y"), getConfig().getDouble(base + ".location.z"), (float) getConfig().getDouble(base + ".location.yaw"), (float) getConfig().getDouble(base + ".location.pitch"));
		}
		return null;
	}

}
