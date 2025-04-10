package dev.danielp.treeclick;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BreakBlockEventHandler implements Listener {

	static final HashSet<Material> LOGS = new HashSet<Material>(Arrays.asList(Material.OAK_LOG, Material.SPRUCE_LOG,
			Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG,
			Material.CHERRY_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM, Material.PALE_OAK_LOG));

	static final HashSet<Material> AXES = new HashSet<Material>(Arrays.asList(Material.WOODEN_AXE, Material.STONE_AXE,
			Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
	
	static final HashSet<Material> LEAVES = new HashSet<Material>(Arrays.asList(Material.OAK_LEAVES, Material.SPRUCE_LEAVES,
			Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES,
			Material.CHERRY_LEAVES, Material.CRIMSON_STEM, Material.WARPED_STEM, Material.PALE_OAK_LEAVES));
	
	

	@EventHandler
	public void blockBreakEvent(BlockBreakEvent e) {

		if (!isLog(e.getBlock()))
			return;
		
		if (!isAxe(e.getPlayer().getInventory().getItemInMainHand().getType()) && !isAxe(e.getPlayer().getInventory().getItemInOffHand().getType()))
			return;
		
		Set<Block> visitedLogs = new HashSet<Block>();
		Queue<Block> logQueue = new LinkedList<Block>();
		List<Block> blocksToBreak = new ArrayList<Block>();

		logQueue.add(e.getBlock());
		visitedLogs.add(e.getBlock());

		int iterationCount = 0;

		while (!logQueue.isEmpty() && iterationCount < 100000) {
			iterationCount++;
			Block b = logQueue.remove();

			if (isLog(b)) {
				blocksToBreak.add(b);
				for (Block adjacent : getAdjacentBlocks(b)) {
					iterationCount++;
					if (visitedLogs.contains(adjacent))
						continue;
					if (!isLog(adjacent))
						continue;
					logQueue.add(adjacent);
					visitedLogs.add(adjacent);
				}
			}
			
		}
		
		Queue<SimpleEntry<Block, Integer>> leafQueue = new LinkedList<SimpleEntry<Block, Integer>>();
		Set<Block> visitedLeaves = new HashSet<Block>();
		
		for (Block b : visitedLogs) {
			for (Block b1: getHorizontallyAdjacentBlocks(b)) {
				iterationCount++;
				if (isLeaf(b1)) {
					leafQueue.add(new SimpleEntry<Block, Integer>(b1, 0));
					visitedLeaves.add(b1);
				}
			}
		}
		
		while (!leafQueue.isEmpty()) {
			iterationCount++;
			SimpleEntry<Block, Integer> entry = leafQueue.remove();
			Block b = entry.getKey();
			int d = entry.getValue();
			
			blocksToBreak.add(b);
			
			if (d < 5) {
				for (Block adjacent : getAdjacentBlocks(b)) {
					if (visitedLeaves.contains(adjacent))
						continue;
					if (!isLeaf(adjacent))
						continue;
					leafQueue.add(new SimpleEntry<>(adjacent, d+1));
					visitedLeaves.add(adjacent);
				}
			}
		}
		
		
		
		blocksToBreak.forEach((Block b) -> {
			b.breakNaturally();
		});

		e.getPlayer().sendMessage("Iteration count: " + iterationCount);

	}

	public static HashSet<Block> getAdjacentBlocks(Block b) {
		return new HashSet<Block>(Arrays.asList(b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.SOUTH),
				b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.WEST), b.getRelative(BlockFace.UP),
				b.getRelative(BlockFace.DOWN)));
	}
	
	public static HashSet<Block> getHorizontallyAdjacentBlocks(Block b) {
		return new HashSet<Block>(Arrays.asList(b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.SOUTH),
				b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.WEST)));
	}
	

	public static boolean isLog(Block b) {
		return LOGS.contains(b.getType());
	}

	public static boolean isLog(Material m) {
		return LOGS.contains(m);
	}
	
	public static boolean isLeaf(Block b) {
		return LEAVES.contains(b.getType());
	}

	public static boolean isLeaf(Material m) {
		return LEAVES.contains(m);
	}
	
	public static boolean isAxe(Material m) {
		return AXES.contains(m);
	}

}
