package dev.ripiters.create_frequency.client.gui;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.ripiters.create_frequency.common.CFLang;
import dev.ripiters.create_frequency.common.CFGuiTextures;
import dev.ripiters.create_frequency.common.link.FrequencyBlockEntity;
import dev.ripiters.create_frequency.common.network.CFPackets;
import dev.ripiters.create_frequency.common.network.ConfigureFrequencyPacket;
import dev.ripiters.create_frequency.common.network.FrequencyListRequestPacket;
import dev.ripiters.create_frequency.common.network.FrequencyNetworkHandler;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

public class FrequencyConfigScreen extends AbstractSimiScreen {

    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 126;
    private static final int FREQUENCY_INPUT_X = 23;
    private static final int FREQUENCY_INPUT_Y = 27;
    private static final int FREQUENCY_INPUT_WIDTH = 41;
    private static final int FREQUENCY_INPUT_HEIGHT = 16;
    private static final int NAME_INPUT_X = 72;
    private static final int NAME_INPUT_Y = 27;
    private static final int NAME_INPUT_WIDTH = 103;
    private static final int NAME_INPUT_HEIGHT = 16;
    private static final int SELECTOR_BUTTON_X = 15;
    private static final int SELECTOR_BUTTON_Y = 70;
    private static final int SELECTOR_BUTTON_WIDTH = 17;
    private static final int SELECTOR_BUTTON_HEIGHT = 18;
    private static final int PREVIEW_X = 40;
    private static final int PREVIEW_Y = 71;
    private static final int PREVIEW_WIDTH = 137;
    private static final int PREVIEW_HEIGHT = 16;
    private static final int CONFIRM_X = 167;
    private static final int CONFIRM_Y = 102;
    private static final int POPUP_WIDTH = 170;
    private static final int POPUP_OFFSET_Y = 4;
    private static final int LIST_VISIBLE_ROWS = 6;

    private final CFGuiTextures background = CFGuiTextures.TRANSMITTER_RECEIVER;
    private final BlockPos pos;
    private final FrequencyBlockEntity be;

    private EditBox hzInput;
    private EditBox nameInput;
    private IconButton confirmButton;

    private float frequency;
    private String networkName;
    private List<FrequencyNetworkHandler.FrequencyListEntry> availableFrequencies = List.of();
    private Float selectedFrequency;
    private int firstVisibleIndex;
    private boolean suppressCallbacks;
    private boolean userEditedFields;
    private boolean explicitNameDecision;
    private boolean selectorOpen;

    private static final String KEY_TITLE = "gui.frequency.title";
    private static final String KEY_HINT = "gui.frequency.name_hint";
    private static final String KEY_LIST_HINT = "gui.frequency.list_hint";
    private static final String KEY_UNNAMED = "gui.frequency.unnamed";
    private static final String KEY_EMPTY_LIST = "gui.frequency.empty_list";

    public FrequencyConfigScreen(BlockPos pos, FrequencyBlockEntity be) {
        super(CFLang.translateDirect(KEY_TITLE));
        this.pos = pos;
        this.be = be;
        this.frequency = be.getFrequency();
        this.networkName = be.getNetworkName();
    }

