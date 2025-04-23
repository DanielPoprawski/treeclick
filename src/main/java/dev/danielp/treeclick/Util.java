package dev.danielp.treeclick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffectType;

public class Util {

	private static Random r;

	static {
		r = new Random();
	}

	public static void handleBreak(Player p, Block b) {
		ItemStack ignore_silk_touch = new ItemStack(p.getInventory().getItemInMainHand());
		ignore_silk_touch.removeEnchantment(Enchantment.SILK_TOUCH);

		if (!ConfigManager.dropToInventory()) {
			b.breakNaturally(ignore_silk_touch);
		} else {
			ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
			drops.addAll(b.getDrops(ignore_silk_touch));
			b.setType(Material.AIR);
			drops.forEach((is) -> {
				HashMap<Integer, ItemStack> leftovers = p.getInventory().addItem(is);
				if (!leftovers.isEmpty()) {
					leftovers.values()
							.forEach((left_over) -> p.getWorld().dropItemNaturally(p.getLocation(), left_over));
				}
			});
		}
	}

	public static int getCuttingSpeed(Player p, ItemStack axe) {
		double efficiency_level = axe.getEnchantmentLevel(Enchantment.EFFICIENCY);
		int haste_level = p.hasPotionEffect(PotionEffectType.HASTE)
				? p.getPotionEffect(PotionEffectType.HASTE).getAmplifier()
				: 0;

		double tool_multiplier = switch (axe.getType()) {
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
		return (int) Math.ceil(cutting_speed * 20);
	}

	public static boolean damageAxe(Player p, ItemStack axe) {
		Damageable d = (Damageable) axe.getItemMeta();
		double unbreaking_level = axe.getEnchantmentLevel(Enchantment.UNBREAKING);

		if (d.hasMaxDamage()) {
			if (d.getDamage() >= d.getMaxDamage() - 1) {
				p.sendMessage("Your axe is about to break!");
				p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
				return false;
			}
		}

		if (r.nextDouble() < (1 / (1 + unbreaking_level))) {
			d.setDamage(d.getDamage() + 1);
		}
		axe.setItemMeta(d);

		return true;
	}

}
