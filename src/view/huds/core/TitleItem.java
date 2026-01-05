package view.huds.core;

import java.awt.Color;

public class TitleItem extends Item {

    public TitleItem(String title, Color titleColor) {
        super(title, titleColor, null, false);
    }

    @Override
    void draw(java.awt.Graphics2D g, java.awt.FontMetrics fm, int posX, int posY, Object value) {

        // Label
        g.setColor(getLabelColor());
        g.drawString(this.getLabel(), posX, posY);
    }
}
