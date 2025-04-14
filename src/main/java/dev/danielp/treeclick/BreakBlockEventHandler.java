package dev.danielp.treeclick;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffectType;
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
		boolean canceled = false;
		blockQueue.add(new SimpleEntry<Block, Integer>(e.getBlock(), 0));
		visitedBlocks.add(e.getBlock());
		short log_count = 0, leaf_count = 0;
		
		Material log_type = e.getBlock().getType();
		Material leaf_type = ConfigManager.getLeafFromLog(log_type);
		Material sapling_type = ConfigManager.getSaplingFromLog(log_type);

		while (!blockQueue.isEmpty() && !canceled) {
			SimpleEntry<Block, Integer> entry = blockQueue.remove();
			Block b = entry.getKey();
			int depth = entry.getValue();
			if (b.getType().equals(log_type)) {
				log_count++;
			} else if (b.getType().equals(leaf_type)) {
				leaf_count++;
			}
//			b.getWorld().spawnParticle(Particle.DUST, b.getLocation().add(0.5, 0.5, 0.5), 100, new Particle.DustOptions(switch (depth) {
//				case 0 -> Color.LIME;
//				case 1 -> Color.GREEN;
//				case 2 -> Color.ORANGE;
//				case 3 -> Color.YELLOW;
//				case 4 -> Color.RED;
//				default -> Color.MAROON;
//			}, 128.0f));

			blocksToBreak.add(b);

			for (Block adjacent : getAdjacentBlocks(b)) {
				if (visitedBlocks.contains(adjacent))
					continue;
				visitedBlocks.add(adjacent);
				if (adjacent.getType().equals(log_type) && depth < 1) {
					blockQueue.add(new SimpleEntry<Block, Integer>(adjacent, 0));
				} else if (adjacent.getType().equals(leaf_type) && depth < 3) {
					blockQueue.add(new SimpleEntry<Block, Integer>(adjacent, depth + 1));
				} else if (ConfigManager.getSafetyBlocks().contains(adjacent.getType())) {
					canceled = true;
					e.getPlayer().sendMessage("Canceled");
				}
			}

		}
		
		if (log_count > leaf_count)
			canceled = true;

		// Start breaking the stem
//		while (!logQueue.isEmpty()) {
//			Block b = logQueue.remove();
//
//			if (isLog(b.getType())) {
//				blocksToBreak.add(b);
//				for (Block adjacent : getAdjacentBlocks(b)) {
//					if (visitedBlocks.contains(adjacent))
//						continue;
//					visitedBlocks.add(adjacent);
//					if (!isLog(adjacent.getType()))
//						continue;
//					logQueue.add(adjacent);
//				}
//			}
//
//		}

		// Grab the first leaves
//		Queue<SimpleEntry<Block, Integer>> leafQueue = new LinkedList<SimpleEntry<Block, Integer>>();
//		// Entry is to make sure only leaves of a certain distance away are destroyed
//
//		for (Block b : visitedBlocks) {
//			for (Block b1 : getHorizontallyAdjacentBlocks(b)) {
//				if (isLeaf(b1.getType())) {
//					leafQueue.add(new SimpleEntry<Block, Integer>(b1, 0));
//					visitedBlocks.add(b1);
//				}
//			}
//		}

		// Start iterating until no more leaves are found within reasonable range
