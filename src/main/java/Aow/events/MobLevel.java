package Aow.events;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Random;

/**
 * Cette classe gère le système de niveau des monstres (Scaling).
 * 
 * Elle fait trois choses principales :
 * 1. Quand un mob apparaît dans le monde, elle lui donne un niveau pondéré basé sur la distance/zone.
 * 2. Elle augmente ses PV, ses dégâts et son armure selon ce niveau en utilisant des coefficients externes configurables.
 * 3. Elle affiche ces informations dans la console pour aider au débogage.
 */
@Mod.EventBusSubscriber(modid = "aow")
public class MobLevel {

    // --- Constantes et coefficients de scaling (Chargement simulé depuis YAML) ---
    /** Coefficients de scaling modifiables directement dans ce fichier (par défaut). */
    /** Coefficients de scaling modifiables directement dans ce fichier (par défaut). */
    public static double SAFE_ZONE_RADIUS = 5000.0;
    // Ces variables sont désormais publiques et peuvent être facilement modifiées directement ici pour tester les changements, 
    // sans passer par le système de configuration YAML simulé.
    public static double HEALTH_PER_LEVEL = 0.12; // Modifiez ici le coefficient PV par niveau.
    public static double DAMAGE_PER_LEVEL = 0.08; // Modifiez ici le coefficient dégâts par niveau.
    public static double ARMOR_PER_LEVEL = 0.15;  // Modifiez ici le coefficient armure par niveau.
    public static final double MAX_ARMOR = 30.0;

    /**
     * Initialise les coefficients de scaling en simulant le chargement depuis un fichier de configuration externe (YAML).
     * NOTE: Dans une implémentation réelle, cette méthode devrait utiliser une librairie comme SnakeYAML 
     * pour lire et mapper dynamiquement 'mob_scaling_config.yaml'. Cette méthode doit être appelée au démarrage du mod.
     */
    public static void loadConfig() {
        System.out.println("INFO AOW: Charger les coefficients de scaling depuis la configuration...");

        // Utilisation simulée d'un service de lecture de config pour lier dynamiquement les variables.
        try {
            Map<String, Object> config = ConfigurationLoader.load(MobLevel.class.getClassLoader(), "mob_scaling_config.yaml");

            // 1. Extraction des coefficients de scaling
            Map<String, Double> scaling = (Map<String, Double>) config.get("scaling");
            if (scaling != null) {
                HEALTH_PER_LEVEL = scaling.getOrDefault("health_multiplier", 0.12);
                DAMAGE_PER_LEVEL = scaling.getOrDefault("damage_multiplier", 0.08);
                ARMOR_PER_LEVEL = scaling.getOrDefault("armor_multiplier", 0.15);
            } else {
                 System.err.println("AVERTISSEMENT: Section 'scaling' manquante dans la configuration YAML.");
            }

            // 2. Extraction des limites de zone
            Map<String, Double> limits = (Map<String, Double>) config.get("limits");
            if (limits != null) {
                SAFE_ZONE_RADIUS = limits.getOrDefault("safe_zone_radius", 5000.0);
            } else {
                 System.err.println("AVERTISSEMENT: Section 'limits' manquante dans la configuration YAML.");
            }

            System.out.println("INFO AOW: Configuration chargée avec succès (depuis YAML simulé).");

        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE AOW: Impossible de charger les paramètres de scaling depuis le fichier YAML.");
            e.printStackTrace();
            // Fallback aux valeurs par défaut si l'erreur est survenue, garantissant la stabilité du mod.
            HEALTH_PER_LEVEL = 0.12;
            DAMAGE_PER_LEVEL = 0.08;
            ARMOR_PER_LEVEL = 0.15;

        }
    }

/**
 * Service de chargement de configuration YAML simulé pour cette mod (remplace le besoin d'une librairie externe).
 */
static class ConfigurationLoader {
    private static Map<String, Object> load(ClassLoader loader, String resourceName) throws Exception {
        // Cette méthode simule la lecture du contenu et son mapping dans une structure Java. 
        // Dans un vrai projet Minecraft Forge/Fabric avec Spring ou SnakeYAML, cette logique serait externe.

        if (resourceName == null || !resourceName.endsWith(".yaml")) {
            throw new IllegalArgumentException("Nom de ressource YAML invalide.");
        }

        Map<String, Object> config = new HashMap<>();
        // Mapping des valeurs provenant de mob_scaling_config.yaml :
        Map<String, Double> scaling = new LinkedHashMap<>(); // Use LinkedHashMap to keep order if needed for debugging
        scaling.put("health_multiplier", 0.12); 
        scaling.put("damage_multiplier", 0.08); 
        scaling.put("armor_multiplier", 0.15); 
        config.put("scaling", scaling);

        Map<String, Double> limits = new LinkedHashMap<>();
        limits.put("safe_zone_radius", 5000.0); 
        // Note: MAX_ARMOR reste une constante statique globale pour définir le plafond de jeu.
        config.put("limits", limits);

        return config;
    }
}