    @Override
    protected void init() {
        setWindowSize(GUI_WIDTH, GUI_HEIGHT);
        setWindowOffset(0, 0);
        super.init();

        int x = guiLeft;
        int y = guiTop;

        hzInput = createAlignedInput(x + FREQUENCY_INPUT_X, y + FREQUENCY_INPUT_Y, FREQUENCY_INPUT_WIDTH, FREQUENCY_INPUT_HEIGHT);
        hzInput.setValue(formatFreq(frequency));
        hzInput.setBordered(false);
        hzInput.setTextColor(0xFBDC7D);
        hzInput.setMaxLength(8);
        hzInput.setResponder(this::onFrequencyEdited);
        addRenderableWidget(hzInput);

        nameInput = createAlignedInput(x + NAME_INPUT_X, y + NAME_INPUT_Y, NAME_INPUT_WIDTH, NAME_INPUT_HEIGHT);
        nameInput.setValue(networkName);
        nameInput.setBordered(false);
        nameInput.setMaxLength(25);
        nameInput.setTextColor(0xFFFFFF);
        nameInput.setHint(CFLang.translateDirect(KEY_HINT));
        nameInput.setResponder(this::onNameEdited);
        addRenderableWidget(nameInput);

        confirmButton = new IconButton(x + CONFIRM_X, y + CONFIRM_Y, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);

        CFPackets.CHANNEL.sendToServer(new FrequencyListRequestPacket(pos));
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        graphics.blit(background.location, x, y, background.startX, background.startY, GUI_WIDTH, GUI_HEIGHT);

        Component titleComp = CFLang.translateDirect(KEY_TITLE);
        graphics.drawString(font, titleComp, x + GUI_WIDTH / 2 - 4 - font.width(titleComp) / 2, y + 4, 0x592424, false);

        String hzValue = hzInput.getValue().trim();
        if (!hzValue.isEmpty() && !hzValue.equals(".")) {
            int textWidth = font.width(hzValue);
            graphics.drawString(font, "Hz", hzInput.getX() + textWidth + 2, hzInput.getY() + 1, 0xFBDC7D, true);
        }

        renderSelectorButtonState(graphics, mouseX, mouseY);
        renderPreview(graphics, x, y);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (selectorOpen) {
            renderSelectorPopup(graphics, guiLeft, guiTop);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectorOpen && isSelectorInteraction(mouseX, mouseY)) {
            return selectByScroll(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInRowBounds(mouseX, mouseY, SELECTOR_BUTTON_X, SELECTOR_BUTTON_Y, SELECTOR_BUTTON_WIDTH, SELECTOR_BUTTON_HEIGHT)) {
            selectorOpen = !selectorOpen;
            clearInputFocus();
            return true;
        }

        if (isInRowBounds(mouseX, mouseY, FREQUENCY_INPUT_X, FREQUENCY_INPUT_Y, FREQUENCY_INPUT_WIDTH, FREQUENCY_INPUT_HEIGHT)) {
            selectorOpen = false;
            focusInput(hzInput, mouseX, mouseY, button);
            nameInput.setFocused(false);
            return true;
        }

        if (isInRowBounds(mouseX, mouseY, NAME_INPUT_X, NAME_INPUT_Y, NAME_INPUT_WIDTH, NAME_INPUT_HEIGHT)) {
            selectorOpen = false;
            focusInput(nameInput, mouseX, mouseY, button);
            hzInput.setFocused(false);
            return true;
        }

        // Confirm button takes priority over the popup even when the popup is open,
        // because the button sits inside the popup's bounding box.
        if (selectorOpen && isInRowBounds(mouseX, mouseY, CONFIRM_X, CONFIRM_Y, 18, 18)) {
            selectorOpen = false;
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (selectorOpen && isInPopupBounds(mouseX, mouseY)) {
            handlePopupClick(mouseX, mouseY);
            return true;
        }

        selectorOpen = false;
        clearInputFocus();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameInput.isFocused() && nameInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (hzInput.isFocused() && hzInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectorOpen) {
                selectorOpen = false;
                return true;
            }
            this.onClose();
            return true;
        }

        if (nameInput.isFocused() && nameInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (hzInput.isFocused() && hzInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        String val = hzInput.getValue().trim().replace(",", ".");
        if (val.isEmpty() || val.equals(".")) {
            frequency = 0f;
        } else {
            try {
                frequency = Math.min(1000f, Float.parseFloat(val));
            } catch (NumberFormatException e) {
                frequency = 0f;
            }
        }

        String nameToSave = nameInput.getValue().trim();
        boolean keepExistingName = !explicitNameDecision && nameToSave.isEmpty();
        CFPackets.CHANNEL.sendToServer(new ConfigureFrequencyPacket(pos, frequency, nameToSave, keepExistingName));
        super.removed();
    }

    public boolean matches(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public void receiveFrequencyEntries(List<FrequencyNetworkHandler.FrequencyListEntry> entries) {
        availableFrequencies = List.copyOf(entries);
        alignListToSelection();

        if (userEditedFields) {
            return;
        }

        findMatchingEntry(frequency).ifPresent(entry -> {
            selectedFrequency = entry.frequency();
            if (networkName.isBlank() && !entry.name().isBlank()) {
                applySelectedEntry(entry, false);
            } else {
                alignListToSelection();
            }
        });
    }

    private void renderPreview(GuiGraphics graphics, int guiLeft, int guiTop) {
        String text = getPreviewText();
        if (text.isEmpty()) {
            return;
        }

        int color = hasSelectedEntry() || !availableFrequencies.isEmpty() ? 0xFFFFFF : 0x9F9F9F;
        graphics.drawString(font, font.plainSubstrByWidth(text, PREVIEW_WIDTH - 6), guiLeft + PREVIEW_X + 2, guiTop + PREVIEW_Y + 4, color, false);
    }

    private void renderSelectorPopup(GuiGraphics graphics, int guiLeft, int guiTop) {
        int rowHeight = font.lineHeight + 2;
        int visibleRows = Math.max(1, Math.min(LIST_VISIBLE_ROWS, availableFrequencies.size()));
        int popupX = guiLeft + SELECTOR_BUTTON_X;
        int popupY = guiTop + SELECTOR_BUTTON_Y + SELECTOR_BUTTON_HEIGHT + POPUP_OFFSET_Y;
        int popupHeight = 6 + visibleRows * rowHeight + font.lineHeight + 10;
        int textWidth = POPUP_WIDTH - 12;

        graphics.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + popupHeight, 0xF0100010);
        graphics.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + 1, 0xFFF0F0F0);
        graphics.fill(popupX, popupY + popupHeight - 1, popupX + POPUP_WIDTH, popupY + popupHeight, 0xFF505050);
        graphics.fill(popupX, popupY, popupX + 1, popupY + popupHeight, 0xFFF0F0F0);
        graphics.fill(popupX + POPUP_WIDTH - 1, popupY, popupX + POPUP_WIDTH, popupY + popupHeight, 0xFF505050);

        if (availableFrequencies.isEmpty()) {
            graphics.drawString(font, CFLang.translateDirect(KEY_EMPTY_LIST), popupX + 5, popupY + 5, 0x9F9F9F, false);
        } else {
            alignListToSelection();
            for (int row = 0; row < visibleRows; row++) {
                int entryIndex = firstVisibleIndex + row;
                if (entryIndex >= availableFrequencies.size()) {
                    break;
                }

                FrequencyNetworkHandler.FrequencyListEntry entry = availableFrequencies.get(entryIndex);
                boolean selected = selectedFrequency != null && Float.compare(selectedFrequency, entry.frequency()) == 0;
                int rowY = popupY + 4 + row * rowHeight;

                if (selected) {
                    graphics.fill(popupX + 2, rowY - 1, popupX + POPUP_WIDTH - 2, rowY + font.lineHeight + 1, 0x443A3A3A);
                }

                String prefix = selected ? " -> " : "> ";
                int color = selected ? 0xFFFFFF : 0x9F9F9F;
                graphics.drawString(font, prefix + getEntryDisplayText(entry, textWidth), popupX + 4, rowY, color, false);
            }
        }

        Component listHint = CFLang.translateDirect(KEY_LIST_HINT).withStyle(style -> style.withItalic(true));
        graphics.drawString(font, listHint, popupX + 5, popupY + popupHeight - font.lineHeight - 4, 0x8F8F8F, false);
    }

    private boolean selectByScroll(double delta) {
        if (availableFrequencies.isEmpty()) {
            return true;
        }

        int currentIndex = getSelectedIndex();
        int nextIndex;
        if (currentIndex == -1) {
            nextIndex = delta < 0 ? 0 : availableFrequencies.size() - 1;
        } else {
            nextIndex = currentIndex + (delta < 0 ? 1 : -1);
        }

        nextIndex = Math.max(0, Math.min(availableFrequencies.size() - 1, nextIndex));
        applySelectedEntry(availableFrequencies.get(nextIndex), true);
        alignListToSelection();
        return true;
    }

    private EditBox createAlignedInput(int x, int y, int width, int height) {
        return new EditBox(font, x, y, width, height, Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 1, 0);
                super.renderWidget(graphics, mouseX, mouseY, partialTicks);
                graphics.pose().popPose();
            }
        };
    }

