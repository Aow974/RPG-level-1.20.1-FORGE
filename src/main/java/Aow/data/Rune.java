package Aow.data;

/**
 * Représente une rune équipée ou en item.
 */
public class Rune {

    private final int hpBonus;
    private final int atkBonus;
    private final int armorBonus;

    public Rune(int hpBonus, int atkBonus, int armorBonus) {
        this.hpBonus = hpBonus;
        this.atkBonus = atkBonus;
        this.armorBonus = armorBonus;
    }

    public int getHpBonus() {
        return hpBonus;
    }

    public int getAtkBonus() {
        return atkBonus;
    }

    public int getArmorBonus() {
        return armorBonus;
    }
}