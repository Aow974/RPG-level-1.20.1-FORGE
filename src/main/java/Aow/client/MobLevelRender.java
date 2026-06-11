package Aow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.text.DecimalFormat;


/**
 * Renderer consolidé : affiche le niveau (Niveau: X) et les statistiques de vie
 * (PV/MaxPV) au-dessus de la tête du monstre en mode client.
 */
@Mod.EventBusSubscriber(modid = "aow", value = Dist.CLIENT)
public class MobLevelRender {

    // Distance max d'affichage (en blocs)
    private static final double MAX_DISTANCE = 64.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;

    // Utilisation d'un pattern plus générique pour le formatage des doubles avec une décimale.
    private static final DecimalFormat FLOAT_FORMAT = new DecimalFormat("0.0");


    /**
     * Hook de rendu révisé : déclenché après que l'entité a été rendue côté client,
     * permettant d'afficher les stats consolidées au-dessus du mob.
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        // 📏 Vérifie la distance
        double distanceSquared = mc.player.distanceToSqr(mob);
        if (distanceSquared > MAX_DISTANCE_SQUARED) return;

        // 🧱 Vérifie si le mob est visible et a une ligne de visée depuis le joueur.
        if (!mc.player.hasLineOfSight(mob)) return;

        // --- Calcul des données à afficher ---
        int level = getMobLevelFromPersistentData(mob);
        double currentHealth = mob.getHealth();
        double maxHealth = mob.getMaxHealth();

        String fullTextLevel = "";
        String fullTextHealth = "";
        if (level != -1) { // Ne rien afficher si le niveau n'est pas calculé/trouvé
            // Niveau
            fullTextLevel = String.format("Niveau: %d", level);

            // Santé
            String healthDisplay = String.format(Locale.ROOT, "PV : %s / %s",
                    FLOAT_FORMAT.format(currentHealth),
                    FLOAT_FORMAT.format(maxHealth));
            fullTextHealth = healthDisplay;
        }


        // --- Rendu du texte au-dessus du mob (Logique consolidée) ---
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        poseStack.pushPose();

        // Position au-dessus du mob
        poseStack.translate(0, mob.getBbHeight() + 0.5f, 0);

        // Face caméra
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        // Taille du texte
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        float x = 0; // Centre l'affichage sur X=0 car on va dessiner deux lignes empilées verticalement.
        // Niveau (Première ligne)
        float yLevel = -10.0F; // Décalage initial vers le haut pour la première ligne (Niveau)
        // Santé (Deuxième ligne, décalée vers le bas)
        float yHealth = yLevel + 10.0F; // Décalage de la deuxième ligne (Santé) par rapport à la première, pour empiler le texte.

        int color = 0xFFFFFF; // Blanc simple

        if (!fullTextLevel.isEmpty() || !fullTextHealth.isEmpty()) {
            // Dessin du Niveau
            mc.font.drawInBatch(
                    fullTextLevel,
                    x,
                    yLevel,
                    color,
                    false,  // shadow = true pour ombre portée
                    poseStack.last().pose(),
                    buffer,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                    0,
                    0xF000F0
            );

            // Dessin de la Santé
            mc.font.drawInBatch(
                    fullTextHealth,
                    x,
                    yHealth,
                    color,
                    false,  // shadow = true pour ombre portée
                    poseStack.last().pose(),
                    buffer,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                    0,
                    0xF000F0
            );
        }


        poseStack.popPose();
    }

    /**
     * Tente d'extraire le niveau pondéré du mob via ses données persistantes (méthode héritée de MobLevel).
     */
    private static int getMobLevelFromPersistentData(Mob mob) {
        return mob.getPersistentData().getInt("mobLevel");
    }
}