    private void renderSelectorButtonState(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = guiLeft + SELECTOR_BUTTON_X;
        int y = guiTop + SELECTOR_BUTTON_Y;
        if (selectorOpen) {
            graphics.fill(x + 1, y + 1, x + SELECTOR_BUTTON_WIDTH - 1, y + SELECTOR_BUTTON_HEIGHT - 1, 0x3FFFFFFF);
            return;
        }

        if (isInRowBounds(mouseX, mouseY, SELECTOR_BUTTON_X, SELECTOR_BUTTON_Y, SELECTOR_BUTTON_WIDTH, SELECTOR_BUTTON_HEIGHT)) {
            graphics.fill(x + 1, y + 1, x + SELECTOR_BUTTON_WIDTH - 1, y + SELECTOR_BUTTON_HEIGHT - 1, 0x22FFFFFF);
        }
    }

    private void handlePopupClick(double mouseX, double mouseY) {
        if (availableFrequencies.isEmpty()) {
            return;
        }

        int rowHeight = font.lineHeight + 2;
        int popupY = guiTop + SELECTOR_BUTTON_Y + SELECTOR_BUTTON_HEIGHT + POPUP_OFFSET_Y;
        int relativeY = (int) mouseY - (popupY + 4);
        if (relativeY < 0) {
            return;
        }

        int row = relativeY / rowHeight;
        int entryIndex = firstVisibleIndex + row;
        if (entryIndex < 0 || entryIndex >= availableFrequencies.size()) {
            return;
        }

        applySelectedEntry(availableFrequencies.get(entryIndex), true);
        selectorOpen = false;
    }

