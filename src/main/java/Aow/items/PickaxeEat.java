package Aow.items;

import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Pioche en diamant qui peut se manger.
 * Perd de la durabilité au lieu d'être consommée.
 */
public class PickaxeEat extends PickaxeItem {

    /** Durabilité perdue à chaque bouchée */
    private static final int DURABILITY_COST = 100;

    public PickaxeEat() {
        super(
                Tiers.NETHERITE,
                1,
                -2.8F,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(5000)
                        .rarity(Rarity.EPIC)
                        .food(
                                new FoodProperties.Builder()
                                        .nutrition(8)
                                        .saturationMod(0.8F)
                                        .build()
                        )
        );
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /**
     * Appelé quand le joueur finit de manger l'item.
     * Applique les effets de la nourriture ET retire de la durabilité.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Applique d'abord les effets de la nourriture (soif, saturation)
        if (entity instanceof Player player) {
            player.getFoodData().eat(
                    stack.getFoodProperties(entity).getNutrition(),
                    stack.getFoodProperties(entity).getSaturationModifier()
            );
        }

        // Retire de la durabilité au lieu de consommer l'item
        stack.hurtAndBreak(DURABILITY_COST, entity, (e) -> {
            // Callback quand l'item se brise (optionnel)
            e.broadcastBreakEvent(entity.getUsedItemHand());
        });

        return stack;
    }
}