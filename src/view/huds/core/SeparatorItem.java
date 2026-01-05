package view.huds.core;

public class SeparatorItem extends Item {
    public SeparatorItem() {
        super("", null, null,false);
    }

    @Override
    void draw(java.awt.Graphics2D g, java.awt.FontMetrics fm, int posX, int posY, Object value) {
        g.drawString(this.getLabel(), posX, posY);
    }   
}
