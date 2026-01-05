package view.huds;

import java.awt.Color;

import view.huds.core.Hud;

public class PlayerHud extends Hud {
    public PlayerHud() {
        super(
                new Color(255, 140, 0, 150 ), // Title color
                Color.GRAY, // Highlight color
                new Color(255, 255, 255, 80), // Label color
                new Color(255, 255, 255, 125), // Data color
                350, 12, 35);

        this.addItems();
    }

    private void addItems() {
        this.addTitle("PLAYER STATUS");
        this.addBar("Damage", 125);
        this.addBar("Energy", 125);
        this.addTitle("Weapons");
        this.addBar("Guns", 125, false);
        this.addBar("Burst", 125, false);
        this.addBar("Mines", 125, false);
        this.addBar("Missiles", 125, false);
        this.prepareHud();
    }
}
