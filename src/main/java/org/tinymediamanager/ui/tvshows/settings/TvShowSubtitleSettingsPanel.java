/*
 * Copyright 2012 - 2024 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.tvshows.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.ui.ScraperInTable;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.panels.MediaScraperConfigurationPanel;
import org.tinymediamanager.ui.panels.ScrollablePanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowSubtitleSettingsPanel} is used to maintain subtitle related settings
 * 
 * @author Manuel Laggner
 */
class TvShowSubtitleSettingsPanel extends JPanel {
  private final TvShowSettings       settings = TvShowModuleManager.getInstance().getSettings();
  private final List<ScraperInTable> scrapers = new ArrayList<>();

  private TmmTable                   tableScraper;
  private JTextPane                  tpScraperDescription;
  private JPanel                     panelScraperOptions;

  TvShowSubtitleSettingsPanel() {
    // data init
    List<String> enabledSubtitleProviders = settings.getSubtitleScrapers();
    int selectedIndex = -1;
    int counter = 0;
    for (MediaScraper scraper : TvShowModuleManager.getInstance().getTvShowList().getAvailableSubtitleScrapers()) {
      ScraperInTable subtitleScraper = new ScraperInTable(scraper);
      if (enabledSubtitleProviders.contains(subtitleScraper.getScraperId())) {
        subtitleScraper.setActive(true);
        if (selectedIndex < 0) {
          selectedIndex = counter;
        }
      }
      scrapers.add(subtitleScraper);
      counter++;
    }

    // UI init
    initComponents();
    initDataBindings();

    // adjust table columns
    // Checkbox and Logo shall have minimal width
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 1, 10);
    TableColumnResizer.adjustColumnPreferredWidths(tableScraper, 5);

    tableScraper.getModel().addTableModelListener(arg0 -> {
      // click on the checkbox
      if (arg0.getColumn() == 0) {
        int row = arg0.getFirstRow();
        ScraperInTable changedScraper = scrapers.get(row);
        if (changedScraper.getActive()) {
          settings.addTvShowSubtitleScraper(changedScraper.getScraperId());
        }
        else {
          settings.removeTvShowSubtitleScraper(changedScraper.getScraperId());
        }
      }
    });

    // implement selection listener to load settings
    tableScraper.getSelectionModel().addListSelectionListener(e -> {
      int index = tableScraper.convertRowIndexToModel(tableScraper.getSelectedRow());
      if (index > -1) {
        panelScraperOptions.removeAll();
        if (scrapers.get(index).getMediaProvider().getProviderInfo().getConfig().hasConfig()) {
          panelScraperOptions.add(new MediaScraperConfigurationPanel(scrapers.get(index).getMediaProvider()));
        }
        panelScraperOptions.revalidate();
      }
    });

    // add a CSS rule to force body tags to use the default label font
    // instead of the value in javax.swing.text.html.default.csss
    Font font = UIManager.getFont("Label.font");
    Color color = UIManager.getColor("Label.foreground");
    String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; color: rgb(" + color.getRed() + ","
        + color.getGreen() + "," + color.getBlue() + "); }";
    tpScraperDescription.setEditorKit(new HTMLEditorKit());
    ((HTMLDocument) tpScraperDescription.getDocument()).getStyleSheet().addRule(bodyRule);

    // select default tv show subtitle scraper
    if (selectedIndex < 0) {
      selectedIndex = 0;
    }
    if (counter > 0) {
      tableScraper.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 0", "[600lp,grow]", "[]"));
    {
      JPanel panelScraper = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][grow]", "[][shrink 0][]"));

      JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper.subtitle"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelScraper, lblScraper, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#subtitles"));
      add(collapsiblePanel, "cell 0 0,wmin 0,grow");
      {
        tableScraper = new TmmTable() {
          @Override
          public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            java.awt.Component comp = super.prepareRenderer(renderer, row, column);
            ScraperInTable scraper = scrapers.get(row);
            comp.setEnabled(scraper.isEnabled());
            return comp;
          }
        };
        tableScraper.setRowHeight(29);
        tableScraper.setShowGrid(true);
        panelScraper.add(tableScraper, "cell 1 0,grow");

        JSeparator separator = new JSeparator();
        panelScraper.add(separator, "cell 1 1,growx");

        JPanel panelScraperDetails = new ScrollablePanel();
        panelScraper.add(panelScraperDetails, "cell 1 2,grow");
        panelScraperDetails.setLayout(new MigLayout("insets 0", "[grow]", "[][grow]"));

        tpScraperDescription = new ReadOnlyTextPane();
        tpScraperDescription.setEditorKit(new HTMLEditorKit());
        panelScraperDetails.add(tpScraperDescription, "cell 0 0,grow");

        panelScraperOptions = new ScrollablePanel();
        panelScraperOptions.setLayout(new FlowLayout(FlowLayout.LEFT));
        panelScraperDetails.add(panelScraperOptions, "cell 0 1,grow");
      }
    }
  }

  protected void initDataBindings() {
    JTableBinding jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, scrapers, tableScraper);
    //
    Property subtitleScraperBeanProperty = BeanProperty.create("active");
    jTableBinding.addColumnBinding(subtitleScraperBeanProperty).setColumnName("Aktiv").setColumnClass(Boolean.class);
    //
    Property subtitleScraperBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding.addColumnBinding(subtitleScraperBeanProperty_1).setColumnName("Logo").setEditable(false).setColumnClass(ImageIcon.class);
    //
    Property subtitleScraperBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding.addColumnBinding(subtitleScraperBeanProperty_2).setColumnName("Name").setEditable(false).setColumnClass(String.class);
    //
    jTableBinding.bind();
    //
    Property jTableBeanProperty = BeanProperty.create("selectedElement.scraperDescription");
    Property jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, tableScraper, jTableBeanProperty, tpScraperDescription,
        jTextPaneBeanProperty);
    autoBinding.bind();
  }
}
