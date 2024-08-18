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
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.addon.YtDlpAddon;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.filenaming.TvShowTrailerNaming;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.ScraperInTable;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.panels.MediaScraperConfigurationPanel;
import org.tinymediamanager.ui.panels.ScrollablePanel;

import net.miginfocom.swing.MigLayout;

public class TvShowTrailerSettingsPanel extends JPanel {

  private final TvShowSettings       settings                   = TvShowModuleManager.getInstance().getSettings();
  private final List<ScraperInTable> scrapers                   = ObservableCollections.observableList(new ArrayList<>());
  private final ItemListener         checkBoxListener;
  private final ButtonGroup          trailerFilenameButtonGroup = new ButtonGroup();

  private TmmTable                   tableTrailerScraper;
  private JTextPane                  tpScraperDescription;
  private JComboBox<TrailerQuality>  cbTrailerQuality;
  private JCheckBox                  checkBox;
  private JCheckBox                  chckbxAutomaticTrailerDownload;
  private JPanel                     panelScraperOptions;
  private JCheckBox                  cbTrailerFilename1;
  private JCheckBox                  cbTrailerFilename2;
  private JCheckBox                  cbTrailerFilename3;
  private JCheckBox                  cbTrailerFilename4;
  private JHintCheckBox              chckbxUseYtDlp;

  TvShowTrailerSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    // data init
    List<String> enabledTrailerProviders = settings.getTrailerScrapers();
    int selectedIndex = -1;
    int counter = 0;
    for (MediaScraper scraper : TvShowModuleManager.getInstance().getTvShowList().getAvailableTrailerScrapers()) {
      ScraperInTable trailerScraper = new ScraperInTable(scraper);
      if (enabledTrailerProviders.contains(trailerScraper.getScraperId())) {
        trailerScraper.setActive(true);
        if (selectedIndex < 0) {
          selectedIndex = counter;
        }
      }
      scrapers.add(trailerScraper);
      counter++;
    }

    // UI init
    initComponents();
    initDataBindings();

    // adjust table columns
    // Checkbox and Logo shall have minimal width
    TableColumnResizer.setMaxWidthForColumn(tableTrailerScraper, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableTrailerScraper, 1, 10);
    TableColumnResizer.adjustColumnPreferredWidths(tableTrailerScraper, 5);

    tableTrailerScraper.getModel().addTableModelListener(arg0 -> {
      // click on the checkbox
      if (arg0.getColumn() == 0) {
        int row = arg0.getFirstRow();
        ScraperInTable changedScraper = scrapers.get(row);
        if (changedScraper.getActive()) {
          settings.addTvShowTrailerScraper(changedScraper.getScraperId());
        }
        else {
          settings.removeTvShowTrailerScraper(changedScraper.getScraperId());
        }
      }
    });

    // implement selection listener to load settings
    tableTrailerScraper.getSelectionModel().addListSelectionListener(e -> {
      int index = tableTrailerScraper.convertRowIndexToModel(tableTrailerScraper.getSelectedRow());
      if (index > -1) {
        panelScraperOptions.removeAll();
        if (scrapers.get(index).getMediaProvider().getProviderInfo().getConfig().hasConfig()) {
          panelScraperOptions.add(new MediaScraperConfigurationPanel(scrapers.get(index).getMediaProvider()));
        }
        panelScraperOptions.revalidate();
      }
    });

    {
      // add a CSS rule to force body tags to use the default label font
      // instead of the value in javax.swing.text.html.default.csss
      Font font = UIManager.getFont("Label.font");
      Color color = UIManager.getColor("Label.foreground");
      String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; color: rgb(" + color.getRed() + ","
          + color.getGreen() + "," + color.getBlue() + "); }";
      tpScraperDescription.setEditorKit(new HTMLEditorKit());
      ((HTMLDocument) tpScraperDescription.getDocument()).getStyleSheet().addRule(bodyRule);
    }

    // select default scraper
    if (selectedIndex < 0) {
      selectedIndex = 0;
    }
    if (counter > 0) {
      tableTrailerScraper.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }

    buildCheckBoxes();

    chckbxUseYtDlp.setEnabled(new YtDlpAddon().isAvailable());
  }

