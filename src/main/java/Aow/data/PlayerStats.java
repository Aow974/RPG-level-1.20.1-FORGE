package Aow.data;

/**
 * Conteneur pour les statistiques du joueur, gérant à la fois les stats de base et les bonus des Runes.
 */
public class PlayerStats {
    // Stats de base (selon le jeu/Forge)
    private int baseHp = 20;
    private int baseAtk = 1;
    private int baseArmor = 0;

    // Bonus accumulé par les runes
    private int runeBonusHp = 0;
    private int runeBonusAtk = 0;
    private int runeBonusArmor = 0;

    public PlayerStats() {}

    /**
     * Calcule le HP final du joueur.
     * @return Le total de HP (base + bonus Runes).
     */
    public int getTotalHP() {
        // Pour ce système simple, nous additionnons les valeurs
        return baseHp + runeBonusHp; 
    }

    /**
     * Calcule l'ATK final du joueur.
     * @return Le total d'ATTACK (base + bonus Runes).
     */
    public int getTotalAtk() {
        return baseAtk + runeBonusAtk;
    }

    /**
     * Calcule l'ARMOR final du joueur.
     * @return Le total d'ARMOUR (base + bonus Runes).
     */
    public int getTotalArmor() {
        return baseArmor + runeBonusArmor;
    }

    /**
     * Ajoute les bonus statiques provenant des runes au PlayerStats.
     * Ce doit être appelé lorsque le joueur est rejoint ou réanimé.
     * @param bonusHp Le bonus total de HP des Runes.
     * @param bonusAtk Le bonus total d'ATK des Runes.
     * @param bonusArmor Le bonus total d'ARMOR des Runes.
     */
    public void applyRuneBonuses(int bonusHp, int bonusAtk, int bonusArmor) {
        this.runeBonusHp = Math.max(0, bonusHp); // Assurer qu'ils ne sont pas négatifs
        this.runeBonusAtk = Math.max(0, bonusAtk);
        this.runeBonusArmor = Math.max(0, bonusArmor);
    }

    // Getters pour les stats de base (si nécessaires plus tard)
    public int getBaseHp() { return baseHp; }
    public int getBaseAtk() { return baseAtk; }
    public int getBaseArmor() { return baseArmor; }
}