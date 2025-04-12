package net.visemesx.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.client.gui.widget.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;
import net.visemesx.VisemesX;
import net.visemesx.audio.AudioCapture;

import java.util.List;

public class VisemesXGUI extends Screen {
    private int activeTab = 0; // 0 = Import, 1 = Emotions, 2 = Voice, 3 = Presets
    private ElementListWidget<MicrophoneEntry> microphoneListWidget;
    private SliderWidget sensitivitySlider;
    private static final Text SENSITIVITY_TEXT = Text.translatable("Microphone Sensitivity: ");
    private static final Text MICROPHONE_TEXT = Text.translatable("Microphone");

    public VisemesXGUI() {
        super(Text.of("VisemesX - Voice & Expression Mod"));
    }

    @Override
    protected void init() {
        // Top Navigation Buttons
        this.addDrawableChild(
                ButtonWidget.builder(Text.of("📂 Import"), button -> setTab(0))
                        .dimensions(this.width / 2 - 120, 40, 80, 20)
                        .tooltip(Tooltip.of(Text.of("Import button")))
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.of("🎭 Emotions"), button -> setTab(1))
                        .dimensions(this.width / 2 - 40, 40, 80, 20)
                        .tooltip(Tooltip.of(Text.of("Emoticons button")))
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.of("🎙️ Voice"), button -> setTab(2))
                        .dimensions(this.width / 2 + 40, 40, 80, 20)
                        .tooltip(Tooltip.of(Text.of("Voice button")))
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.of("💾 Presets"), button -> setTab(3))
                        .dimensions(this.width / 2 + 120, 40, 80, 20)
                        .tooltip(Tooltip.of(Text.of("Presets button")))
                        .build()
        );
        updateTab();
    }

    private void setTab(int tab) {
        this.activeTab = tab;
        this.clearChildren();
        this.init();
    }
    private void importSkinZip() {
        // TODO: Implement the ZIP import logic
        System.out.println("Import ZIP clicked!");
    }

    private void StopMic() {
        VisemesX.getInstance().getAudioCapture().stopCapture();
    }
    private void StartMic() {
        VisemesX.getInstance().getAudioCapture().startCapture();
    }

    private void TogglePTT() {
        VisemesX.getInstance().pushtotalk = !VisemesX.getInstance().pushtotalk;
        System.out.println("[VisemeDetector] PTT = " + VisemesX.getInstance().pushtotalk );
    }

    private void assignEmotion(int slot) {
        // TODO: Implement the ZIP import logic
        System.out.println("Import ZIP clicked!");
    }
    private void setMicSensitivity(float value) {
        VisemesX.getInstance().getPhonemeProcessor().micsensitivity = value;
        System.out.println("SETMIC " + VisemesX.getInstance().getPhonemeProcessor().micsensitivity);
    }

    private void savePreset() {
        // TODO: Implement the ZIP import logic
        System.out.println("Import ZIP clicked!");
    }

    private void updateTab() {
        switch (activeTab) {
            case 0 -> showImportTab();
            case 1 -> showEmotionsTab();
            case 2 -> showVoiceSettingsTab();
            case 3 -> showPresetsTab();
        }
    }
    private void showImportTab() {
        this.addDrawableChild(
                ButtonWidget.builder(Text.of("📂 Import ZIP"), button -> importSkinZip())
                        .dimensions(this.width / 2 - 50, 110, 100, 20)
                        .narrationSupplier((btn) -> (net.minecraft.text.MutableText) Text.of("Import ZIP button"))
                        .build()
        );
    }

    private void showEmotionsTab() {
        for (int i = 0; i < 9; i++) {
            final int slot = i;  // Make 'slot' final
            this.addDrawableChild(
                    ButtonWidget.builder(Text.of("E" + (i + 1)), button -> assignEmotion(slot))
                            .dimensions(20 + (i * 30), 110, 30, 20)
                            .narrationSupplier((btn) -> Text.literal("Emotion " + (slot + 1) + " button"))
                            .build()
            );
        }
    }

    private void showVoiceSettingsTab() {
        int labelY = 80;
        int controlY = 50;
        int controlWidth = 150;

        int textHeight = 10;
        TextWidget t = new TextWidget(Text.of("visemesx.audio_input_settings"), this.textRenderer);
        t.setY(50);
        t.setX(this.width / 2);

        this.addDrawableChild(
                t
        );
        TextWidget tm = new TextWidget(MICROPHONE_TEXT, this.textRenderer);
        tm.setY(70);
        tm.setX(this.width / 2);
        this.addDrawableChild(
                tm
        );

        AudioCapture audioCapture = VisemesX.getInstance().getAudioCapture();
        List<String> microphoneNames = audioCapture.getCaptureDeviceNames();

        if (!microphoneNames.isEmpty()) {
            microphoneListWidget = new ElementListWidget<MicrophoneEntry>(this.client, controlWidth,  controlY, 0, controlY){};
            microphoneListWidget.setHeight(60);
            microphoneListWidget.setX(40);
            microphoneListWidget.setY(0);
            microphoneListWidget.setWidth(100);

            for (String name : microphoneNames) {
                microphoneListWidget.addEntry(new MicrophoneEntry(name, this::onMicrophoneSelected));
            }
            this.addDrawableChild(microphoneListWidget);

        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("visemesx.error.no_microphones"), button -> {}).dimensions(this.width / 2, controlY, controlWidth, 20).build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("Toggle Push To Talk Visemes"), button -> {this.TogglePTT();}).dimensions(this.width / 2, this.height - 40, 100, 40).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("Start Visemes"), button -> {this.StartMic();}).dimensions(20, this.height - 40, 30, 40).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("Stop Visemes"), button -> {this.StopMic();}).dimensions(this.width - 60, this.height - 40, 30, 40).build());
        sensitivitySlider = new SliderWidget(this.width / 2, controlY + 90, controlWidth, 20, Text.of(""), VisemesX.getInstance().getPhonemeProcessor().micsensitivity) {
            @Override
            protected void updateMessage() {

            }


            @Override
            protected void applyValue() {
                VisemesXGUI.this.setMicSensitivity((float) this.value);
            }
        };
        TextWidget st = new TextWidget(Text.of("Microphone Sensitivity: "+ sensitivitySlider.value), this.textRenderer);
        st.setX(sensitivitySlider.getX());
        st.setY(sensitivitySlider.getY() - 20);
        this.addDrawableChild(
                st
        );
        this.addDrawableChild(sensitivitySlider);
    }

    public void onMicrophoneSelected(String microphoneName) {
        AudioCapture audioCapture = VisemesX.getInstance().getAudioCapture();
        int index = audioCapture.getCaptureDeviceIndex(microphoneName);
        audioCapture.selectedindex = index;
        if (index != -1) {
            if(VisemesX.getInstance().getAudioCapture().capturing) {
                VisemesX.getInstance().restartAudioCapture();
            }
            System.out.println("Selected microphone: " + microphoneName + " (Index: " + index + ")");
        } else {
            System.out.println("Error: Could not find index for microphone: " + microphoneName);
        }
    }

    private void showPresetsTab() {
        this.addDrawableChild(
                ButtonWidget.builder(Text.of("💾 Save Preset"), button -> savePreset())
                        .dimensions(this.width / 2 - 50, 110, 100, 20)
                        .narrationSupplier((btn) -> (net.minecraft.text.MutableText) Text.of("Save Preset button"))
                        .build()
        );
    }

    public void drawCenteredText(DrawContext textc,MatrixStack matrices, Text text, int x, int y, int color) {
        // Get the width of the text
        int textWidth = this.textRenderer.getWidth(text);

        // Calculate the position so that the text is centered
        int xPosition = x - textWidth / 2;

        // Use the textRenderer to draw the text at the calculated position
        textc.drawText(this.textRenderer,text,x,y,color,true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context,mouseX,mouseY,delta); // Render background (if necessary)
        super.render(context, mouseX, mouseY, delta); // Calls the parent class's render method


        // Access the matrix stack from the context
        MatrixStack matrices = context.getMatrices();

        // Custom rendering logic
        drawCenteredText(context,matrices, Text.of("VisemesX - Voice & Expression Mod"), this.width / 2, 20, 0xFFFFFF);

    }


    private class MicrophoneEntry extends ElementListWidget.Entry<MicrophoneEntry> {
        private final String microphoneName;
        private final ButtonWidget selectButton;
        private final VisemesXGUI parentScreen;

        public MicrophoneEntry(String microphoneName, java.util.function.Consumer<String> onSelect) {
            super();
            this.microphoneName = microphoneName;
            this.parentScreen = VisemesXGUI.this;
            this.selectButton = ButtonWidget.builder(Text.literal(microphoneName), button -> onSelect.accept(microphoneName))
                    .width(parentScreen.microphoneListWidget.getRowWidth() - 10)
                    .build();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.selectButton.setX(x + 2);
            this.selectButton.setY(y - 10);
            this.selectButton.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(this.selectButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
            return List.of(this.selectButton);
        }
    }
}
