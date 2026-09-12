package cn.ethereal.event.events;

import cn.ethereal.event.Event;
import net.minecraft.client.gui.ScaledResolution;

public class Render2DEvent extends Event {
    private final float partialTicks;
    private final ScaledResolution scaledResolution;
    private final int width;
    private final int height;

    public Render2DEvent(float partialTicks, ScaledResolution scaledResolution) {
        this.partialTicks = partialTicks;
        this.scaledResolution = scaledResolution;
        this.width = scaledResolution.getScaledWidth();
        this.height = scaledResolution.getScaledHeight();
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public ScaledResolution getScaledResolution() {
        return scaledResolution;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}