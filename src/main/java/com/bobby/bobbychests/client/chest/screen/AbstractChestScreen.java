package com.bobby.bobbychests.client.chest.screen;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.client.chest.screen.tab.LockFeatureTab;
import com.bobby.bobbychests.client.chest.screen.tab.NetworkFeatureTab;
import com.bobby.bobbychests.client.chest.screen.tab.UpgradeSlotsTab;
import com.bobby.bobbychests.network.SortChestPayload;
import com.bobby.bobbychests.network.SetGlobalStorageIdPayload;
import com.bobby.bobbychests.network.SetLockedPayload;
import com.bobby.bobbycore.client.gui.GuiExtraAreasScreen;
import com.bobby.bobbycore.client.gui.TabStrip;
import com.bobby.bobbycore.client.gui.ThemedContainerScreen;
import com.bobby.bobbycore.client.gui.ThemedScreenChrome;
import com.bobby.bobbycore.client.gui.draw.ScreenHeader;
import com.bobby.bobbycore.client.gui.draw.SlotChrome;
import com.bobby.bobbycore.client.gui.draw.UiDraw;
import com.bobby.bobbycore.client.gui.font.BobbyFonts;
import com.bobby.bobbycore.client.gui.layout.GuiLayout;
import com.bobby.bobbycore.client.gui.scroll.WindowedContainerSlot;
import com.bobby.bobbycore.client.gui.theme.BobbyThemes;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import com.bobby.bobbycore.client.gui.widget.UiButton;
import com.bobby.bobbycore.client.gui.widget.UiCheckbox;
import com.bobby.bobbycore.client.gui.widget.UiTextBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
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

public abstract class AbstractChestScreen<M extends AbstractChestMenu> extends ThemedContainerScreen<M> implements GuiExtraAreasScreen {
    private UiTextBox editBox;
    private UiCheckbox lockCheckbox;
    private UiButton sortButton;
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
    private LockFeatureTab lockTab;
    private NetworkFeatureTab networkTab;
    private UpgradeSlotsTab upgradesTab;
    private static final long CLAMP_POPUP_MS = 1200L;
    private static final int GUI_MARGIN_PX = 6;
    private static final int ID_BOX_H = 12;
    private static final int ID_BOX_W = 54;
    private static final int SORT_BTN_W = 30;
    private static final int SORT_RIGHT_PAD = 6;

    protected static final UiTheme UI_THEME = BobbyThemes.BOBBY_DARK;

    private ItemStack headerIcon = ItemStack.EMPTY;

