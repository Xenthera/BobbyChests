package com.bobby.bobbychests.client.screen;

import com.bobby.bobbychests.menu.AbstractChestMenu;
import com.bobby.bobbychests.network.SetGlobalStorageIdPayload;
import com.bobby.bobbychests.network.SetLockedPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2i;

import java.util.List;
import java.util.Optional;

public abstract class AbstractChestScreen<M extends AbstractChestMenu> extends AbstractContainerScreen<M> {
    private EditBox editBox;
    private Button lockButton;
    private boolean locked;
    private int lastSentId = Integer.MIN_VALUE;
    private int pendingId = Integer.MIN_VALUE;
    private long sendAfterMs = 0L;
    private static final long ID_DEBOUNCE_MS = 150L;
    private int maxChannelId;
    private boolean applyingClampedText = false;
    private long clampPopupUntilMs = 0L;
    private static final long CLAMP_POPUP_MS = 1200L;

    protected AbstractChestScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.getImageWidthPx(), menu.getImageHeightPx());
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }


    protected int idBoxX() {
        return (this.width / 2) + 11;
    }


    protected int idBoxY() {
        return this.topPos + 5;
    }

    @Override
    protected void init() {
        super.init();
        this.clampGuiOnScreen();
        int idBoxX = this.idBoxX();
        int idBoxY = this.idBoxY();
        int idBoxW = 62;
        int idBoxH = 10;

        this.locked = this.menu.getInitialLocked();
        this.maxChannelId = this.menu.getMaxChannelId();

        // Keep the same visual text position as bordered=true (x+4, y+(h-8)/2),
        // but render with no background/border.
        this.editBox = new EditBox(this.font, idBoxX + 4, idBoxY + (idBoxH - 8) / 2, idBoxW - 8, idBoxH, Component.literal("ID"));
        this.editBox.setMaxLength(9);
        this.editBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
        this.editBox.setBordered(false);
        int initial = clampChannel(this.menu.getInitialChestId());
        this.editBox.setValue(String.valueOf(initial));
        this.lastSentId = initial;
        this.editBox.setResponder(this::onIdEdited);
        this.addRenderableWidget(this.editBox);

        // Place the lock button outside the menu to the right.
        int lockSize = 18;
        int lockX = this.computeLockButtonX(lockSize);
        int lockY = this.topPos;
        this.lockButton = Button.builder(lockLabel(this.locked), btn -> {
            this.locked = !this.locked;
            btn.setMessage(lockLabel(this.locked));
            this.resetScrollMenuOnStorageKeyChange();
            ClientPacketDistributor.sendToServer(new SetLockedPayload(this.menu.getChestPos(), this.locked));
        }).bounds(lockX, lockY, lockSize, lockSize).build();
        this.addRenderableWidget(this.lockButton);

        this.repositionChromeWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.delegateContainerMouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        return this.tryScrollTallGuiOnMouseWheel(mouseX, mouseY, scrollX, scrollY);
    }

    /** Bundle / item-slot mouse wheel handling from {@link AbstractContainerScreen}. */
    protected boolean delegateContainerMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** When the chest GUI is taller than the window, nudge the whole container vertically with the wheel. */
    protected boolean tryScrollTallGuiOnMouseWheel(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.needsGuiVerticalScroll()) {
            return false;
        }
        if (scrollY == 0.0D) {
            return false;
        }

        if (!this.isMouseOverChestGui(mouseX, mouseY)) {
            return false;
        }

        int margin = 6;
        int minTop = margin;
        int maxTop = this.height - margin - this.imageHeight;
        int step = 18; // one slot row at a time feels natural for chest UIs
        int delta = (int) Math.signum(scrollY) * step;
        this.topPos = Mth.clamp(this.topPos - delta, maxTop, minTop);
        this.repositionChromeWidgets();
        return true;
    }

    private boolean needsGuiVerticalScroll() {
        int margin = 6;
        return this.imageHeight + (2 * margin) > this.height;
    }

    private boolean isMouseOverChestGui(double mouseX, double mouseY) {
        return mouseX >= this.leftPos
                && mouseY >= this.topPos
                && mouseX < this.leftPos + this.imageWidth
                && mouseY < this.topPos + this.imageHeight;
    }

    private void repositionChromeWidgets() {
        if (this.editBox == null || this.lockButton == null) {
            return;
        }
        int idBoxW = 62;
        int idBoxH = 10;
        int idBoxX = this.idBoxX();
        int idBoxY = this.idBoxY();
        this.editBox.setX(idBoxX + 4);
        this.editBox.setY(idBoxY + (idBoxH - 8) / 2);

        int lockSize = 18;
        int lockX = this.computeLockButtonX(lockSize);
        int lockY = this.topPos;
        this.lockButton.setX(lockX);
        this.lockButton.setY(lockY);
    }

    private void clampGuiOnScreen() {
        // AbstractContainerScreen centers the GUI, but for very wide/tall menus the centered rect can still spill
        // off-screen once extra widgets (lock button) are considered.
        int margin = 6;
        int lockSize = 18;
        int lockPad = 6;
        int extraRight = lockSize + lockPad;

        int minLeft = margin;
        int maxLeft = this.width - margin - this.imageWidth - extraRight;
        if (maxLeft < minLeft) {
            // Not enough horizontal room even with clamping; keep as much on-screen as possible.
            this.leftPos = (this.width - this.imageWidth) / 2;
        } else {
            this.leftPos = Mth.clamp(this.leftPos, minLeft, maxLeft);
        }

        int minTop = margin;
        int maxTop = this.height - margin - this.imageHeight;
        if (maxTop < minTop) {
            // Taller than the window: allow vertical scrolling instead of "centering" into negative space.
            this.topPos = Mth.clamp(this.topPos, maxTop, minTop);
        } else {
            this.topPos = Mth.clamp(this.topPos, minTop, maxTop);
        }
    }

    private int computeLockButtonX(int lockSize) {
        int desired = this.leftPos + this.imageWidth + 6;
        int margin = 6;
        int maxX = this.width - margin - lockSize;
        // Prefer "outside to the right", but fall back inward if it would render off-screen.
        return Math.min(desired, maxX);
    }

    private static Component lockLabel(boolean locked) {
        return Component.literal(locked ? "🔒" : "🔓");
    }

    private void onIdEdited(String value) {
        if (this.applyingClampedText) {
            return;
        }
        // Allow the user to temporarily clear the field while editing.
        // We'll coerce to a valid number only when the box loses focus.
        if (value.isBlank()) {
            this.pendingId = Integer.MIN_VALUE;
            return;
        }
        int id = 0;
        try {
            id = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            id = 0;
        }
        int clamped = clampChannel(id);
        if (!value.isBlank()) {
            String clampedText = String.valueOf(clamped);
            if (!clampedText.equals(value) && this.editBox != null) {
                showClampPopup();
                this.applyingClampedText = true;
                this.editBox.setValue(clampedText);
                this.applyingClampedText = false;
                // Ensure the server gets the clamped value (the setValue() will re-trigger this responder,
                // but that's a client-only rewrite; we still need to send).
                if (clamped != this.lastSentId) {
                    this.pendingId = clamped;
                    this.sendAfterMs = Util.getMillis();
                } else {
                    this.pendingId = Integer.MIN_VALUE;
                }
                // The setValue() will re-trigger this responder; no need to continue.
                return;
            }
        }
        if (clamped == this.lastSentId) {
            this.pendingId = Integer.MIN_VALUE;
            return;
        }
        this.pendingId = clamped;
        this.sendAfterMs = Util.getMillis() + ID_DEBOUNCE_MS;
    }

    /**
     * Scrollable chest screens override this to reset row scroll when the player edits channel id or toggles lock
     * before packets apply.
     */
    protected void resetScrollMenuOnStorageKeyChange() {}

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.pendingId == Integer.MIN_VALUE) {
            return;
        }
        if (Util.getMillis() < this.sendAfterMs) {
            return;
        }
        int id = clampChannel(this.pendingId);
        this.pendingId = Integer.MIN_VALUE;
        this.lastSentId = id;
        if (this.editBox != null && !this.editBox.isFocused()) {
            String clampedText = String.valueOf(id);
            if (!clampedText.equals(this.editBox.getValue())) {
                this.editBox.setValue(clampedText);
            }
        }
        this.resetScrollMenuOnStorageKeyChange();
        ClientPacketDistributor.sendToServer(new SetGlobalStorageIdPayload(this.menu.getChestPos(), id));
    }

    private int clampChannel(int id) {
        if (id < 0) {
            return 0;
        }
        if (this.maxChannelId <= 0) {
            return id;
        }
        return Math.min(id, this.maxChannelId);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inBounds) {
        boolean handled = super.mouseClicked(event, inBounds);
        if (this.editBox != null && event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            if (this.editBox.isMouseOver(mx, my)) {
                this.editBox.setFocused(true);
                if (this.editBox.getValue().equals("0")) {
                    this.editBox.setValue("");
                }
                return true;
            }
            if (this.editBox.isFocused()) {
                // Clamp immediately when leaving the box, even if debounce hasn't fired yet.
                int id = 0;
                String value = this.editBox.getValue();
                if (!value.isBlank()) {
                    try {
                        id = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        id = 0;
                    }
                } else {
                    // If left blank, treat as 0 on blur.
                    id = 0;
                }
                int clamped = clampChannel(id);
                String clampedText = String.valueOf(clamped);
                if (!clampedText.equals(value)) {
                    showClampPopup();
                    this.applyingClampedText = true;
                    this.editBox.setValue(clampedText);
                    this.applyingClampedText = false;
                }
                // Ensure the change is sent even if the user cleared the field.
                if (clamped != this.lastSentId) {
                    this.pendingId = clamped;
                    this.sendAfterMs = Util.getMillis();
                }
                this.editBox.setFocused(false);
            }
        }
        return handled;
    }

    private void showClampPopup() {
        this.clampPopupUntilMs = Util.getMillis() + CLAMP_POPUP_MS;
    }

    @Override
    protected void extractLabels(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.editBox == null) {
            return;
        }
        if (Util.getMillis() > this.clampPopupUntilMs) {
            return;
        }
        Component msg = Component.literal("Max channels " + this.maxChannelId);

        int tipX = this.editBox.getX();
        int tipY = this.editBox.getY() + this.editBox.getHeight() + 4;
        ClientTooltipPositioner positioner = (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) -> {
            int clampedX = Math.max(0, Math.min(x, screenWidth - tooltipWidth));
            int clampedY = Math.max(0, Math.min(y, screenHeight - tooltipHeight));
            return new Vector2i(clampedX, clampedY);
        };
        graphics.setTooltipForNextFrame(
                this.font,
                List.of(msg.getVisualOrderText()),
                Optional.<TooltipComponent>empty(),
                positioner,
                tipX,
                tipY,
                true,
                null
        );
    }
}

