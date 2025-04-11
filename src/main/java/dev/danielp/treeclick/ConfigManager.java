package dev.danielp.treeclick;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigManager {

	private static Plugin myPlugin;
	private static File configYml;
	private static File configYaml;
	private static FileConfiguration config;

	static {
		ConfigManager.myPlugin = main.getPlugin(main.class);
		configYml = new File(myPlugin.getDataFolder(), "config.yml");
		configYaml = new File(myPlugin.getDataFolder(), "config.yaml");
		try {
			if (!configYml.exists() && !configYaml.exists())
				myPlugin.saveDefaultConfig();
			config = myPlugin.getConfig();
		} catch (Exception e) {
			throw e;
		}
	}

	public static final boolean isEnabled() {
		return config.getBoolean("enabled");
	}

	public static final boolean isWorldEnabled(World w) {
		Boolean allWorlds = config.getBoolean("worlds.all_worlds");
		if (allWorlds)
			return true;
		
		for (String world : config.getStringList("worlds.enabled_worlds")) {
			if (myPlugin.getServer().getWorld(world).equals(w))
				return true;
		}
		return false;
	}

	public static final HashSet<Material> getLogs() {
		ConfigurationSection trees = config.getConfigurationSection("cutting.trees");
		HashSet<Material> logList = new HashSet<Material>();

		for (String treeName : trees.getKeys(false)) {
			String log_name = trees.getConfigurationSection(treeName).getString("log_block");
			logList.add(Material.getMaterial(log_name));
		}
		return logList;
	}

	public static final HashSet<Material> getLeaves() {
		ConfigurationSection trees = config.getConfigurationSection("cutting.trees");
		HashSet<Material> leafList = new HashSet<Material>();

		for (String treeName : trees.getKeys(false)) {
			String leaf_name = trees.getConfigurationSection(treeName).getString("leaf_block");
			leafList.add(Material.getMaterial(leaf_name));
		}
		return leafList;
	}

	public static final HashSet<Material> getSafetyBlocks() {
		List<String> blockList = config.getStringList("safety.safe_blocks");
		HashSet<Material> safetyBlocks = new HashSet<Material>();
		for (String s : blockList) {
			safetyBlocks.add(Material.getMaterial(s));
		}
		return safetyBlocks;
	}

	public static final boolean requireAxe() {
		return config.getBoolean("cutting.require_axe");
	}

	public static final boolean dropToInventory() {
		return config.getBoolean("cutting.drop_to_inventory");
	}

	public static final boolean instantChop() {
		return config.getBoolean("cutting.instant_chop");
	}

	public static final boolean useToolSpeed() {
		return config.getBoolean("cutting.use_tool_speed");
	}

	public static final int getCuttingSpeed() {
		return config.getInt("cutting.cutting_speed");
	}

}
