package com.example.katanas.item;

import com.example.katanas.util.ComboTracker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Base class for all katanas.
 *
 * Handles: faster attack speed (via material), on-hit hook, right-click active ability with
 * cooldown + durability cost, ambient particles while held, swing particles, and tooltips.
 *
 * Subclasses implement the four abstract hooks only.
 */
public abstract class KatanaItem extends SwordItem {
    private static final int AMBIENT_INTERVAL_TICKS = 4;

    private final int cooldownTicks;
    private final int abilityDurabilityCost;

    protected KatanaItem(KatanaMaterial material, Rarity rarity, boolean glint,
                         int cooldownTicks, int abilityDurabilityCost) {
        super(material, buildSettings(material, rarity, glint));
        this.cooldownTicks = cooldownTicks;
        this.abilityDurabilityCost = abilityDurabilityCost;
    }

    private static Item.Settings buildSettings(KatanaMaterial material, Rarity rarity, boolean glint) {
        Item.Settings settings = new Item.Settings()
                .rarity(rarity)
                .attributeModifiers(SwordItem.createAttributeModifiers(material, 3, material.getAttackSpeed()));
        if (material.isFireproof()) {
            settings.fireproof();
        }
        if (glint) {
            settings.component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return settings;
    }

    // ------------------------------------------------------------------ hooks

    /** Passive effect, called server-side after a successful melee hit. */
    protected abstract void onKatanaHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker);

    /** Right-click ability. Return true if it fired (cooldown + durability are then applied). */
    protected abstract boolean activateAbility(ServerWorld world, PlayerEntity player, ItemStack stack);

    /** Called every few ticks while this katana is in the selected hotbar slot. */
    protected abstract void ambientParticles(ServerWorld world, LivingEntity holder);

    /**
     * Called once at the start of every attack swing while this katana is selected.
     * technique: 0 kesa-giri, 1 yoko-giri, 2 gyaku-kesa, 3 tsuki (same order as the client combo).
     */
    protected abstract void swingParticles(ServerWorld world, LivingEntity holder, int technique);

    /**
     * Client only: game time (player.age) when the right-click ability was last used, so the first-person
     * animator can tell an ability swing from an attack swing.
     */
    public static float clientAbilityStartAge = -1000f;

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    // --------------------------------------------------------------- behaviour

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.postHit(stack, target, attacker);
        if (attacker.getWorld() instanceof ServerWorld serverWorld) {
            onKatanaHit(serverWorld, stack, target, attacker);
        }
        return result;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            clientAbilityStartAge = player.age; // client: start the ability animation, server decides the effect
            return TypedActionResult.success(stack);
        }
        if (!activateAbility(serverWorld, player, stack)) {
            return TypedActionResult.fail(stack);
        }
        ComboTracker.abilityUsed(player.getUuid(), world.getTime());

        player.getItemCooldownManager().set(this, cooldownTicks);
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        stack.damage(abilityDurabilityCost, player,
                hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!selected || !(world instanceof ServerWorld serverWorld) || !(entity instanceof LivingEntity holder)) {
            return;
        }
        if (world.getTime() % AMBIENT_INTERVAL_TICKS == 0) {
            ambientParticles(serverWorld, holder);
        }
        // handSwingTicks == 0 is seen exactly once per swing, regardless of tick ordering.
        if (holder.handSwinging && holder.handSwingTicks == 0
                && !ComboTracker.isAbilitySwing(holder.getUuid(), world.getTime())) {
            int technique = ComboTracker.next(holder.getUuid(), world.getTime());
            swingParticles(serverWorld, holder, technique);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        String key = this.getTranslationKey();
        tooltip.add(Text.translatable(key + ".passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(key + ".active").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.elementalkatanas.cooldown", cooldownTicks / 20)
                .formatted(Formatting.DARK_GRAY));
    }
}
