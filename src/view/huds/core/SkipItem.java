package view.huds.core;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class SkipItem extends Item {
    public SkipItem() {
        super("", null, null, true);
    }

    @Override
    public void draw(Graphics2D g, FontMetrics fm, int posX, int posY, Object value) {
        // No drawing needed for skip item
    }
}
