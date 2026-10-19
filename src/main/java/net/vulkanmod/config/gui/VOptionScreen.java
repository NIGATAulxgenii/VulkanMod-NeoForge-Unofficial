package net.vulkanmod.config.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.SearchHelper;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.config.gui.widget.ModIconWidget;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.gui.widget.VButtonWidget;
import net.vulkanmod.config.gui.widget.VTextInputWidget;
import net.vulkanmod.config.option.CyclingOption;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.config.option.OptionPage;
import net.vulkanmod.config.option.PerformanceImpact;
import net.vulkanmod.vulkan.util.ColorUtil;

public class VOptionScreen extends Screen {
   public static final int MARGIN = 10;
   private final Screen parent;
   private final List<ModSettingsEntry> modSettingsEntries;
   private final List<OptionPage> optionPages;
   private OptionPage searchResultsPage;
   private int currentListIdx = 0;
   private boolean isSearchActive = false;
   private int tooltipX;
   private int tooltipY;
   private int tooltipWidth;
   private VButtonWidget applyButton;
   private VButtonWidget undoButton;
   private VTextInputWidget searchField;
   private final List<VAbstractWidget> iconWidgets = Lists.newArrayList();
   private final List<VButtonWidget> pageButtons = Lists.newArrayList();
   private final List<VButtonWidget> buttons = Lists.newArrayList();

   public VOptionScreen(Component title, Screen parent) {
      super(title);
      this.parent = parent;
      this.optionPages = new ArrayList<>();
      this.modSettingsEntries = new ArrayList<>(ModSettingsRegistry.INSTANCE.getModEntries());
   }

   protected void init() {
      this.initOptionsPages();
      if (this.optionPages.isEmpty()) {
         throw new IllegalStateException("Default Options weren't added!");
      } else {
         int top = 32;
         int bottom = 60;
         int itemHeight = 20;
         int leftMargin = 116;
         int listWidth = Math.min(this.width - leftMargin - 10, 420);
         int listHeight = this.height - top - bottom;
         this.buildLists(leftMargin, top, listWidth, listHeight, itemHeight);
         this.searchField = this.createSearchField();
         int x = leftMargin + listWidth + 6;
         int tooltipWidth = Math.min(this.width - x - 10, 420);
         int y = top + itemHeight + 6;
         if (tooltipWidth < 200) {
            x = leftMargin + 3;
            tooltipWidth = listWidth;
            y = this.height - bottom + 10;
         }

         this.tooltipX = x;
         this.tooltipY = y;
         this.tooltipWidth = tooltipWidth;
         this.buildPage();
         this.applyButton.active = false;
         this.undoButton.visible = false;
      }
   }

   private void initOptionsPages() {
      this.optionPages.clear();

      for (ModSettingsEntry modPageSet : this.modSettingsEntries) {
         modPageSet.initPages();
         this.optionPages.addAll(modPageSet.getPages());
      }
   }

   private VTextInputWidget createSearchField() {
      int rightMargin = 10;
      int padding = 10;
      int kofiWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + padding;
      int topBarRight = this.width - kofiWidth - rightMargin;
      if (UpdateChecker.isUpdateAvailable()) {
         int updateWidth = this.minecraft.font.width(Component.translatable("vulkanmod.options.buttons.update_available")) + padding;
         topBarRight -= updateWidth + 5;
      }

      int leftMargin = 116;
      int width = Math.min(topBarRight - leftMargin - 4, 413);
      return new VTextInputWidget(
         leftMargin, 4, width, 20, Component.translatable("vulkanmod.options.searchFieldPlaceholder"), widget -> this.performSearch(widget.getInput())
      );
   }

   private void buildLists(int left, int top, int listWidth, int listHeight, int itemHeight) {
      for (OptionPage page : this.optionPages) {
         page.createList(left, top, listWidth, listHeight, itemHeight);
         page.updateOptionStates();
      }
   }

