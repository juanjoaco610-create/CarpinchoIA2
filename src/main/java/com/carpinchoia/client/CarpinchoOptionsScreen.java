package com.carpinchoia.client;

import com.carpinchoia.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public class CarpinchoOptionsScreen extends Screen {
    private final Screen parent;

    public CarpinchoOptionsScreen(Screen parent) {
        super(Component.literal("Carpincho"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 4;
        addRenderableWidget(new Slider(x, y, "Volumen de pasos", Config.STEP_VOLUME.get(), Config.STEP_VOLUME::set));
        addRenderableWidget(new Slider(x, y + 28, "Volumen de alertas", Config.ALERT_VOLUME.get(), Config.ALERT_VOLUME::set));
        addRenderableWidget(Button.builder(Component.literal("Voz: en Ajustes de Simple Voice Chat > Volumen > Carpincho"),
            b -> {}).bounds(x - 60, y + 56, 320, 20).build()).active = false;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .bounds(x, y + 96, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        Config.STEP_VOLUME.save();
        Config.ALERT_VOLUME.save();
        Minecraft.getInstance().setScreen(parent);
    }

    private static class Slider extends AbstractSliderButton {
        private final String label;
        private final DoubleConsumer setter;

        Slider(int x, int y, String label, double v, DoubleConsumer setter) {
            super(x, y, 200, 20, Component.empty(), v);
            this.label = label;
            this.setter = setter;
            updateMessage();
        }
        @Override protected void updateMessage() { setMessage(Component.literal(label + ": " + (int) Math.round(value * 100) + "%")); }
        @Override protected void applyValue() { setter.accept(value); }
    }
}
