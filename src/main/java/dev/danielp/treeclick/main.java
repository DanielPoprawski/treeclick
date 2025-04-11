package dev.danielp.treeclick;

import org.bukkit.plugin.java.JavaPlugin;

public class main extends JavaPlugin {
	
	
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new BreakBlockEventHandler(), this);
	}
	
	public void onDisable() {
		
	}

}