   private void performSearch(String query) {
      if (query != null && !query.trim().isEmpty()) {
         String searchTerm = query.toLowerCase().trim();
         List<OptionBlock> searchResults = new ArrayList<>();

         for (OptionPage page : this.optionPages) {
            List<Option<?>> matchingOptions = new ArrayList<>();

            for (OptionBlock block : page.optionBlocks) {
               for (Option<?> option : block.options()) {
                  boolean matches = false;
                  String optionName = option.getName().getString().toLowerCase();
                  String optionTooltip = option.getTooltip() != null ? option.getTooltip().getString().toLowerCase() : "";
                  String displayedValue = option.getDisplayedValue().getString().toLowerCase();
                  if (optionName.contains(searchTerm) || optionTooltip.contains(searchTerm) || displayedValue.contains(searchTerm)) {
                     matches = true;
                  } else if (option instanceof CyclingOption<?> cycling && SearchHelper.matchesAnyValue(cycling, searchTerm)) {
                     matches = true;
                  }

                  if (matches) {
                     matchingOptions.add(option);
                  }
               }
            }

            if (!matchingOptions.isEmpty()) {
               searchResults.add(new OptionBlock("§l" + page.name, matchingOptions.toArray(new Option[0])));
               searchResults.add(new OptionBlock("", new Option[0]));
            }
         }

         this.searchResultsPage = new OptionPage("Search Results", searchResults.toArray(new OptionBlock[0]));
         int top = 32;
         int bottom = 60;
         int itemHeight = 20;
         int leftMargin = 116;
         int listWidth = Math.min(this.width - leftMargin - 10, 420);
         int listHeight = this.height - top - bottom;
         this.searchResultsPage.createList(leftMargin, top, listWidth, listHeight, itemHeight);
         this.isSearchActive = true;
         this.buildPage();
      } else {
         this.isSearchActive = false;
         this.currentListIdx = 0;
         this.buildPage();
      }
   }

   private void buildPage() {
      this.buttons.clear();
      this.pageButtons.clear();
      this.iconWidgets.clear();
      String savedInput = this.searchField != null ? this.searchField.getInput() : "";
      boolean savedFocused = this.searchField != null && this.searchField.focused;
      boolean savedSelected = this.searchField != null && this.searchField.selected;
      this.clearWidgets();
      int x = 10;
      int y = 4;
      int width = 100;
      int j = 0;

      for (ModSettingsEntry modEntry : this.modSettingsEntries) {
         ModIconWidget iconWidget = new ModIconWidget(modEntry.modName, modEntry.getIcon(), x, y, width, 28);
         this.iconWidgets.add(iconWidget);
         this.addWidget(iconWidget);
         y += 28;

         for (OptionPage page : modEntry.getPages()) {
            int finalIdx = j;
            VButtonWidget widget = new VButtonWidget(x, y, width, 20, Component.nullToEmpty(page.name), button -> this.setOptionList(finalIdx));
            widget.setTextLayout(false, 12);
            this.buttons.add(widget);
            this.pageButtons.add(widget);
            this.addWidget(widget);
            y += 20;
            j++;
         }
      }

      if (!this.isSearchActive) {
         this.pageButtons.get(this.currentListIdx).setSelected(true);
         VOptionList currentList = this.optionPages.get(this.currentListIdx).getOptionList();
         this.addWidget(currentList);
      } else if (this.searchResultsPage != null) {
         VOptionList searchList = this.searchResultsPage.getOptionList();
         this.addWidget(searchList);
         this.searchResultsPage.updateOptionStates();
      }

      this.addButtonsWithSearchBar();
      this.searchField.setInput(savedInput);
      if (savedFocused) {
         this.searchField.setFocused(true);
         this.searchField.setSelected(savedSelected);
      }
   }

