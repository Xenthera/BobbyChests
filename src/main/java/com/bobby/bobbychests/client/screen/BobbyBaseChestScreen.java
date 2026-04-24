package com.bobby.bobbychests.client.screen;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.menu.BobbyBaseChestMenu;
import com.bobby.bobbychests.network.SetGlobalStorageIdPayload;
import com.bobby.bobbychests.network.SetLockedPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class BobbyBaseChestScreen extends AbstractContainerScreen<BobbyBaseChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_54.png");

    private EditBox editBox;
    private Button lockButton;
    private boolean locked;
    private int lastSentId = Integer.MIN_VALUE;
    private int pendingId = Integer.MIN_VALUE;
    private long sendAfterMs = 0L;
    private static final long ID_DEBOUNCE_MS = 150L;
    public BobbyBaseChestScreen(BobbyBaseChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 114 + 6 * 18); // width, height of your background
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        int idBoxX = (this.width / 2) + 11;
        int idBoxY = this.topPos + 5;
        int idBoxW = 62;
        int idBoxH = 10;

        this.locked = this.menu.getInitialLocked();

        // Keep the same visual text position as bordered=true (x+4, y+(h-8)/2),
        // but render with no background/border.
        editBox = new EditBox(this.font, idBoxX + 4, idBoxY + (idBoxH - 8) / 2, idBoxW - 8, idBoxH, Component.literal("ID"));
        editBox.setMaxLength(9);
        editBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
        editBox.setBordered(false);
        editBox.setValue(String.valueOf(this.menu.getInitialChestId()));
        this.lastSentId = this.menu.getInitialChestId();
        editBox.setResponder(this::onIdEdited);
        this.addRenderableWidget(editBox);

        // Place the lock button outside the menu to the right.
        int lockSize = 18;
        int lockX = this.leftPos + this.imageWidth + 6;
        int lockY = this.topPos;
        this.lockButton = Button.builder(lockLabel(this.locked), btn -> {
            this.locked = !this.locked;
            btn.setMessage(lockLabel(this.locked));
            ClientPacketDistributor.sendToServer(new SetLockedPayload(this.menu.getChestPos(), this.locked));
        }).bounds(lockX, lockY, lockSize, lockSize).build();
        this.addRenderableWidget(this.lockButton);
    }

    private static Component lockLabel(boolean locked) {
        return Component.literal(locked ? "🔒" : "🔓");
    }

    private void onIdEdited(String value) {
        int id = 0;
        if (!value.isBlank()) {
            try {
                id = Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                id = 0;
            }
        }
        if (id == this.lastSentId) {
            this.pendingId = Integer.MIN_VALUE;
            return;
        }
        this.pendingId = id;
        this.sendAfterMs = Util.getMillis() + ID_DEBOUNCE_MS;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.pendingId == Integer.MIN_VALUE) {
            return;
        }
        if (Util.getMillis() < this.sendAfterMs) {
            return;
        }
        int id = this.pendingId;
        this.pendingId = Integer.MIN_VALUE;
        this.lastSentId = id;
        ClientPacketDistributor.sendToServer(new SetGlobalStorageIdPayload(this.menu.getChestPos(), id));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BG,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                256, 256
        );
    }
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // intentionally blank
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inBounds) {
        // Let vanilla handle slots + widget focus first.
        boolean handled = super.mouseClicked(event, inBounds);
        if (this.editBox != null && event.button() == 0) { // left click
            double mx = event.x();
            double my = event.y();
            // If you clicked the textbox, ensure it can regain focus.
            if (this.editBox.isMouseOver(mx, my)) {
                this.editBox.setFocused(true);
                if(this.editBox.getValue().equals("0")){
                    this.editBox.setValue("");
                }
                return true;
            }
            // If you clicked anywhere outside the textbox, unfocus it.
            if (this.editBox.isFocused()) {
                this.editBox.setFocused(false);
            }
        }

        return handled;
    }

}
