package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.StereoConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Fabric 向け Config 画面（parallax_cm と output_sub_dir のみ）。
 */
public class StereoConfigScreen extends Screen {

    private final Screen parent;
    private final StereoConfig config;
    private EditBox parallaxBox;
    private EditBox outputSubDirBox;

    public StereoConfigScreen(Screen parent, StereoConfig config) {
        super(Component.translatable("sbsshot.config.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int cx     = this.width / 2;
        int labelX = cx - 160;
        int fieldX = cx - 10;
        int fieldW = 160;
        int y      = 60;
        int rowH   = 26;

        // parallax_cm
        this.addRenderableWidget(Button.builder(
                Component.translatable("sbsshot.config.stereo.parallax_cm"), b -> {})
                .bounds(labelX, y, 165, 20)
                .tooltip(Tooltip.create(
                        Component.translatable("sbsshot.config.stereo.parallax_cm.tooltip")))
                .build());
        parallaxBox = new EditBox(this.font, fieldX, y, fieldW, 20,
                Component.literal(String.valueOf(config.parallaxCm)));
        parallaxBox.setValue(String.valueOf(config.parallaxCm));
        this.addRenderableWidget(parallaxBox);
        y += rowH;

        // output_sub_dir
        this.addRenderableWidget(Button.builder(
                Component.translatable("sbsshot.config.stereo.output_sub_dir"), b -> {})
                .bounds(labelX, y, 165, 20)
                .tooltip(Tooltip.create(
                        Component.translatable("sbsshot.config.stereo.output_sub_dir.tooltip")))
                .build());
        outputSubDirBox = new EditBox(this.font, fieldX, y, fieldW, 20,
                Component.literal(config.outputSubDir));
        outputSubDirBox.setValue(config.outputSubDir);
        this.addRenderableWidget(outputSubDirBox);
        y += rowH + 16;

        // Save / Cancel
        this.addRenderableWidget(Button.builder(
                Component.translatable("sbsshot.config.save"),
                b -> { save(); onClose(); })
                .bounds(cx - 106, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 6, y, 100, 20).build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private void save() {
        try { config.parallaxCm = Math.max(1f, Math.min(30f,
                Float.parseFloat(parallaxBox.getValue().trim())));
        } catch (NumberFormatException ignored) {}
        String d = outputSubDirBox.getValue().trim();
        if (!d.isEmpty()) config.outputSubDir = d;
        FabricConfigHelper.save(config);
    }
}
