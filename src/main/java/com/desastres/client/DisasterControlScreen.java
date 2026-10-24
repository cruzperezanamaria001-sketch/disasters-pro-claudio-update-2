package com.desastres.client;

import com.desastres.disaster.DisasterType;
import com.desastres.network.ModNetwork;
import com.desastres.network.SpawnDisasterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DisasterControlScreen extends Screen {
    // Se recuerdan entre aperturas de la pantalla
    private static int intensity = 2;   // 1..3
    private static int targetMode = 0;  // 0 = donde miro, 1 = sobre mi

    private static final int BTN_W = 140;
    private static final int BTN_H = 20;
    private static final int GAP = 4;
    private static final int COLS = 2;

    public DisasterControlScreen() {
        super(Component.translatable("gui.disasters.title"));
    }

    @Override
    protected void init() {
        DisasterType[] types = DisasterType.values();
        int rows = (types.length + COLS - 1) / COLS;
        int totalW = COLS * BTN_W + (COLS - 1) * GAP;
        int totalH = 22 + rows * (BTN_H + GAP) + 6 + 2 * (BTN_H + GAP);
        int startX = (this.width - totalW) / 2;
        int topY = (this.height - totalH) / 2;
        int y0 = topY + 22;

        for (int i = 0; i < types.length; i++) {
            final int index = i;
            int col = i % COLS;
            int row = i / COLS;
            DisasterType type = types[i];
            this.addRenderableWidget(Button.builder(Component.translatable(type.translationKey()), b -> sendSpawn(index))
                    .bounds(startX + col * (BTN_W + GAP), y0 + row * (BTN_H + GAP), BTN_W, BTN_H)
                    .tooltip(Tooltip.create(Component.translatable(type.translationKey() + ".desc")))
                    .build());
        }

        int y = y0 + rows * (BTN_H + GAP) + 6;

        this.addRenderableWidget(Button.builder(intensityText(), b -> {
                    intensity = intensity % 3 + 1;
                    b.setMessage(intensityText());
                })
                .bounds(startX, y, BTN_W, BTN_H)
                .tooltip(Tooltip.create(Component.translatable("gui.disasters.intensity.tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(targetText(), b -> {
                    targetMode = (targetMode + 1) % 2;
                    b.setMessage(targetText());
                })
                .bounds(startX + BTN_W + GAP, y, BTN_W, BTN_H)
                .tooltip(Tooltip.create(Component.translatable("gui.disasters.target.tooltip")))
                .build());

        y += BTN_H + GAP;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.disasters.stop_all"), b -> sendStop())
                .bounds(startX, y, BTN_W, BTN_H)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(startX + BTN_W + GAP, y, BTN_W, BTN_H)
                .build());
    }

    private static Component intensityText() {
        return Component.translatable("gui.disasters.intensity",
                Component.translatable("gui.disasters.intensity." + intensity));
    }

    private static Component targetText() {
        return Component.translatable(targetMode == 0 ? "gui.disasters.target.look" : "gui.disasters.target.self");
    }

    private void sendSpawn(int index) {
        ModNetwork.CHANNEL.sendToServer(new SpawnDisasterPacket(index, intensity, targetMode));
        this.onClose();
    }

    private void sendStop() {
        ModNetwork.CHANNEL.sendToServer(new SpawnDisasterPacket(-1, 0, 0));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        DisasterType[] types = DisasterType.values();
        int rows = (types.length + COLS - 1) / COLS;
        int totalH = 22 + rows * (BTN_H + GAP) + 6 + 2 * (BTN_H + GAP);
        int topY = (this.height - totalH) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, topY + 4, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
