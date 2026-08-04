package com.bobby.bobbychests.client.chest.screen;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.client.chest.screen.tab.UpgradeSlotsTab;
import com.bobby.bobbychests.network.SortChestPayload;
import com.bobby.bobbychests.network.SetGlobalStorageIdPayload;
import com.bobby.bobbychests.network.SetLockedPayload;
import com.bobby.bobbycore.client.gui.GuiExtraAreasScreen;
import com.bobby.bobbycore.client.gui.TabStrip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2i;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractChestScreen<M extends AbstractChestMenu> extends AbstractContainerScreen<M> implements GuiExtraAreasScreen {
    private EditBox editBox;
    private ImageButton lockButtonLocked;
    private ImageButton lockButtonUnlocked;
    private ImageButton sortButton;
    private boolean locked;
    private boolean usingGlobalStorage;
    private int lastSentId = Integer.MIN_VALUE;
    private int pendingId = Integer.MIN_VALUE;
    private long sendAfterMs = 0L;
    private static final long ID_DEBOUNCE_MS = 150L;
    private int maxChannelId;
    private boolean applyingClampedText = false;
    private long clampPopupUntilMs = 0L;
    private boolean clearSortButtonFocusNextTick;
    /** Previous tick's {@link AbstractChestMenu#hasLockUpgradeInstalled()}; used to refresh lock UI when the card is added or removed. */
    private boolean lastHadLockUpgradeInstalled;
    private TabStrip tabStrip;
    private UpgradeSlotsTab upgradesTab;
    private static final long CLAMP_POPUP_MS = 1200L;
    private static final int GUI_MARGIN_PX = 6;
    private static final int ID_BOX_H = 10;
    private static final int ID_BOX_W = 62;
    private static final int ID_BOX_RIGHT_PAD = 15;
    private static final int LOCK_TOGGLE_W = 18;
    private static final int LOCK_TOGGLE_H = 9;
    private static final int LOCK_TOGGLE_LEFT_PAD = 7;
    private static final int SORT_BTN_SIZE = 9;
    private static final int SORT_BTN_GAP_AFTER_ID = 2;
    private static final Identifier SORT_BUTTON = Identifier.fromNamespaceAndPath("bobbychests", "sort_button");
    private static final Identifier SORT_BUTTON_DISABLED = Identifier.fromNamespaceAndPath("bobbychests", "sort_button_disabled");
    private static final Identifier SORT_BUTTON_HIGHLIGHTED = Identifier.fromNamespaceAndPath("bobbychests", "sort_button_highlighted");
    private static final WidgetSprites SORT_BUTTON_SPRITES = new WidgetSprites(SORT_BUTTON, SORT_BUTTON_DISABLED, SORT_BUTTON_HIGHLIGHTED);

    private static final Identifier LOCK_TOGGLE_LOCKED = Identifier.fromNamespaceAndPath("bobbychests", "lock_toggle_locked");
    private static final Identifier LOCK_TOGGLE_LOCKED_DISABLED = Identifier.fromNamespaceAndPath("bobbychests", "lock_toggle_locked_disabled");
    private static final Identifier LOCK_TOGGLE_LOCKED_HIGHLIGHTED = Identifier.fromNamespaceAndPath("bobbychests", "lock_toggle_locked_highlighted");
    private static final WidgetSprites LOCKED_SPRITES = new WidgetSprites(LOCK_TOGGLE_LOCKED, LOCK_TOGGLE_LOCKED_DISABLED, LOCK_TOGGLE_LOCKED_HIGHLIGHTED);

    private static final Identifier LOCK_TOGGLE_UNLOCKED = Identifier.fromNamespaceAndPath("bobbychests", "lock_toggle_unlocked");
    private static final Identifier LOCK_TOGGLE_UNLOCKED_DISABLED = Identifier.fromNamespaceAndPath("bobbychests", "lock_toggle_unlocked_disabled");
    private static final Identifier LOCK_TOGGLE_UNLOCKED_HIGHLIGHTED = Identifier.fromNamespaceAndPath("bobbychests", "lock_toggle_unlocked_highlighted");
    private static final WidgetSprites UNLOCKED_SPRITES = new WidgetSprites(LOCK_TOGGLE_UNLOCKED, LOCK_TOGGLE_UNLOCKED_DISABLED, LOCK_TOGGLE_UNLOCKED_HIGHLIGHTED);

    protected AbstractChestScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.getImageWidthPx(), menu.getImageHeightPx());
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }


    protected int idBoxX() {
        return this.leftPos + this.menu.getChestPanelWidthPx() - ID_BOX_W - ID_BOX_RIGHT_PAD;
    }


    protected int idBoxY() {
        return this.topPos + 5;
    }

    private AbstractTieredChestBlockEntity getChestBlockEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity be = this.minecraft.level.getBlockEntity(this.menu.getChestPos());
        return be instanceof AbstractTieredChestBlockEntity chest ? chest : null;
    }

    @Override
    protected void init() {
        super.init();
        this.clampGuiOnScreen();
        this.locked = this.menu.getInitialLocked();
        this.usingGlobalStorage = this.menu.getInitialUsingGlobalStorage();
        this.maxChannelId = this.menu.getMaxChannelId();

        this.initIdBox();
        this.initSortButton();
        this.initLockToggle();
        this.initTabStrip();
        this.updateStorageModeWidgets();
    }

    private void initTabStrip() {
        this.tabStrip = new TabStrip(this.leftPos + this.menu.getChestPanelWidthPx(), this.topPos + 4);
        this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
        this.upgradesTab = this.tabStrip.addTab(new UpgradeSlotsTab(this.menu));
    }

    private void initIdBox() {
        int idBoxX = this.idBoxX();
        int idBoxY = this.idBoxY();

        // Keep the same visual text position as bordered=true (x+4, y+(h-8)/2),
        // but render with no background/border.
        this.editBox = new EditBox(
                this.font,
                idBoxX + 4,
                idBoxY + (ID_BOX_H - 8) / 2,
                ID_BOX_W - 8,
                ID_BOX_H,
                Component.literal("ID"));
        this.editBox.setMaxLength(9);
        this.editBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
        this.editBox.setBordered(false);
        int initial = clampChannel(this.menu.getInitialChestId());
        this.editBox.setValue(String.valueOf(initial));
        this.lastSentId = initial;
        this.editBox.setResponder(this::onIdEdited);
        this.addRenderableWidget(this.editBox);
    }

    private void initSortButton() {
        if (!this.shouldShowSortButton()) {
            return;
        }

        int sortX = this.idBoxX() + ID_BOX_W + SORT_BTN_GAP_AFTER_ID;
        int sortY = this.idBoxY();
        this.sortButton = new ImageButton(
                sortX,
                sortY,
                SORT_BTN_SIZE,
                SORT_BTN_SIZE,
                SORT_BUTTON_SPRITES,
                btn -> {
                    ClientPacketDistributor.sendToServer(new SortChestPayload(this.menu.getChestPos()));
                    this.clearSortButtonFocusNextTick = true;
                }
        );
        this.sortButton.setTooltip(Tooltip.create(Component.literal("Sort")));
        this.addRenderableWidget(this.sortButton);
    }

    private void initLockToggle() {
        // Lock toggle exists for all chests but is only visible/active when the lock upgrade card is installed.
        int lockX = this.lockToggleX();
        int lockY = this.idBoxY();
        this.lockButtonLocked = new ImageButton(lockX, lockY, LOCK_TOGGLE_W, LOCK_TOGGLE_H, LOCKED_SPRITES, btn -> this.onLockToggleClicked());
        this.lockButtonUnlocked = new ImageButton(lockX, lockY, LOCK_TOGGLE_W, LOCK_TOGGLE_H, UNLOCKED_SPRITES, btn -> this.onLockToggleClicked());
        this.lockButtonLocked.setTooltip(Tooltip.create(Component.literal("Locked")));
        this.lockButtonUnlocked.setTooltip(Tooltip.create(Component.literal("Unlocked")));
        this.addRenderableWidget(this.lockButtonLocked);
        this.addRenderableWidget(this.lockButtonUnlocked);
        this.lastHadLockUpgradeInstalled = this.menu.hasLockUpgradeInstalled();
        this.applyLockToggleUi();
    }

    protected boolean shouldShowSortButton() {
        return true;
    }

    protected final void extractTieredChestGuiBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Identifier textureGlobal, Identifier textureLocal, int textureAtlasSize) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.usingGlobalStorage() ? textureGlobal : textureLocal,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.menu.getChestPanelWidthPx(),
                this.imageHeight,
                textureAtlasSize,
                textureAtlasSize);
        this.renderTabStrip(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderTabStrip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.tabStrip == null) {
            return;
        }
        this.tabStrip.setOrigin(this.leftPos + this.menu.getChestPanelWidthPx(), this.topPos + 4);
        this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
        this.tabStrip.render(graphics, mouseX, mouseY, partialTick);
        this.tabStrip.renderTooltips(this, graphics, mouseX, mouseY);
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        if (this.tabStrip == null) {
            return List.of();
        }
        this.tabStrip.setOrigin(this.leftPos + this.menu.getChestPanelWidthPx(), this.topPos + 4);
        this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
        return this.tabStrip.getGuiExtraAreas();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scrolling is handled by the container (e.g. scrollable chests) rather than moving the whole GUI.
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Syncs UI from the block entity when pooled storage is enabled/disabled by upgrade cards. */
    private void syncGlobalStorageModeFromBlockEntity() {
        AbstractTieredChestBlockEntity chest = this.getChestBlockEntity();
        if (chest == null) {
            return;
        }
        boolean global = chest.getStorageMode() == ChestStorageMode.GLOBAL;
        if (global != this.usingGlobalStorage) {
            if (this.menu instanceof AbstractScrollableChestMenu scm) {
                scm.setScrollRows(0);
            }
            this.usingGlobalStorage = global;
            this.updateStorageModeWidgets();
        }
    }

    private void syncIdBoxFromBlockEntity() {
        if (!this.usingGlobalStorage) {
            return;
        }
        if (this.editBox == null || this.editBox.isFocused()) {
            return;
        }
        AbstractTieredChestBlockEntity chest = this.getChestBlockEntity();
        if (chest == null) {
            return;
        }
        int id = clampChannel(chest.getGlobalStorageId());
        if (id == this.lastSentId) {
            return;
        }
        this.pendingId = Integer.MIN_VALUE;
        this.lastSentId = id;
        String text = String.valueOf(id);
        if (!text.equals(this.editBox.getValue())) {
            this.editBox.setValue(text);
        }
    }

    private void clampGuiOnScreen() {
        // AbstractContainerScreen centers the GUI, but for very wide/tall menus the centered rect can still spill
        // off-screen once extra widgets are considered.
        int minLeft = GUI_MARGIN_PX;
        int maxLeft = this.width - GUI_MARGIN_PX - this.imageWidth;
        if (maxLeft < minLeft) {
            // Not enough horizontal room even with clamping; keep as much on-screen as possible.
            this.leftPos = (this.width - this.imageWidth) / 2;
        } else {
            this.leftPos = Mth.clamp(this.leftPos, minLeft, maxLeft);
        }

        int minTop = GUI_MARGIN_PX;
        int maxTop = this.height - GUI_MARGIN_PX - this.imageHeight;
        if (maxTop < minTop) {
            // Taller than the window: allow vertical scrolling instead of "centering" into negative space.
            this.topPos = Mth.clamp(this.topPos, maxTop, minTop);
        } else {
            this.topPos = Mth.clamp(this.topPos, minTop, maxTop);
        }
    }

    private int lockToggleX() {
        return this.leftPos + LOCK_TOGGLE_LEFT_PAD;
    }

    private void onLockToggleClicked() {
        this.locked = !this.locked;
        this.applyLockToggleUi();
        this.resetScrollMenuOnStorageKeyChange();
        ClientPacketDistributor.sendToServer(new SetLockedPayload(this.menu.getChestPos(), this.locked));
    }

    /**
     * Keeps the lock toggle aligned with the server when the lock upgrade is removed (chest auto-unlocks) or
     * re-inserted, without polling the block entity every tick while the upgrade slot is stable (so optimistic clicks
     * still feel instant).
     */
    private void syncLockStateFromChestWhenUpgradePresenceChanges() {
        boolean hasLockUpgrade = this.menu.hasLockUpgradeInstalled();
        boolean pullLockedFromChest = !hasLockUpgrade || hasLockUpgrade != this.lastHadLockUpgradeInstalled;
        AbstractTieredChestBlockEntity chest = this.getChestBlockEntity();
        if (chest != null) {
            if (pullLockedFromChest) {
                this.locked = chest.isLocked();
            }
            this.lastHadLockUpgradeInstalled = hasLockUpgrade;
        }
        this.applyLockToggleUi();
    }

    private void applyLockToggleUi() {
        if (this.lockButtonLocked == null || this.lockButtonUnlocked == null) {
            return;
        }
        boolean hasUpgrade = this.menu.hasLockUpgradeInstalled();
        if (!hasUpgrade) {
            this.lockButtonLocked.visible = false;
            this.lockButtonUnlocked.visible = false;
            this.lockButtonLocked.active = false;
            this.lockButtonUnlocked.active = false;
            return;
        }

        this.lockButtonLocked.visible = this.locked;
        this.lockButtonUnlocked.visible = !this.locked;
        this.lockButtonLocked.active = true;
        this.lockButtonUnlocked.active = true;
    }

    protected final boolean usingGlobalStorage() {
        return this.usingGlobalStorage;
    }

    private void updateStorageModeWidgets() {
        if (this.editBox != null) {
            this.editBox.visible = this.usingGlobalStorage;
            this.editBox.active = this.usingGlobalStorage;
        }
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
        if (clamped == this.lastSentId) {
            this.pendingId = Integer.MIN_VALUE;
            return;
        }
        this.pendingId = clamped;
        this.sendAfterMs = Util.getMillis() + ID_DEBOUNCE_MS;
    }

    /**
     * Subclasses tweak scroll position when identity keys change mid-interaction; see
     * {@link AbstractScrollableChestScreen#resetScrollMenuOnStorageKeyChange()}.
     */
    protected void resetScrollMenuOnStorageKeyChange() {}

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.tabStrip != null) {
            this.tabStrip.setOrigin(this.leftPos + this.menu.getChestPanelWidthPx(), this.topPos + 4);
            this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
            this.tabStrip.tick();
        }
        this.clearSortButtonFocusIfQueued();
        this.syncGlobalStorageModeFromBlockEntity();
        this.syncLockStateFromChestWhenUpgradePresenceChanges();
        this.syncIdBoxFromBlockEntity();
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

    private void clearSortButtonFocusIfQueued() {
        if (!this.clearSortButtonFocusNextTick) {
            return;
        }
        this.clearSortButtonFocusNextTick = false;
        if (this.sortButton == null) {
            return;
        }
        this.sortButton.setFocused(false);
        if (this.getFocused() == this.sortButton) {
            this.setFocused(null);
        }
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
        if (this.tabStrip != null && event.button() == 0
                && this.tabStrip.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot instanceof AbstractChestMenu.UpgradeSlot upgradeSlot
                && upgradeSlot.isActive()
                && !this.menu.getCarried().isEmpty()) {
            Optional<Component> deny = this.menu.getUpgradeInstallDenyReason(
                    this.menu.getCarried(),
                    this.hoveredSlot.getContainerSlot());
            if (deny.isPresent()) {
                graphics.setTooltipForNextFrame(this.font, List.of(deny.get()), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
        DeepStorageSlotRenderer.renderSlotCount(graphics, this.font, this.menu, slot);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        return DeepStorageSlotRenderer.appendStoredCountTooltip(itemStack, super.getTooltipFromContainerItem(itemStack));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.editBox == null || Util.getMillis() > this.clampPopupUntilMs) {
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