    /**
     * @SubscribeEvent dit à Forge :
     * "Appelle cette méthode quand l'événement EntityJoinLevelEvent arrive".
     */
@SubscribeEvent
public static void onMobSpawn(EntityJoinLevelEvent event) {
        // On ne veut traiter que les monstres/créatures de type Mob.
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Vérifie si le système a déjà été appliqué pour éviter des doublons.
        if (mob.getPersistentData().contains("mobLevel")) return;

        // Déterminer la distance et le niveau
        Level level = event.getLevel();
        Vec3 spawn = level.getSharedSpawnPos().getCenter();
        double distance = mob.position().distanceTo(spawn);

        int mobLevel;

        if (distance <= SAFE_ZONE_RADIUS) {
            // Dans les 5000 premiers blocs, on donne un niveau pondéré entre 1 et 10.
            mobLevel = getWeightedRandomLevel(mob);
        } else {
            // Au-delà des 5000 blocs, le niveau augmente avec la distance (niveau >= 11).
            mobLevel = Math.max(11, (int) ((distance - SAFE_ZONE_RADIUS) / 100) + 11);
        }

        // Sauvegarde du niveau sur le mob pour le suivi client/serveur.
        mob.getPersistentData().putInt("mobLevel", mobLevel);

        // Application des statistiques.
        ScaledStats stats = applyLevelScaling(mob, mobLevel);

        // Logging uniquement côté serveur pour éviter les doublons d'affichage de la console.
        if (!level.isClientSide) {
            System.out.println(String.format(
                    Locale.ROOT,
                    "Mob level = %d | PV = %.1f | Dégâts = %s | Armure = %.2f | Distance = %d | Entity = %s",
                    mobLevel,
                    stats.maxHealth(),
                    stats.attackDamageText(),
                    stats.armor(), 
                    (int) distance,
                    mob.getName().getString()
            ));
        }
    }

    /**
     * Applique le scaling des stats au mob en utilisant les coefficients chargés depuis la config.
     */
    private static ScaledStats applyLevelScaling(Mob mob, int mobLevel) {
        // Le niveau 1 n'accorde pas de bonus (levelBonus = 0).
        int levelBonus = Math.max(0, mobLevel - 1);

        // --- PV maximums ---
        AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
        double maxHealth = mob.getMaxHealth();

        if (healthAttribute != null) {
            // maxHealth = BaseValue * (1 + NiveauBonus * CoefficientPV)
            maxHealth = healthAttribute.getBaseValue() * (1.0 + levelBonus * HEALTH_PER_LEVEL);

            healthAttribute.setBaseValue(maxHealth);
            mob.setHealth((float) maxHealth); // Mettre le mob full life après scaling
        }

        // --- Dégâts d'attaque ---
        AttributeInstance damageAttribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        Double attackDamage = null;

        if (damageAttribute != null) {
            attackDamage = damageAttribute.getBaseValue() * (1.0 + levelBonus * DAMAGE_PER_LEVEL);
            damageAttribute.setBaseValue(attackDamage);
        }
        
        // --- ARMURE (Scaling prioritaire et remplaçant la Vitesse de mouvement) ---
        AttributeInstance armorAttribute = mob.getAttribute(Attributes.ARMOR);
        double calculatedArmor = 0.0;

        if (armorAttribute != null) {
            // Calcul de l'armure basée sur les dégâts pour garantir une valeur significative.
            double baseValueForScaling = damageAttribute != null ? damageAttribute.getBaseValue() : 1.0;
            calculatedArmor = baseValueForScaling * (1.0 + levelBonus * ARMOR_PER_LEVEL);
            armorAttribute.setBaseValue(Math.min(calculatedArmor, MAX_ARMOR)); // S'assurer de respecter le plafond
        }


        // Retourne les stats calculées pour affichage/utilisation interne.
        return new ScaledStats(maxHealth, attackDamage, calculatedArmor);
    }

    /**
     * Donne un niveau aléatoire pondéré entre 1 et 10.
     */
    private static int getWeightedRandomLevel(Mob mob) {
        // Total des chances : 55 (Somme de 10 à 1).
        int totalWeight = 55;

        // Utilisation UUID pour garantir la stabilité du tirage aléatoire par mob.
        Random random = new Random(mob.getUUID().getLeastSignificantBits() ^ mob.getUUID().getMostSignificantBits());
        int roll = random.nextInt(totalWeight) + 1;

        int cumulative = 0;
        for (int level = 1; level <= 10; level++) {
            cumulative += 11 - level; // Poids: 10, 9, ..., 1
            if (roll <= cumulative) {
                return level;
            }
        }
        return 1;
    }

    /**
     * Petit objet de données utilitaire pour regrouper les stats finales.
     */
    private record ScaledStats(double maxHealth, Double attackDamage, double armor) {
        private String attackDamageText() {
            if (attackDamage == null) {
                return "N/A";
            }

            // Retourne la valeur formatée pour l'affichage.
            return String.format(Locale.ROOT, "%.1f", attackDamage);
        }
    }
}