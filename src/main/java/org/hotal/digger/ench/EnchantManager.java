package org.hotal.digger.ench;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnchantManager {

    public void applyEfficiencyEnchant(Player player, int blocksMined) {
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().toString().endsWith("_PICKAXE") && !hasAppropriateEnchant(tool, blocksMined)) {
            Enchantment enchantment = Enchantment.DIG_SPEED;
            int enchantLevel = getEnchantLevelForBlocksMined(blocksMined);

            if (enchantLevel > 0) {
                tool.addEnchantment(enchantment, enchantLevel);
            }
        }
    }

    private boolean hasAppropriateEnchant(ItemStack tool, int blocksMined) {
        int expectedLevel = getEnchantLevelForBlocksMined(blocksMined);
        return tool.getEnchantmentLevel(Enchantment.DIG_SPEED) >= expectedLevel;
    }

    private int getEnchantLevelForBlocksMined(int blocksMined) {
        if (blocksMined >= 50000) return 5;
        if (blocksMined >= 20000) return 4;
        if (blocksMined >= 12000) return 3;
        if (blocksMined >= 8000) return 2;
        return 0;
    }
}
