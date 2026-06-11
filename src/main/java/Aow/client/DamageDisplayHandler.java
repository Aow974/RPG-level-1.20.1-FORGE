package Aow.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * DamageDisplayHandler
 *
 * Affiche dans le coin supérieur droit de l'écran :
 *  - Le dernier coup infligé (pendant 1,5 s)
 *  - Le DPS glissant sur une fenêtre de 5 s
 *
 * Le HUD disparaît complètement 5 s après le dernier coup.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "aow", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DamageDisplayHandler {

    // ── Constantes ──────────────────────────────────────────────────────────────

    /** Durée d'affichage du dernier coup (ms) */
    private static final long HIT_DISPLAY_MS   = 1_500L;

    /** Durée pendant laquelle le HUD reste visible après le dernier coup (ms) */
    private static final long HUD_LINGER_MS    = 5_000L;

    /** Fenêtre glissante pour le calcul du DPS (ms) */
    private static final long DPS_WINDOW_MS    = 5_000L;

    // ── État ────────────────────────────────────────────────────────────────────

    /** File des coups dans la fenêtre DPS (time, damage) */
    private static final Deque<DamageEntry> history = new ArrayDeque<>();

    /** Valeur du dernier coup (dégâts réels après résistances) */
    private static float lastHit      = 0f;

    /** Timestamp du dernier coup */
    private static long  lastHitTime  = Long.MIN_VALUE;

    /** DPS affiché (mis à jour toutes les secondes) */
    private static float displayedDps     = 0f;

    /** Timestamp du dernier refresh du DPS affiché */
    private static long  lastDpsRefreshMs = 0L;

    /** Intervalle de rafraîchissement du DPS (ms) */
    private static final long DPS_REFRESH_MS = 1_000L;

    // ── Entrée de l'historique ───────────────────────────────────────────────────

    private record DamageEntry(long time, float damage) {}

    // ── Événement : coup infligé ─────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {

        Minecraft mc = Minecraft.getInstance();

        // Seulement en local client
        if (mc.player == null) return;

        // La source doit être le joueur local
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.getUUID() != mc.player.getUUID()) return;

        // Ne pas compter les dégâts sur soi-même
        if (event.getEntity() == mc.player) return;

        float damage = event.getAmount(); // dégâts déjà réduits par armure/enchants
        long  now    = System.currentTimeMillis();

        lastHit     = damage;
        lastHitTime = now;

        history.addLast(new DamageEntry(now, damage));
        pruneHistory(now);
    }

    // ── Événement : rendu HUD ────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {

        // On se greffe après la barre de vie (hotbar overlay)
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        long now = System.currentTimeMillis();

        // Cacher le HUD si inactif depuis trop longtemps
        if (now - lastHitTime > HUD_LINGER_MS) return;

        pruneHistory(now);

        // Rafraîchir le DPS affiché une fois par seconde
        if (now - lastDpsRefreshMs >= DPS_REFRESH_MS) {
            displayedDps     = calculateDps(now);
            lastDpsRefreshMs = now;
        }

        Minecraft   mc          = Minecraft.getInstance();
        GuiGraphics graphics    = event.getGuiGraphics();
        Font        font        = mc.font;
        int         screenWidth = mc.getWindow().getGuiScaledWidth();

        // ── Paramètres de mise en page ──
        final int PAD_RIGHT  = 6;
        final int PAD_TOP    = 6;
        final int LINE_H     = 13;
        final int INNER_PAD  = 3;  // padding interne fond
        final int TEXT_H     = 8;  // hauteur glyphe standard

        // ── Ligne 1 : dernier coup ──────────────────────────────────────────────
        boolean showHit = (now - lastHitTime) < HIT_DISPLAY_MS;

        if (showHit) {
            String text  = String.format("Coup : %.1f ❤", lastHit);
            int    tw    = font.width(text);
            int    x     = screenWidth - tw - PAD_RIGHT;
            int    y     = PAD_TOP;

            drawBackground(graphics, x - INNER_PAD, y - INNER_PAD,
                    x + tw + INNER_PAD, y + TEXT_H + INNER_PAD);

            // Couleur : dégâts faibles = jaune, élevés = rouge vif
            int color = damageColor(lastHit);
            graphics.drawString(font, text, x, y, color, false);
        }

        // ── Ligne 2 : DPS ───────────────────────────────────────────────────────
        String dtext = String.format("DPS : %.1f", displayedDps);
        int    dtw   = font.width(dtext);
        int    dx    = screenWidth - dtw - PAD_RIGHT;
        int    dy    = PAD_TOP + (showHit ? LINE_H : 0);

        drawBackground(graphics, dx - INNER_PAD, dy - INNER_PAD,
                dx + dtw + INNER_PAD, dy + TEXT_H + INNER_PAD);

        graphics.drawString(font, dtext, dx, dy, 0xFFFFFFFF, false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Supprime les entrées plus vieilles que la fenêtre DPS.
     */
    private static void pruneHistory(long now) {
        while (!history.isEmpty() && now - history.peekFirst().time() > DPS_WINDOW_MS) {
            history.pollFirst();
        }
    }

    /**
     * Calcule le DPS glissant sur la fenêtre DPS_WINDOW_MS.
     *
     * On divise la somme des dégâts par le temps écoulé entre le PREMIER
     * et le DERNIER coup de la fenêtre, avec un minimum de 0.1 s pour éviter
     * la division par zéro sur le premier coup isolé.
     */
    private static float calculateDps(long now) {
        if (history.isEmpty()) return 0f;

        float totalDamage = 0f;
        for (DamageEntry e : history) {
            totalDamage += e.damage();
        }

        // On divise toujours par la fenêtre complète (DPS_WINDOW_MS).
        // Exemple : 1 coup à 50 dmg sur 5 s → DPS = 10, pas 500.
        float windowSec = DPS_WINDOW_MS / 1000f;

        return totalDamage / windowSec;
    }

    /**
     * Fond semi-transparent noir.
     */
    private static void drawBackground(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fill(x1, y1, x2, y2, 0x88000000);
    }

    /**
     * Couleur dégradée selon la puissance du coup :
     *  < 5  → jaune
     *  5-15 → orange
     *  > 15 → rouge vif
     */
    private static int damageColor(float dmg) {
        if (dmg < 5f)  return 0xFFFFFF55; // jaune
        if (dmg < 15f) return 0xFFFF9900; // orange
        return               0xFFFF3333; // rouge
    }
}