   private void addButtonsWithSearchBar() {
      int rightMargin = 10;
      int padding = 10;
      int buttonWidth = Minecraft.getInstance().font.width(CommonComponents.GUI_DONE) + 2 * padding;
      int x0 = this.width - buttonWidth - rightMargin;
      int y0 = this.height - 20 - 7;
      VButtonWidget doneButton = new VButtonWidget(x0, y0, buttonWidth, 20, CommonComponents.GUI_DONE, button -> Minecraft.getInstance().setScreen(this.parent));
      buttonWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.apply")) + 2 * padding;
      x0 -= buttonWidth + 5;
      this.applyButton = new VButtonWidget(x0, y0, buttonWidth, 20, Component.translatable("vulkanmod.options.buttons.apply"), button -> this.applyOptions());
      buttonWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.undo")) + 2 * padding;
      x0 -= buttonWidth + 5;
      this.undoButton = new VButtonWidget(x0, y0, buttonWidth, 20, Component.translatable("vulkanmod.options.buttons.undo"), button -> this.undo());
      int kofiWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + padding;
      int kofiX = this.width - kofiWidth - rightMargin;
      VButtonWidget supportButton = new VButtonWidget(
         kofiX,
         4,
         kofiWidth,
         20,
         Component.translatable("vulkanmod.options.buttons.kofi"),
         button -> Util.getPlatform().openUri("https://ko-fi.com/xcollateral")
      );
      this.buttons.add(this.applyButton);
      this.buttons.add(doneButton);
      this.buttons.add(supportButton);
      this.buttons.add(this.undoButton);
      this.addWidget(this.applyButton);
      this.addWidget(doneButton);
      this.addWidget(supportButton);
      this.addWidget(this.undoButton);
      this.addWidget(this.searchField);
      if (UpdateChecker.isUpdateAvailable()) {
         assert this.minecraft != null;

         int updateWidth = this.minecraft.font.width(Component.translatable("vulkanmod.options.buttons.update_available")) + padding;
         VButtonWidget updateButton = new VButtonWidget(
            kofiX - updateWidth - 5,
            4,
            updateWidth,
            20,
            Component.translatable("vulkanmod.options.buttons.update_available").withStyle(ChatFormatting.UNDERLINE),
            button -> Util.getPlatform().openUri("https://modrinth.com/mod/vulkanmod")
         );
         this.buttons.add(updateButton);
         this.addWidget(updateButton);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      for (GuiEventListener element : this.children()) {
         if (element.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(element);
            if (button == 0) {
               this.setDragging(true);
            }

            this.updateState();
            return true;
         }
      }

      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.setDragging(false);
      this.updateState();
      return this.getChildAt(mouseX, mouseY).filter(guiEventListener -> guiEventListener.mouseReleased(mouseX, mouseY, button)).isPresent();
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      this.renderBackground(guiGraphics, 0, 0, delta);
      GuiRenderer.guiGraphics = guiGraphics;
      RenderSystem.enableBlend();
      VOptionList currentList;
      if (this.isSearchActive && this.searchResultsPage != null) {
         currentList = this.searchResultsPage.getOptionList();
      } else {
         currentList = this.optionPages.get(this.currentListIdx).getOptionList();
      }

      currentList.updateState(mouseX, mouseY);
      currentList.renderWidget(mouseX, mouseY);

      for (VAbstractWidget widget : this.iconWidgets) {
         widget.render(mouseX, mouseY);
      }

      for (VButtonWidget button : this.buttons) {
         button.updateState(mouseX, mouseY);
         button.render(mouseX, mouseY);
      }

      this.searchField.updateState(mouseX, mouseY);
      this.searchField.render(mouseX, mouseY);
      VAbstractWidget hoveredWidget = null;

      for (VButtonWidget b : this.buttons) {
         if (b.isMouseOver(mouseX, mouseY)) {
            hoveredWidget = b;
            break;
         }
      }

      if (hoveredWidget == null) {
         hoveredWidget = currentList.getHoveredWidget(mouseX, mouseY);
      }

      if (hoveredWidget != null) {
         this.renderTooltip(hoveredWidget, this.tooltipX, this.tooltipY);
      }
   }

   private void renderTooltip(VAbstractWidget widget, int x, int y) {
      List<FormattedCharSequence> list = this.getWidgetTooltip(widget);
      if (!list.isEmpty()) {
         int lines = list.size();
         int padding = 3;
         int width = GuiRenderer.getMaxTextWidth(this.font, list);
         int height = lines * 10;
         float intensity = 0.05F;
         int color = ColorUtil.ARGB.pack(intensity, intensity, intensity, 0.6F);
         GuiRenderer.fill(x - padding, y - padding, x + width + padding, y + height + padding, color);
         color = VGuiConstants.COLOR_RED;
         GuiRenderer.renderBorder(x - padding, y - padding, x + width + padding, y + height + padding, 1, color);
         int yOffset = 0;

         for (FormattedCharSequence text : list) {
            GuiRenderer.drawString(this.font, text, x, y + yOffset, -1);
            yOffset += 10;
         }
      }
   }

   private List<FormattedCharSequence> getWidgetTooltip(VAbstractWidget widget) {
      Component tooltip = widget.getTooltip();
      PerformanceImpact impact = widget.getImpact();
      List<FormattedCharSequence> textList = new ArrayList<>();
      if (tooltip != null) {
         textList.addAll(this.font.split(tooltip, this.tooltipWidth));
      }

      if (impact != null) {
         textList.addAll(this.font.split(Component.translatable("Performance Impact: %s", new Object[]{impact.component()}), this.tooltipWidth));
      }

      return textList;
   }

   private void updateState() {
      if (!(this.applyButton == null | this.undoButton == null)) {
         boolean modified = false;

         for (OptionPage page : this.optionPages) {
            modified |= page.optionChanged();
         }

         if (modified) {
            for (OptionPage page : this.optionPages) {
               page.optionChanged();
            }
         }

         this.applyButton.active = modified;
         this.undoButton.visible = modified;
      }
   }

   private void setOptionList(int i) {
      this.currentListIdx = i;
      this.isSearchActive = false;
      this.searchField.setInput("");
      this.searchField.setFocused(false);
      this.buildPage();
      this.pageButtons.get(i).setSelected(true);
   }

   private void undo() {
      for (OptionPage page : this.optionPages) {
         page.resetToOriginalState();
         page.updateOptionStates();
      }

      this.buildPage();
   }

   private void applyOptions() {
      for (OptionPage page : List.copyOf(this.optionPages)) {
         page.applyOptionChanges();
         page.updateOptionStates();
      }

      for (ModSettingsEntry modEntry : this.modSettingsEntries) {
         modEntry.runOnApply();
      }
   }
}
