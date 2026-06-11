package Aow.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import Aow.items.PickaxeEat;

public class ModItems {

    // Je précise ici que mon MODID = aow
    public static final String MODID = "aow";

    // ── Registres ────────────────────────────────────────────────────────────────
    // Registre des items du mod
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // je crée le registre de création de mon onglet de mod créatif
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ── Items ─────────────────────────────────────────────────────────────────────
    // Ajoute tes items ici, une ligne chacun.

    // Tiers.DIAMOND = 4 dmg de base ; bonus 16 → 20 dmg total
    public static final RegistryObject<Item> DIAMOND_KILLER_SWORD =
            registerSword("diamond_killer_sword", Tiers.DIAMOND, 16, -2.4f);
    public static final RegistryObject<Item> AOW_SWORD_1 =
            registerSword("aow_sword1", Tiers.NETHERITE , 50, 6.0f);
    public static final RegistryObject<Item> AOW_TOOLTEST =
            ITEMS.register("aow_pickaxe_eat", PickaxeEat::new);

    // ── Onglet créatif ────────────────────────────────────────────────────────────

    public static final RegistryObject<CreativeModeTab> AOW_TAB = CREATIVE_MODE_TABS.register(
            "aow_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    // Icône : épée en netherite vanilla
                    .icon(() -> new ItemStack(Items.NETHERITE_SWORD))
                    .title(Component.translatable("itemGroup.aow.aow_tab"))
                    // Tous les items du mod sont ajoutés automatiquement
                    .displayItems((parameters, output) ->
                            ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build()
    );

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Enregistre une épée en une ligne. */
    private static RegistryObject<Item> registerSword(
            String name, Tier tier, int damageBonus, float attackSpeed) {
        return ITEMS.register(name,
                () -> new SwordItem(tier, damageBonus, attackSpeed, new Item.Properties()));
    }

    // ── Enregistrement ────────────────────────────────────────────────────────────

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}