  private void buildCheckBoxes() {
    cbTrailerFilename1.removeItemListener(checkBoxListener);
    cbTrailerFilename2.removeItemListener(checkBoxListener);
    cbTrailerFilename3.removeItemListener(checkBoxListener);
    cbTrailerFilename4.removeItemListener(checkBoxListener);
    clearSelection(cbTrailerFilename1, cbTrailerFilename2, cbTrailerFilename3, cbTrailerFilename4);

    // trailer filenames
    List<TvShowTrailerNaming> trailerFilenames = settings.getTrailerFilenames();
    if (trailerFilenames.contains(TvShowTrailerNaming.TVSHOW_TRAILER)) {
      cbTrailerFilename1.setSelected(true);
    }
    else if (trailerFilenames.contains(TvShowTrailerNaming.TVSHOWNAME_TRAILER)) {
      cbTrailerFilename2.setSelected(true);
    }
    else if (trailerFilenames.contains(TvShowTrailerNaming.TRAILERS_TVSHOWNAME_TRAILER)) {
      cbTrailerFilename3.setSelected(true);
    }
    else if (trailerFilenames.contains(TvShowTrailerNaming.TRAILER)) {
      cbTrailerFilename4.setSelected(true);
    }

    cbTrailerFilename1.addItemListener(checkBoxListener);
    cbTrailerFilename2.addItemListener(checkBoxListener);
    cbTrailerFilename3.addItemListener(checkBoxListener);
    cbTrailerFilename4.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  private void checkChanges() {
    // set trailer filenames
    settings.clearTrailerFilenames();
    if (cbTrailerFilename1.isSelected()) {
      settings.addTrailerFilename(TvShowTrailerNaming.TVSHOW_TRAILER);
    }
    if (cbTrailerFilename2.isSelected()) {
      settings.addTrailerFilename(TvShowTrailerNaming.TVSHOWNAME_TRAILER);
    }
    if (cbTrailerFilename3.isSelected()) {
      settings.addTrailerFilename(TvShowTrailerNaming.TRAILERS_TVSHOWNAME_TRAILER);
    }
    if (cbTrailerFilename4.isSelected()) {
      settings.addTrailerFilename(TvShowTrailerNaming.TRAILER);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 0", "[60lp,grow]", "[][15lp!][]"));
    {
      JPanel panelScraper = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][grow]", "[][shrink 0][]"));

      JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper.trailer"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelScraper, lblScraper, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#trailer"));
      add(collapsiblePanel, "cell 0 0,wmin 0,grow");
      {
        tableTrailerScraper = new TmmTable() {
          @Override
          public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            java.awt.Component comp = super.prepareRenderer(renderer, row, column);
            ScraperInTable scraper = scrapers.get(row);
            comp.setEnabled(scraper.isEnabled());
            return comp;
          }
        };
        tableTrailerScraper.setRowHeight(29);
        tableTrailerScraper.setShowGrid(true);
        panelScraper.add(tableTrailerScraper, "cell 1 0,grow");

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
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][]")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#advanced-options-2"));
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");

      {
        chckbxUseYtDlp = new JHintCheckBox(TmmResourceBundle.getString("Settings.trailer.ytdlp"));
        chckbxUseYtDlp.setToolTipText(TmmResourceBundle.getString("Settings.trailer.ytdlp.desc"));
        chckbxUseYtDlp.setHintIcon(IconManager.HINT);
        panelOptions.add(chckbxUseYtDlp, "cell 1 0 2 1");

        checkBox = new JCheckBox(TmmResourceBundle.getString("Settings.trailer.preferred"));
        panelOptions.add(checkBox, "cell 1 1 2 1");

        JLabel lblTrailerSource = new JLabel(TmmResourceBundle.getString("Settings.trailer.source"));
        panelOptions.add(lblTrailerSource, "cell 2 2");

        JLabel lblTrailerQuality = new JLabel(TmmResourceBundle.getString("Settings.trailer.quality"));
        panelOptions.add(lblTrailerQuality, "cell 2 3");

        cbTrailerQuality = new JComboBox();
        cbTrailerQuality.setModel(new DefaultComboBoxModel<>(TrailerQuality.values()));
        panelOptions.add(cbTrailerQuality, "cell 2 3");

        chckbxAutomaticTrailerDownload = new JCheckBox(TmmResourceBundle.getString("Settings.trailer.automaticdownload"));
        panelOptions.add(chckbxAutomaticTrailerDownload, "cell 1 4 2 1");

        JLabel lblAutomaticTrailerDownloadHint = new JLabel(TmmResourceBundle.getString("Settings.trailer.automaticdownload.hint"));
        panelOptions.add(lblAutomaticTrailerDownloadHint, "cell 2 5");
        TmmFontHelper.changeFont(lblAutomaticTrailerDownloadHint, L2);

        JPanel panelTrailerFilenames = new JPanel();
        panelOptions.add(panelTrailerFilenames, "cell 1 6 2 1");
        panelTrailerFilenames.setLayout(new MigLayout("insets 0", "[][]", "[][][]"));

        JLabel lblTrailerFileNaming = new JLabel(TmmResourceBundle.getString("Settings.trailerFileNaming"));
        panelTrailerFilenames.add(lblTrailerFileNaming, "cell 0 0");

        cbTrailerFilename1 = new JCheckBox("tvshow-trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename1);
        panelTrailerFilenames.add(cbTrailerFilename1, "cell 1 0");

        cbTrailerFilename2 = new JCheckBox(
            TmmResourceBundle.getString("Settings.trailer.tvshowtitle") + "-trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename2);
        panelTrailerFilenames.add(cbTrailerFilename2, "cell 1 1");

        cbTrailerFilename3 = new JCheckBox("trailers/" + TmmResourceBundle.getString("Settings.trailer.tvshowtitle") + "-trailer."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename3);
        panelTrailerFilenames.add(cbTrailerFilename3, "cell 1 2");

        cbTrailerFilename4 = new JCheckBox("trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename4);
        panelTrailerFilenames.add(cbTrailerFilename4, "cell 1 3");
      }
    }
  }

  protected void initDataBindings() {
    JTableBinding jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, scrapers, tableTrailerScraper);
    //
    Property trailerScraperBeanProperty = BeanProperty.create("active");
    jTableBinding.addColumnBinding(trailerScraperBeanProperty).setColumnName("Aktiv").setColumnClass(Boolean.class);
    //
    Property trailerScraperBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding.addColumnBinding(trailerScraperBeanProperty_1).setColumnName("Logo").setEditable(false).setColumnClass(ImageIcon.class);
    //
    Property trailerScraperBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding.addColumnBinding(trailerScraperBeanProperty_2).setColumnName("Name").setEditable(false).setColumnClass(String.class);
    //
    jTableBinding.bind();
    //
    Property jTableBeanProperty = BeanProperty.create("selectedElement.scraperDescription");
    Property jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, tableTrailerScraper, jTableBeanProperty, tpScraperDescription,
        jTextPaneBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("trailerQuality");
    Property jComboBoxBeanProperty_1 = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1, cbTrailerQuality,
        jComboBoxBeanProperty_1);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("useTrailerPreference");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2, checkBox,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property tvShowSettingsBeanProperty_3 = BeanProperty.create("automaticTrailerDownload");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_3,
        chckbxAutomaticTrailerDownload, jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property tvShowSettingsBeanProperty = BeanProperty.create("useYtDlp");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, chckbxUseYtDlp,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
  }
}
