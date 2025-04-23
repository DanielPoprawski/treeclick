package dev.danielp.treeclick;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Axis;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BreakBlockEventHandler implements Listener {

	private main Plugin = main.getPlugin(main.class);
	final HashSet<Material> LOGS = ConfigManager.getLogs();
	final HashSet<Material> LEAVES = ConfigManager.getLeaves();
	final HashSet<Material> AXES = new HashSet<Material>(Arrays.asList(Material.WOODEN_AXE, Material.STONE_AXE,
			Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));

	@EventHandler
	public void blockBreakEvent(BlockBreakEvent e) {
		// Check to make sure the event should be firing
		if (!ConfigManager.isEnabled())
			return;
		if (!ConfigManager.isWorldEnabled(e.getBlock().getWorld()))
			return;
		if (!isLog(e.getBlock().getType()))
			return;
		if (!isAxe(e.getPlayer().getInventory().getItemInMainHand().getType()) && ConfigManager.requireAxe())
			return;
		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL)
			return;

		Set<Block> visitedBlocks = new HashSet<Block>();
		Queue<SimpleEntry<Block, Integer>> blockQueue = new LinkedList<SimpleEntry<Block, Integer>>();
		List<Block> blocksToBreak = new ArrayList<Block>();
		blockQueue.add(new SimpleEntry<Block, Integer>(e.getBlock(), 0));
		visitedBlocks.add(e.getBlock());
		short log_count = 0, leaf_count = 0;

		Material log_type = e.getBlock().getType();
		Material leaf_type = ConfigManager.getLeafFromLog(log_type);
		Material sapling_type = ConfigManager.getSaplingFromLog(log_type);

		while (!blockQueue.isEmpty()) {
			SimpleEntry<Block, Integer> entry = blockQueue.remove();
			Block b = entry.getKey();
			int depth = entry.getValue();
			if (b.getType().equals(log_type)) {
				log_count++;
			} else if (b.getType().equals(leaf_type)) {
				leaf_count++;
			}

			blocksToBreak.add(b);

			for (Block adjacent : getNearbyBlocks(b)) {
				if (visitedBlocks.contains(adjacent))
					continue;
				visitedBlocks.add(adjacent);
				if (adjacent.getType().equals(log_type)) {
					if (depth < 1 || ((Orientable) adjacent.getBlockData()).getAxis() != Axis.Y) {
						blockQueue.add(new SimpleEntry<Block, Integer>(adjacent, 0));
					} else if (depth >= 1 && depth <= 3) {
						blockQueue.add(new SimpleEntry<Block, Integer>(adjacent, depth + 1));
					}

				} else if (adjacent.getType().equals(leaf_type) && depth < 3) {
					blockQueue.add(new SimpleEntry<Block, Integer>(adjacent, depth + 1));
				} else if (ConfigManager.getSafetyBlocks().contains(adjacent.getType())) {
					return;
				}
			}

		}

		if (leaf_count < 16 && leaf_count < (2 * log_count) && ConfigManager.requireLeaves()) {
			return;
		}

		blocksToBreak.sort((a, b) -> {
			if (isLog(a.getType()) && !isLog(b.getType()))
				return -1;
			if (!isLog(a.getType()) && isLog(b.getType()))
				return 1;
			return Integer.compare(a.getY(), b.getY());
		});

		// Break all blocks and leaves

		Player player = e.getPlayer();
		ItemStack playerAxe = e.getPlayer().getInventory().getItemInMainHand();

		// Checks if blocks are to break instantly or sequentially
		if (ConfigManager.instantChop()) {
			for (Block block : blocksToBreak) {
				if (isLog(block.getType()))
					Util.damageAxe(player, playerAxe);
				Util.handleBreak(player, block);
			}
		} else {
			int cutting_speed = Util.getCuttingSpeed(player, playerAxe);
			for (int i = 0; i < blocksToBreak.size(); i++) {
				Block b = blocksToBreak.get(i);
				final long mult = isLog(b.getType()) ? 2 : 1;
				new BukkitRunnable() {
					@Override
					public void run() {
						// Break the block
						Util.handleBreak(player, b);
						// Play appropriate sound for block breaking
						b.getWorld().playSound(b.getLocation(),
								mult == 2 ? Sound.BLOCK_WOOD_BREAK : Sound.BLOCK_GRASS_BREAK, 0.1f, 1f);
						// Damage the player's axe and account for unbreaking enchant
						if (mult == 1)
							return;
						Util.damageAxe(player, playerAxe);
					}
				}.runTaskLater(Plugin, isLog(b.getType()) ? i * cutting_speed : i);
			}
		}

		if (ConfigManager.replantSaplings()) {
			int maxY = 256;
			HashSet<Block> lowestYBlocks = new HashSet<Block>();
			for (Block b : blocksToBreak) {
				if (b.getY() < maxY) {
					maxY = b.getY();
					lowestYBlocks.clear();
					lowestYBlocks.add(b);
				} else if (b.getY() == maxY)
					lowestYBlocks.add(b);
			}
			new BukkitRunnable() {

				@Override
				public void run() {
					lowestYBlocks.forEach(b -> {
						if (b.getRelative(BlockFace.DOWN).getType().equals(Material.GRASS_BLOCK)
								|| b.getRelative(BlockFace.DOWN).getType().equals(Material.DIRT)
								|| b.getRelative(BlockFace.DOWN).getType().equals(Material.PODZOL))
							b.setType(sapling_type);
						else
							b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5),
									new ItemStack(sapling_type, 1));
					});
				}
			}.runTaskLater(Plugin, 2);
		}

	}

	public static Block[] getNearbyBlocks(Block b) {
		return new Block[] { b.getRelative(1, 0, 0), b.getRelative(-1, 0, 0), b.getRelative(0, 0, 1),
				b.getRelative(0, 0, -1), b.getRelative(0, 1, 0), b.getRelative(0, -1, 0), b.getRelative(1, 0, 1),
				b.getRelative(1, 0, -1), b.getRelative(-1, 0, 1), b.getRelative(-1, 0, -1), b.getRelative(1, 1, 0),
				b.getRelative(-1, 1, 0), b.getRelative(0, 1, 1), b.getRelative(0, 1, -1), b.getRelative(1, 1, 1),
				b.getRelative(1, 1, -1), b.getRelative(-1, 1, 1), b.getRelative(-1, 1, -1) };
	}

	public static Block[] getAdjacent(Block b) {
		return new Block[] { b.getRelative(BlockFace.UP), b.getRelative(BlockFace.DOWN), b.getRelative(BlockFace.NORTH),
				b.getRelative(BlockFace.WEST), b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.SOUTH) };
	}

	public static HashSet<Block> getHorizontallyAdjacentBlocks(Block b) {
		return new HashSet<Block>(Arrays.asList(b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.SOUTH),
				b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.WEST)));
	}

	public boolean isLog(Material m) {
		return LOGS.contains(m);
	}

	public boolean isLeaf(Material m) {
		return LEAVES.contains(m);
	}

	public boolean isAxe(Material m) {
		return AXES.contains(m);
	}

}
