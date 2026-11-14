package com.example.katanas.registry;

import com.example.katanas.ElementalKatanas;
import com.example.katanas.item.FlameKatana;
import com.example.katanas.item.FrostKatana;
import com.example.katanas.item.GravityKatana;
import com.example.katanas.item.LightningKatana;
import com.example.katanas.item.SpaceKatana;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
    private ModItems() {}

    // Tier 1 (iron-based)
    public static final Item FROST_KATANA = register("frost_katana", new FrostKatana());
    public static final Item FLAME_KATANA = register("flame_katana", new FlameKatana());
    public static final Item LIGHTNING_KATANA = register("lightning_katana", new LightningKatana());

    // Tier 2 (netherite-based)
    public static final Item GRAVITY_KATANA = register("gravity_katana", new GravityKatana());
    public static final Item SPACE_KATANA = register("space_katana", new SpaceKatana());

    public static final RegistryKey<ItemGroup> KATANA_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, Identifier.of(ElementalKatanas.MOD_ID, "katanas"));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(ElementalKatanas.MOD_ID, name), item);
    }

    /** Called from the mod initializer; touching this class also runs the static item registrations above. */
    public static void register() {
        Registry.register(Registries.ITEM_GROUP, KATANA_GROUP_KEY, FabricItemGroup.builder()
                .icon(() -> new ItemStack(FROST_KATANA))
                .displayName(Text.translatable("itemGroup.elementalkatanas.katanas"))
                .entries((context, entries) -> {
                    entries.add(FROST_KATANA);
                    entries.add(FLAME_KATANA);
                    entries.add(LIGHTNING_KATANA);
                    entries.add(GRAVITY_KATANA);
                    entries.add(SPACE_KATANA);
                })
                .build());
        ElementalKatanas.LOGGER.info("Registered 5 katanas.");
    }
}