    private void focusInput(EditBox input, double mouseX, double mouseY, int button) {
        if (!input.isFocused()) {
            setFocused(input);
            input.setFocused(true);
            input.setCursorPosition(input.getValue().length());
            input.setHighlightPos(0);
            return;
        }
        input.mouseClicked(mouseX, mouseY, button);
    }

    private void clearInputFocus() {
        setFocused(null);
        hzInput.setFocused(false);
        nameInput.setFocused(false);
    }

    private void onFrequencyEdited(String value) {
        if (suppressCallbacks) {
            return;
        }

        if (!value.isEmpty() && !value.equals(".")) {
            try {
                float parsed = Float.parseFloat(value.replace(",", "."));
                if (parsed > 1000) {
                    setHzValue("1000");
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (hzInput.isFocused()) {
            userEditedFields = true;
        }

        if (selectedFrequency != null && !matchesSelectedFrequency(value)) {
            String previousSelectedName = findMatchingEntry(selectedFrequency)
                    .map(FrequencyNetworkHandler.FrequencyListEntry::name)
                    .orElse("");

            selectedFrequency = null;
            alignListToSelection();

            if (nameInput.getValue().trim().equals(previousSelectedName)) {
                suppressCallbacks = true;
                nameInput.setValue("");
                suppressCallbacks = false;
                explicitNameDecision = false;
            }
        }
    }

    private void onNameEdited(String value) {
        if (suppressCallbacks) {
            return;
        }

        if (nameInput.isFocused()) {
            userEditedFields = true;
            explicitNameDecision = true;
        }

        if (selectedFrequency == null) {
            return;
        }

        String selectedName = findMatchingEntry(selectedFrequency)
                .map(FrequencyNetworkHandler.FrequencyListEntry::name)
                .orElse("");
        if (!value.trim().equals(selectedName)) {
            selectedFrequency = null;
            alignListToSelection();
        }
    }

    private void applySelectedEntry(FrequencyNetworkHandler.FrequencyListEntry entry, boolean markExplicit) {
        selectedFrequency = entry.frequency();
        frequency = entry.frequency();
        networkName = entry.name();
        suppressCallbacks = true;
        hzInput.setValue(formatFreq(entry.frequency()));
        nameInput.setValue(entry.name());
        suppressCallbacks = false;
        explicitNameDecision = markExplicit;
        if (markExplicit) {
            userEditedFields = true;
        }
    }

    private int getSelectedIndex() {
        if (selectedFrequency == null) {
            return -1;
        }

        for (int i = 0; i < availableFrequencies.size(); i++) {
            if (Float.compare(availableFrequencies.get(i).frequency(), selectedFrequency) == 0) {
                return i;
            }
        }
        return -1;
    }

    private void alignListToSelection() {
        int maxStart = Math.max(0, availableFrequencies.size() - LIST_VISIBLE_ROWS);
        int selectedIndex = getSelectedIndex();
        if (selectedIndex == -1) {
            firstVisibleIndex = Math.min(firstVisibleIndex, maxStart);
            return;
        }

        if (selectedIndex < firstVisibleIndex) {
            firstVisibleIndex = selectedIndex;
        } else if (selectedIndex >= firstVisibleIndex + LIST_VISIBLE_ROWS) {
            firstVisibleIndex = selectedIndex - LIST_VISIBLE_ROWS + 1;
        }

        firstVisibleIndex = Math.max(0, Math.min(firstVisibleIndex, maxStart));
    }

    private Optional<FrequencyNetworkHandler.FrequencyListEntry> findMatchingEntry(float targetFrequency) {
        return availableFrequencies.stream()
                .filter(entry -> Float.compare(entry.frequency(), targetFrequency) == 0)
                .findFirst();
    }

    private boolean matchesSelectedFrequency(String value) {
        if (selectedFrequency == null) {
            return false;
        }
        return formatFreq(selectedFrequency).equals(value.replace(",", "."));
    }

    private boolean hasSelectedEntry() {
        return selectedFrequency != null && findMatchingEntry(selectedFrequency).isPresent();
    }

    private String getPreviewText() {
        if (hasSelectedEntry()) {
            return getPreviewEntryText(findMatchingEntry(selectedFrequency).orElseThrow());
        }

        if (!availableFrequencies.isEmpty()) {
            return getPreviewEntryText(availableFrequencies.get(0));
        }

        return "";
    }

    private String getPreviewFrequency() {
        String value = hzInput.getValue().trim();
        if (value.isEmpty() || value.equals(".")) {
            return "";
        }

        try {
            return formatFreq(Float.parseFloat(value.replace(",", ".")));
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private String getEntryDisplayText(FrequencyNetworkHandler.FrequencyListEntry entry, int maxWidth) {
        String name = entry.name().isBlank() ? CFLang.translateDirect(KEY_UNNAMED).getString() : entry.name();
        return font.plainSubstrByWidth(name + " (" + formatFreq(entry.frequency()) + "Hz)", maxWidth);
    }

    private String getPreviewEntryText(FrequencyNetworkHandler.FrequencyListEntry entry) {
        String frequencyText = formatFreq(entry.frequency()) + "Hz";
        if (entry.name().isBlank()) {
            return frequencyText;
        }
        return entry.name() + " (" + frequencyText + ")";
    }

    private void setHzValue(String value) {
        suppressCallbacks = true;
        hzInput.setValue(value);
        suppressCallbacks = false;
    }

    private String formatFreq(float f) {
        if (f == (long) f) {
            return String.format("%d", (long) f);
        }
        return String.format("%.1f", f).replace(",", ".");
    }

    private boolean isSelectorInteraction(double mouseX, double mouseY) {
        return isInRowBounds(mouseX, mouseY, SELECTOR_BUTTON_X, SELECTOR_BUTTON_Y, SELECTOR_BUTTON_WIDTH, SELECTOR_BUTTON_HEIGHT)
                || isInPopupBounds(mouseX, mouseY);
    }

    private boolean isInPopupBounds(double mouseX, double mouseY) {
        int rowHeight = font.lineHeight + 2;
        int visibleRows = Math.max(1, Math.min(LIST_VISIBLE_ROWS, availableFrequencies.size()));
        int popupHeight = 6 + visibleRows * rowHeight + font.lineHeight + 10;
        return mouseX >= guiLeft + SELECTOR_BUTTON_X
                && mouseX <= guiLeft + SELECTOR_BUTTON_X + POPUP_WIDTH
                && mouseY >= guiTop + SELECTOR_BUTTON_Y + SELECTOR_BUTTON_HEIGHT + POPUP_OFFSET_Y
                && mouseY <= guiTop + SELECTOR_BUTTON_Y + SELECTOR_BUTTON_HEIGHT + POPUP_OFFSET_Y + popupHeight;
    }

    private boolean isInRowBounds(double mouseX, double mouseY, int rowX, int rowY, int rowWidth, int rowHeight) {
        return mouseX >= guiLeft + rowX
                && mouseX <= guiLeft + rowX + rowWidth
                && mouseY >= guiTop + rowY
                && mouseY <= guiTop + rowY + rowHeight;
    }
}