    protected AbstractChestScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.getImageWidthPx(), menu.getImageHeightPx());
        this.titleLabelX = ScreenHeader.titleX();
        this.titleLabelY = this.uiTheme().titlePadY();
        this.inventoryLabelX = GuiLayout.centeredPlayerInventoryOriginX(menu.getChestPanelWidthPx());
        int invTop = menu.getPlayerInventoryTopY();
        // Sit the "Inventory" label with more air under the content/inventory divider.
        this.inventoryLabelY = invTop > 0 ? invTop - 14 : this.imageHeight - 94;
    }

    /** Absolute Y that vertically centers a header widget of {@code height} px. */
    protected int headerWidgetY(int height) {
        return this.topPos + this.uiTheme().headerCenteredY(height);
    }

    /** Per-tier content tint (header/body/inventory/slots). */
    protected UiTheme uiTheme() {
        return ChestGuiThemes.forMenu(this.menu);
    }


    protected int sortButtonX() {
        return this.leftPos + this.menu.getChestPanelWidthPx() - SORT_BTN_W - SORT_RIGHT_PAD;
    }

    protected int sortButtonY() {
        return this.headerWidgetY(ID_BOX_H);
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
        this.headerIcon = ScreenHeader.blockIcon(this.menu.getChestPos());
        this.clampGuiOnScreen();
        this.locked = this.menu.getInitialLocked();
        this.usingGlobalStorage = this.menu.getInitialUsingGlobalStorage();
        this.maxChannelId = this.menu.getMaxChannelId();

        this.initIdBox();
        this.initSortButton();
        this.initLockToggle();
        this.ensureTabStrip();
        this.syncFeatureTabs();
    }

    private void ensureTabStrip() {
        if (this.tabStrip != null) {
            return;
        }
        this.tabStrip = new TabStrip(this.leftPos + this.menu.getChestPanelWidthPx(), this.tabStripOriginY());
        this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
    }

    /** Screen Y for the first tab: 1px below the panel header bottom. */
    private int tabStripOriginY() {
        return this.topPos + GuiLayout.tabStripOriginY();
    }

    /**
     * Feature tabs appear only for upgrades that need controls: lock, network, and the upgrade-card strip.
     * Adds/removes tabs in place so an already-open upgrades tab stays open when cards are installed.
     * Order: upgrades, then lock, then network.
     */
    private void syncFeatureTabs() {
        this.ensureTabStrip();
        boolean wantLock = this.menu.hasLockUpgradeInstalled();
        boolean wantNetwork = this.menu.hasNetworkingUpgradeInstalled();
        boolean wantUpgrades = this.menu.getUpgradeSlotCount() > 0;

        if (wantUpgrades && this.upgradesTab == null) {
            this.upgradesTab = this.tabStrip.addTab(0, new UpgradeSlotsTab(this.menu));
        } else if (!wantUpgrades && this.upgradesTab != null) {
            this.tabStrip.removeTab(this.upgradesTab);
            this.upgradesTab = null;
        }

        if (wantLock && this.lockTab == null) {
            int index = this.upgradesTab != null ? 1 : 0;
            this.lockTab = this.tabStrip.addTab(index, new LockFeatureTab());
        } else if (!wantLock && this.lockTab != null) {
            this.tabStrip.removeTab(this.lockTab);
            this.lockTab = null;
        }

        if (wantNetwork && this.networkTab == null) {
            int index = 0;
            if (this.upgradesTab != null) {
                index++;
            }
            if (this.lockTab != null) {
                index++;
            }
            this.networkTab = this.tabStrip.addTab(index, new NetworkFeatureTab(ID_BOX_W, ID_BOX_H));
        } else if (!wantNetwork && this.networkTab != null) {
            this.tabStrip.removeTab(this.networkTab);
            this.networkTab = null;
        }

        this.layoutFeatureWidgets();
    }

    /** Parks lock/network widgets inside their open feature tabs; hides them otherwise. */
    private void layoutFeatureWidgets() {
        boolean showLock = this.lockTab != null && this.lockTab.contentVisible();
        if (this.lockCheckbox != null) {
            if (showLock) {
                this.lockCheckbox.setPosition(
                        this.lockTab.contentWidgetX(this.lockCheckbox.getWidth()),
                        this.lockTab.contentWidgetY());
            }
            this.lockCheckbox.visible = showLock;
            this.lockCheckbox.active = showLock;
            if (this.lockCheckbox.selected() != this.locked) {
                this.lockCheckbox.setSelected(this.locked);
            }
        }

        boolean showNetwork = this.networkTab != null && this.networkTab.contentVisible();
        if (this.editBox != null) {
            if (showNetwork) {
                this.editBox.setPosition(this.networkTab.idBoxX(), this.networkTab.idBoxY());
            } else if (this.editBox.isFocused()) {
                this.editBox.setFocused(false);
                if (this.getFocused() == this.editBox) {
                    this.setFocused(null);
                }
            }
            this.editBox.visible = showNetwork;
            this.editBox.active = showNetwork;
        }
    }

    private void initIdBox() {
        int initial = clampChannel(this.menu.getInitialChestId());
        this.editBox = UiTextBox.builder(this.font, Component.literal("ID"))
                .bounds(0, 0, ID_BOX_W, ID_BOX_H)
                .theme(NetworkFeatureTab.tabTheme())
                .maxLength(9)
                .filter(s -> s.chars().allMatch(Character::isDigit))
                .responder(this::onIdEdited)
                .value(String.valueOf(initial))
                .build();
        this.editBox.visible = false;
        this.editBox.active = false;
        this.lastSentId = initial;
        this.addRenderableWidget(this.editBox);
    }

    private void initSortButton() {
        if (!this.shouldShowSortButton()) {
            return;
        }

        this.sortButton = UiButton.builder(Component.literal("Sort"), btn -> {
                    ClientPacketDistributor.sendToServer(new SortChestPayload(this.menu.getChestPos()));
                    this.clearSortButtonFocusNextTick = true;
                })
                .bounds(this.sortButtonX(), this.sortButtonY(), SORT_BTN_W, ID_BOX_H)
                .theme(this.uiTheme())
                .tooltip(Tooltip.create(Component.literal("Sort")))
                .build();
        this.addRenderableWidget(this.sortButton);
    }

    private void initLockToggle() {
        this.lockCheckbox = UiCheckbox.builder(BobbyFonts.literal("Locked"), selected -> {
                    this.locked = selected;
                    this.resetScrollMenuOnStorageKeyChange();
                    ClientPacketDistributor.sendToServer(new SetLockedPayload(this.menu.getChestPos(), this.locked));
                })
                .selected(this.locked)
                .theme(LockFeatureTab.tabTheme())
                .tint(LockFeatureTab.TINT)
                .tooltip(Tooltip.create(Component.literal("Lock this chest")))
                .build();
        this.lockCheckbox.visible = false;
        this.lockCheckbox.active = false;
        this.addRenderableWidget(this.lockCheckbox);
        this.lastHadLockUpgradeInstalled = this.menu.hasLockUpgradeInstalled();
    }

    protected boolean shouldShowSortButton() {
        return true;
    }

    /**
     * Programmatic panel + slot chrome. Texture args are unused (kept so existing tiered
     * screen call sites compile without churn).
     */
    protected final void extractTieredChestGuiBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            Identifier textureGlobal,
            Identifier textureLocal,
            int textureAtlasSize) {
        this.drawThemedChestBackground(graphics, mouseX, mouseY, partialTick);
    }

    protected final void drawThemedChestBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int panelW = this.menu.getChestPanelWidthPx();
        int invTop = this.menu.getPlayerInventoryTopY();
        // Divider sits above the inventory label so the label isn't jammed under the content rule.
        int invBand = invTop > 0 ? Math.min(invTop - 16, this.inventoryLabelY - 3) : -1;
        UiTheme theme = this.uiTheme();
        ThemedScreenChrome.drawPanel(
                graphics, theme, this.leftPos, this.topPos, panelW, this.imageHeight, invBand);
        UiDraw.contentPaneBorder(
                graphics, theme, this.leftPos, this.topPos, panelW, this.imageHeight, invBand);
        SlotChrome.drawMenuSlots(
                graphics, theme, this.leftPos, this.topPos, this.menu.slots, this.menu.getChestSlotCount());
        this.renderTabStrip(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderTabStrip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.tabStrip == null) {
            return;
        }
        this.tabStrip.setOrigin(this.leftPos + this.menu.getChestPanelWidthPx(), this.tabStripOriginY());
        this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
        this.tabStrip.render(graphics, mouseX, mouseY, partialTick);
        // Widgets track tab Y after render's interpolated stack layout.
        this.layoutFeatureWidgets();
        this.tabStrip.renderTooltips(this, graphics, mouseX, mouseY);
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        if (this.tabStrip == null) {
            return List.of();
        }
        this.tabStrip.setOrigin(this.leftPos + this.menu.getChestPanelWidthPx(), this.tabStripOriginY());
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
            this.syncFeatureTabs();
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

    /**
     * Keeps the lock checkbox aligned with the server when the lock upgrade is removed (chest auto-unlocks) or
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
    }

    protected final boolean usingGlobalStorage() {
        return this.usingGlobalStorage;
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
        this.syncGlobalStorageModeFromBlockEntity();
        this.syncLockStateFromChestWhenUpgradePresenceChanges();
        this.syncFeatureTabs();
        if (this.tabStrip != null) {
            this.tabStrip.setOrigin(this.leftPos + this.menu.getChestPanelWidthPx(), this.tabStripOriginY());
            this.tabStrip.setAttachmentOffset(this.menu.getUpgradeTabAttachmentOffset());
            this.tabStrip.tick();
        }
        this.layoutFeatureWidgets();
        this.clearSortButtonFocusIfQueued();
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

    /**
     * Scrollable chests only: hovered storage slot as {@code (col, row)} at the
     * bottom-right of the content pane (scroll-aware for windowed slots).
     */
    private void drawHoveredChestSlotCoord(GuiGraphicsExtractor graphics, UiTheme theme) {
        if (!(this.menu instanceof AbstractScrollableChestMenu)) {
            return;
        }
        Slot slot = this.hoveredSlot;
        if (slot == null || slot.index < 0 || slot.index >= this.menu.getChestSlotCount()) {
            return;
        }
        if (slot instanceof AbstractChestMenu.UpgradeSlot) {
            return;
        }
        int col;
        int row;
        if (slot instanceof WindowedContainerSlot windowed) {
            col = windowed.column();
            row = windowed.logicalRow();
        } else {
            int cols = Math.max(1, this.menu.getChestGridColumns());
            int index = slot.getContainerSlot();
            col = index % cols;
            row = index / cols;
        }
        String text = "(" + col + ", " + row + ")";
        float scale = 0.75F;
        int panelW = this.menu.getChestPanelWidthPx();
        int invTop = this.menu.getPlayerInventoryTopY();
        int invBand = invTop > 0 ? Math.min(invTop - 16, this.inventoryLabelY - 3) : this.imageHeight;
        int contentBottom = invBand > 0 ? invBand : this.imageHeight;
        int textW = Math.round(this.font.width(text) * scale);
        int textH = Math.round(8 * scale);
        int labelX = panelW - 6 - textW;
        int labelY = contentBottom - textH - 3;
        graphics.pose().pushMatrix();
        graphics.pose().translate(labelX, labelY);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, BobbyFonts.literal(text), 0, 0, theme.labelMuted(), false);
        graphics.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        UiTheme theme = this.uiTheme();
        ScreenHeader.draw(graphics, this.font, theme, this.title, this.headerIcon, theme.labelPrimary());
        graphics.text(
                this.font,
                BobbyFonts.apply(this.playerInventoryTitle),
                this.inventoryLabelX,
                this.inventoryLabelY,
                theme.labelPrimary(),
                false);
        this.drawHoveredChestSlotCoord(graphics, theme);

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

