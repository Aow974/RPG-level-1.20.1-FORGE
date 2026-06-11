package Aow.data;

import java.util.List;

/**
 * Conteneur des statistiques du joueur.
 * Les bonus ne sont PAS stockés en snapshot.
 * Ils sont calculés dynamiquement à partir des runes équipées.
 */
public class PlayerStats {

    // Stats de base
    private final int baseHp = 20;
    private final int baseAtk = 1;
    private final int baseArmor = 0;

    public int getBaseHp() { return baseHp; }
    public int getBaseAtk() { return baseAtk; }
    public int getBaseArmor() { return baseArmor; }
}