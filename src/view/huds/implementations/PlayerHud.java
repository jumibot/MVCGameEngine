package view.huds.implementations;

import java.awt.Color;

import view.huds.core.DataHUD;

public class PlayerHUD extends DataHUD {
    public PlayerHUD() {
        super(
                new Color(255, 140, 0, 150 ), // Title color
                Color.GRAY, // Highlight color
                new Color(255, 255, 255, 80), // Label color
                new Color(255, 255, 255, 175), // Data color
                50, 12, 35);

        this.addItems();
    }

    private void addItems() {
        this.addTitle("PLAYER STATUS");
        this.addSkipValue(); // Entity ID
        this.addSkipValue(); // Player name
        this.addBar("Damage", 125, false);
        this.addBar("Energy", 125, false);
        this.addBar("Shield", 125, false);
        this.addTextItem("Temperature");
        this.addTitle("Weapons");
        this.addSkipValue(); // Active weapon
        this.addBar("Guns", 125, false);
        this.addBar("Burst", 125, false);
        this.addBar("Mines", 125, false);
        this.addBar("Missiles", 125, false);
        this.prepareHud();
    }
}
