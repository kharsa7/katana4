package com.example.katanas.item;

import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Supplier;

/**
 * Tier definitions. Final sword damage = 3 (base) + attackDamage.
 *  TIER_1: 6.0 damage, attack speed 2.0  (iron sword: 6.0 / 1.6)
 *  TIER_2: 9.0 damage, attack speed 2.2  (netherite sword: 8.0 / 1.6)
 */
public enum KatanaMaterial implements ToolMaterial {
    TIER_1(BlockTags.INCORRECT_FOR_IRON_TOOL, 650, 6.0f, 3.0f, -2.0f, 14, false,
            () -> Ingredient.ofItems(Items.IRON_INGOT)),
    TIER_2(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2800, 9.0f, 6.0f, -1.8f, 18, true,
            () -> Ingredient.ofItems(Items.NETHERITE_INGOT));

    private final TagKey<Block> inverseTag;
    private final int durability;
    private final float miningSpeed;
    private final float attackDamage;
    private final float attackSpeed;
    private final int enchantability;
    private final boolean fireproof;
    private final Supplier<Ingredient> repair;

    KatanaMaterial(TagKey<Block> inverseTag, int durability, float miningSpeed, float attackDamage,
                   float attackSpeed, int enchantability, boolean fireproof, Supplier<Ingredient> repair) {
        this.inverseTag = inverseTag;
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.enchantability = enchantability;
        this.fireproof = fireproof;
        this.repair = repair;
    }

    @Override public int getDurability() { return durability; }
    @Override public float getMiningSpeedMultiplier() { return miningSpeed; }
    @Override public float getAttackDamage() { return attackDamage; }
    @Override public TagKey<Block> getInverseTag() { return inverseTag; }
    @Override public int getEnchantability() { return enchantability; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }

    /** Attack speed modifier (added to the player's base 4.0). */
    public float getAttackSpeed() { return attackSpeed; }
    public boolean isFireproof() { return fireproof; }
}