//		while (!leafQueue.isEmpty()) {
//			SimpleEntry<Block, Integer> entry = leafQueue.remove();
//			Block b = entry.getKey();
//			int d = entry.getValue();
//
//			blocksToBreak.add(b);
//
//			if (d < 5) {
//				for (Block adjacent : getAdjacentBlocks(b)) {
//					if (visitedBlocks.contains(adjacent))
//						continue;
//					if (!isLeaf(adjacent.getType()))
//						continue;
//					leafQueue.add(new SimpleEntry<>(adjacent, d + 1));
//					visitedBlocks.add(adjacent);
//				}
//			}
//		}

		// Player's axe
		ItemStack playerAxe = e.getPlayer().getInventory().getItemInMainHand();
		double unbreaking_level = playerAxe.getEnchantmentLevel(Enchantment.UNBREAKING);
		double efficiency_level = playerAxe.getEnchantmentLevel(Enchantment.EFFICIENCY);
		int haste_level = e.getPlayer().hasPotionEffect(PotionEffectType.HASTE)
				? e.getPlayer().getPotionEffect(PotionEffectType.HASTE).getAmplifier()
				: 0;
		org.bukkit.inventory.meta.Damageable d = (Damageable) playerAxe.getItemMeta();

		double tool_multiplier = switch (playerAxe.getType()) {
		case WOODEN_AXE -> 2.0;
		case STONE_AXE -> 4.0;
		case IRON_AXE -> 6.0;
		case DIAMOND_AXE -> 8.0;
		case NETHERITE_AXE -> 9.0;
		case GOLDEN_AXE -> 12.0;
		default -> 1.0;
		};
		double cutting_speed = 3.0 / tool_multiplier;

		if (efficiency_level > 0)
			cutting_speed /= (Math.pow(efficiency_level, 2) + 1);
		if (haste_level > 0)
			cutting_speed /= (1 + 0.2 * haste_level);
		final int cutting_speed_ticks = (int) Math.ceil(cutting_speed * 20);

		blocksToBreak.sort((a, b) -> {
			if (isLog(a.getType()) && !isLog(b.getType()))
				return -1;
			if (!isLog(a.getType()) && isLog(b.getType()))
				return 1;
			return Integer.compare(a.getY(), b.getY());
		});

		// Break all blocks and leaves
		if (canceled)
			blocksToBreak.clear();
	
		
		for (int i = 0; i < blocksToBreak.size(); i++) {
			Block b = blocksToBreak.get(i);
			final long mult = isLog(b.getType()) ? 2 : 1;
			Random r = new Random();
			new BukkitRunnable() {
				@Override
				public void run() {
					// Break the block
					b.breakNaturally();
					// Play appropriate sound for block breaking
					b.getWorld().playSound(b.getLocation(),
							mult == 2 ? Sound.BLOCK_WOOD_BREAK : Sound.BLOCK_GRASS_BREAK, 0.1f, 1f);

					// Damage the player's axe and account for unbreaking enchant
					if (mult == 1)
						return;

					if (r.nextDouble() < (1 / (1 + unbreaking_level)))
						d.setDamage(d.getDamage() + 1);
					playerAxe.setItemMeta(d);
				}
			}.runTaskLater(Plugin, isLog(b.getType()) ? i * cutting_speed_ticks : i);
		}

	}

	public static Block[] getAdjacentBlocks(Block b) {
//		return new HashSet<Block>(Arrays.asList(b.getRelative(BlockFace.NORTH), b.getRelative(BlockFace.SOUTH),
//				b.getRelative(BlockFace.EAST), b.getRelative(BlockFace.WEST), b.getRelative(BlockFace.UP),
//				b.getRelative(BlockFace.DOWN)));

		return new Block[] {
				   b.getRelative(1, 0, 0), b.getRelative(-1, 0, 0), b.getRelative(0, 0, 1), b.getRelative(0, 0, -1), 
				   b.getRelative(0, 1, 0), b.getRelative(0, -1, 0), 
				   b.getRelative(1, 0, 1), b.getRelative(1, 0, -1), b.getRelative(-1, 0, 1), b.getRelative(-1, 0, -1),
				   b.getRelative(1, 1, 0), b.getRelative(-1, 1, 0), b.getRelative(0, 1, 1), b.getRelative(0, 1, -1),
				   b.getRelative(1, 1, 1), b.getRelative(1, 1, -1), b.getRelative(-1, 1, 1), b.getRelative(-1, 1, -1)
				};
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
