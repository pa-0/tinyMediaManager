/*
 * Copyright 2012 - 2025 Manuel Laggner
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
package org.tinymediamanager.ui.dialogs;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.ImageSizeAndUrl;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.interfaces.IMediaArtworkProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.ui.ArtworkDragAndDropListener;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.WrapLayout;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperCheckComboBox;
import org.tinymediamanager.ui.components.combobox.TmmCheckComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * The Class ImageChooser. Let the user choose the right image for the media entity
 *
 * @author Manuel Laggner
 */
public class ImageChooserDialog extends TmmDialog {
  private static final Logger               LOGGER         = LoggerFactory.getLogger(ImageChooserDialog.class);
  private static final String               DIALOG_ID      = "imageChooser";

  private final Map<String, Object>         ids;
  private final MediaArtworkType            type;
  private final MediaType                   mediaType;
  private final ImageLabel                  imageLabel;
  private final List<MediaScraper>          artworkScrapers;

  private final List<JToggleButton>         buttons        = new ArrayList<>();
  private final List<JPanel>                imagePanels    = new ArrayList<>();
  private final ActionListener              filterListener;

  private ButtonGroup                       buttonGroup;
  private JProgressBar                      progressBar;
  private JLabel                            lblProgressAction;
  private JScrollPane                       scrollPane;
  private JPanel                            panelImages;
  private LockableViewPort                  viewport;
  private JTextField                        tfImageUrl;

  private String                            openFolderPath = null;
  private List<MediaArtwork>                artwork;
  private List<String>                      extraThumbs    = null;
  private List<String>                      extraFanarts   = null;
  private DownloadTask                      task;

  private MediaScraperCheckComboBox         cbScraper;
  private TmmCheckComboBox<ImageSizeAndUrl> cbSize;
  private TmmCheckComboBox<MediaLanguages>  cbLanguage;

  private JLabel                            lblThumbs;
  private JButton                           btnMarkExtrathumbs;
  private JButton                           btnUnMarkExtrathumbs;
  private JLabel                            lblExtrathumbsSelected;

  private JLabel                            lblFanart;
  private JButton                           btnMarkExtrafanart;
  private JButton                           btnUnMarkExtrafanart;
  private JLabel                            lblExtrafanartSelected;

  /**
   * Instantiates a new image chooser dialog.
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param imageLabel
   *          the image label
   * @param mediaType
   *          the media for which artwork has to be chosen
   */
  public ImageChooserDialog(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      ImageLabel imageLabel, MediaType mediaType) {

    super(parent, "", DIALOG_ID);
    this.imageLabel = imageLabel;
    this.type = type;
    this.mediaType = mediaType;
    this.ids = ids;
    this.artworkScrapers = artworkScrapers;
    this.artwork = null;

    filterListener = e -> SwingUtilities.invokeLater(this::filterChanged);

    init();

    cbScraper.addActionListener(filterListener);
    cbSize.addActionListener(filterListener);
    cbLanguage.addActionListener(filterListener);
  }

  private void init() {
    switch (type) {
      case BACKGROUND:
        setTitle(TmmResourceBundle.getString("image.choose.fanart"));
        break;

      case POSTER:
        setTitle(TmmResourceBundle.getString("image.choose.poster"));
        break;

      case BANNER:
        setTitle(TmmResourceBundle.getString("image.choose.banner"));
        break;

      case SEASON_POSTER:
        Object season = ids.get("tvShowSeason");
        if (season instanceof TvShowSeason tvShowSeason) {
          setTitle(TmmResourceBundle.getString("image.choose.season") + " - " + TmmResourceBundle.getString("metatag.season") + " "
              + tvShowSeason.getSeason());
        }
        else if (season instanceof Integer) {
          setTitle(TmmResourceBundle.getString("image.choose.season") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season"));
        }
        break;

      case SEASON_FANART:
        season = ids.get("tvShowSeason");
        if (season instanceof TvShowSeason tvShowSeason) {
          setTitle(TmmResourceBundle.getString("image.choose.season.fanart") + " - " + TmmResourceBundle.getString("metatag.season") + " "
              + tvShowSeason.getSeason());
        }
        else if (season instanceof Integer) {
          setTitle(TmmResourceBundle.getString("image.choose.season.fanart") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season.fanart"));
        }
        break;

      case SEASON_BANNER:
        season = ids.get("tvShowSeason");
        if (season instanceof TvShowSeason tvShowSeason) {
          setTitle(TmmResourceBundle.getString("image.choose.season.banner") + " - " + TmmResourceBundle.getString("metatag.season") + " "
              + tvShowSeason.getSeason());
        }
        else if (season instanceof Integer) {
          setTitle(TmmResourceBundle.getString("image.choose.season.banner") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season.banner"));
        }
        break;

      case SEASON_THUMB:
        season = ids.get("tvShowSeason");
        if (season instanceof TvShowSeason tvShowSeason) {
          setTitle(TmmResourceBundle.getString("image.choose.season.thumb") + " - " + TmmResourceBundle.getString("metatag.season") + " "
              + tvShowSeason.getSeason());
        }
        else if (season instanceof Integer) {
          setTitle(TmmResourceBundle.getString("image.choose.season.thumb") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season.thumb"));
        }
        break;

      case CLEARART:
        setTitle(TmmResourceBundle.getString("image.choose.clearart"));
        break;

      case DISC:
        setTitle(TmmResourceBundle.getString("image.choose.disc"));
        break;

      case CLEARLOGO:
        setTitle(TmmResourceBundle.getString("image.choose.clearlogo"));
        break;

      case CHARACTERART:
        setTitle(TmmResourceBundle.getString("image.choose.characterart"));
        break;

      case THUMB:
        setTitle(TmmResourceBundle.getString("image.choose.thumb"));
        break;

      case KEYART:
        setTitle(TmmResourceBundle.getString("image.choose.keyart"));
        break;
    }

    /* UI components */
    JPanel contentPanel = new JPanel();
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("hidemode 3, insets n n 0 n", "[850lp,grow][]", "[][10lp!][500lp,grow][shrink 0][][]"));
    {
      JPanel panelFilter = new JPanel();
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFilter, new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.filterby")),
          true);

      contentPanel.add(collapsiblePanel, "cell 0 0 2 1,grow, wmin 0");
      panelFilter.setLayout(new MigLayout("insets 0", "[25%:n][20lp!][25%:n][20lp!][25%:n]", "[][]"));

      JLabel lblScraperT = new TmmLabel(TmmResourceBundle.getString("scraper.artwork"));
      panelFilter.add(lblScraperT, "cell 0 0");

      JLabel lblDimensionT = new TmmLabel(TmmResourceBundle.getString("metatag.size"));
      panelFilter.add(lblDimensionT, "cell 2 0");

      JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
      panelFilter.add(lblLanguageT, "cell 4 0");

      cbScraper = new MediaScraperCheckComboBox(artworkScrapers);
      panelFilter.add(cbScraper, "cell 0 1,growx,wmin 0");

      cbSize = new TmmCheckComboBox();
      cbSize.setSingleLineEditor();
      panelFilter.add(cbSize, "cell 2 1,growx,wmin 0");

      cbLanguage = new TmmCheckComboBox();
      cbLanguage.setSingleLineEditor(); // looks weird when preselecting langu?
      // our preferred should be activated?
      // List<MediaLanguages> preferred = TvShowModuleManager.getInstance().getSettings().getImageScraperLanguages();
      // cbLanguage.setItems(preferred);
      // cbLanguage.setSelectedItems(preferred);
      panelFilter.add(cbLanguage, "cell 4 1,growx,wmin 0");
    }
    {
      scrollPane = new NoBorderScrollPane();
      viewport = new LockableViewPort();

      scrollPane.setViewport(viewport);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      contentPanel.add(scrollPane, "cell 0 2 2 1,grow");
      {
        panelImages = new JPanel();
        viewport.setView(panelImages);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panelImages.setLayout(new WrapLayout(FlowLayout.LEFT));
      }
    }
    {
      JSeparator separator = new JSeparator();
      contentPanel.add(separator, "cell 0 3 2 1,growx");
    }
    {
      tfImageUrl = new EnhancedTextField(TmmResourceBundle.getString("image.inserturl"));
      contentPanel.add(tfImageUrl, "cell 0 4,growx");
      tfImageUrl.setColumns(10);

      JButton btnAddImage = new JButton(TmmResourceBundle.getString("image.downloadimage"));
      btnAddImage.addActionListener(e -> {
        if (StringUtils.isNotBlank(tfImageUrl.getText())) {
          downloadAndPreviewImage(tfImageUrl.getText());
        }
      });
      contentPanel.add(btnAddImage, "cell 1 4");
    }

    {
      // add buttons to select/deselect all extrafanarts/extrathumbs
      if (type == BACKGROUND || type == THUMB) {
        lblThumbs = new JLabel(TmmResourceBundle.getString("mediafiletype.extrathumb") + ":");
        contentPanel.add(lblThumbs, "flowx,cell 0 5");
        lblThumbs.setVisible(false);

        btnMarkExtrathumbs = new SquareIconButton(IconManager.CHECK_ALL);
        contentPanel.add(btnMarkExtrathumbs, "cell 0 5");
        btnMarkExtrathumbs.setVisible(false);
        btnMarkExtrathumbs.setToolTipText(TmmResourceBundle.getString("image.extrathumbs.markall"));
        btnMarkExtrathumbs.addActionListener(arg0 -> {
          for (JToggleButton button : buttons) {
            if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox chkbx) {
              chkbx.setSelected(true);
              updateExtrathumbSelectedCount();
            }
          }
        });

        btnUnMarkExtrathumbs = new SquareIconButton(IconManager.CLEAR_ALL);
        contentPanel.add(btnUnMarkExtrathumbs, "cell 0 5");
        btnUnMarkExtrathumbs.setVisible(false);
        btnUnMarkExtrathumbs.setToolTipText(TmmResourceBundle.getString("image.extrathumbs.unmarkall"));
        btnUnMarkExtrathumbs.addActionListener(arg0 -> {
          for (JToggleButton button : buttons) {
            if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox chkbx) {
              chkbx.setSelected(false);
              updateExtrathumbSelectedCount();
            }
          }
        });

        lblExtrathumbsSelected = new JLabel("");
        contentPanel.add(lblExtrathumbsSelected, "cell 0 5, gapx n 100lp");
      }
    }
    {
      if (type == BACKGROUND) {
        lblFanart = new JLabel(TmmResourceBundle.getString("mediafiletype.extrafanart") + ":");
        contentPanel.add(lblFanart, "flowx,cell 0 5");
        lblFanart.setVisible(false);

        btnMarkExtrafanart = new SquareIconButton(IconManager.CHECK_ALL);
        contentPanel.add(btnMarkExtrafanart, "cell 0 5");
        btnMarkExtrafanart.setVisible(false);
        btnMarkExtrafanart.setToolTipText(TmmResourceBundle.getString("image.extrafanart.markall"));
        btnMarkExtrafanart.addActionListener(arg0 -> {
          for (JToggleButton button : buttons) {
            if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox chkbx) {
              chkbx.setSelected(true);
              updateExtraFanartSelectedCount();
            }
          }
        });

        btnUnMarkExtrafanart = new SquareIconButton(IconManager.CLEAR_ALL);
        contentPanel.add(btnUnMarkExtrafanart, "cell 0 5");
        btnUnMarkExtrafanart.setVisible(false);
        btnUnMarkExtrafanart.setToolTipText(TmmResourceBundle.getString("image.extrafanart.unmarkall"));
        btnUnMarkExtrafanart.addActionListener(arg0 -> {
          for (JToggleButton button : buttons) {
            if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox chkbx) {
              chkbx.setSelected(false);
              updateExtraFanartSelectedCount();
            }
          }
        });

        lblExtrafanartSelected = new JLabel("");
        contentPanel.add(lblExtrafanartSelected, "cell 0 5");
      }
    }

    {
      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new MigLayout("", "[][grow]", "[]"));

      progressBar = new JProgressBar();
      infoPanel.add(progressBar, "cell 0 0");

      lblProgressAction = new JLabel("");
      infoPanel.add(lblProgressAction, "cell 1 0");

      setBottomInformationPanel(infoPanel);
    }

    {
      JButton cancelButton = new JButton(TmmResourceBundle.getString("Button.cancel"));
      Action actionCancel = new CancelAction();
      cancelButton.setAction(actionCancel);
      cancelButton.setActionCommand("Cancel");
      addButton(cancelButton);

      JButton btnAddFile = new JButton(TmmResourceBundle.getString("Button.addfile"));
      Action actionLocalFile = new LocalFileChooseAction();
      btnAddFile.setAction(actionLocalFile);
      addButton(btnAddFile);

      JButton okButton = new JButton(TmmResourceBundle.getString("Button.ok"));
      Action actionOK = new OkAction();
      okButton.setAction(actionOK);
      okButton.setActionCommand("OK");
      addDefaultButton(okButton);
    }

    {
      new DropTarget(this, new ArtworkDragAndDropListener(imageLabel) {
        @Override
        public void drop(DropTargetDropEvent dtde) {
          super.drop(dtde);

          // cancel the task and close the dialog
          if (task != null) {
            task.cancel(true);
          }

          setVisible(false);
        }
      });
    }
  }

  private void startScraping() {
    // scrape from online sources or use pre-selected artwork
    task = new DownloadTask(ids, artworkScrapers, artwork);
    task.execute();
  }

  private void updateExtrathumbSelectedCount() {
    int count = 0;
    for (JToggleButton btn : buttons) {
      if (btn.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox checkBox) {
        if (checkBox.isSelected()) {
          count++;
        }
      }
    }
    if (count > 0) {
      lblExtrathumbsSelected.setText(count + " " + TmmResourceBundle.getString("tmm.selected"));
    }
    else {
      lblExtrathumbsSelected.setText("");
    }
  }

  private void setArtwork(List<MediaArtwork> artwork) {
    this.artwork = artwork;
  }

  private void updateExtraFanartSelectedCount() {
    int count = 0;
    for (JToggleButton btn : buttons) {
      if (btn.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox checkBox) {
        if (checkBox.isSelected()) {
          count++;
        }
      }
    }
    if (count > 0) {
      lblExtrafanartSelected.setText(count + " " + TmmResourceBundle.getString("tmm.selected"));
    }
    else {
      lblExtrafanartSelected.setText("");
    }
  }

  public void bindExtraThumbs(List<String> extraThumbs) {
    if (type != BACKGROUND && type != THUMB) {
      return;
    }

    this.extraThumbs = extraThumbs;

    if (extraThumbs != null) {
      lblThumbs.setVisible(true);
      btnMarkExtrathumbs.setVisible(true);
      btnUnMarkExtrathumbs.setVisible(true);
    }
    else {
      lblThumbs.setVisible(false);
      btnMarkExtrathumbs.setVisible(false);
      btnUnMarkExtrathumbs.setVisible(false);
    }
  }

  public void bindExtraFanarts(List<String> extraFanarts) {
    if (type != BACKGROUND) {
      return;
    }

    this.extraFanarts = extraFanarts;

    if (extraFanarts != null) {
      lblFanart.setVisible(true);
      btnMarkExtrafanart.setVisible(true);
      btnUnMarkExtrafanart.setVisible(true);
    }
    else {
      lblFanart.setVisible(false);
      btnMarkExtrafanart.setVisible(false);
      btnUnMarkExtrafanart.setVisible(false);
    }
  }

  public void setOpenFolderPath(String openFolderPath) {
    this.openFolderPath = openFolderPath;
  }

  private void startProgressBar(String description) {
    lblProgressAction.setText(description);
    progressBar.setVisible(true);
    progressBar.setIndeterminate(true);
  }

  private void stopProgressBar() {
    lblProgressAction.setText("");
    progressBar.setVisible(false);
    progressBar.setIndeterminate(false);
  }

  private void addImage(byte[] imageData, final MediaArtwork artwork) throws Exception {
    BufferedImage originalImage = ImageUtils.createImage(imageData);
    artwork.addImageSize(originalImage.getWidth(), originalImage.getHeight(), artwork.getPreviewUrl(), 0);

    Point size = null;

    GridBagLayout gbl = new GridBagLayout();

    switch (type) {
      case BACKGROUND:
      case CLEARART:
      case THUMB:
      case DISC:
      case CHARACTERART:
        size = ImageUtils.calculateSize(300, 150, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

      case BANNER:
      case LOGO:
      case CLEARLOGO:
        size = ImageUtils.calculateSize(300, 100, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

      case POSTER:
      case KEYART:
      default:
        size = ImageUtils.calculateSize(150, 250, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

    }

    gbl.columnWeights = new double[] { Double.MIN_VALUE };
    gbl.rowWeights = new double[] { Double.MIN_VALUE };
    JPanel imagePanel = new JPanel();
    imagePanel.setLayout(gbl);

    int row = 0;

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    JToggleButton button = new JToggleButton() {
      @Override
      public void setSelected(boolean b) {
        super.setSelected(b);
      }
    };
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1) {
          button.setSelected(true);
          new OkAction().actionPerformed(new ActionEvent(e.getSource(), e.getID(), "OK"));
        }
      }
    });
    button.setBackground(Color.white);
    button.setMargin(new Insets(10, 10, 10, 10));
    if (artwork.isAnimated()) {
      button.setText("<html><img width=\"" + size.x + "\" height=\"" + size.y + "\" src='" + artwork.getPreviewUrl() + "'/></html>");
      button.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
    }
    else {
      ImageIcon imageIcon = new ImageIcon(
          Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.AUTOMATIC, size.x, size.y, Scalr.OP_ANTIALIAS));
      button.setIcon(imageIcon);
    }

    button.putClientProperty("MediaArtwork", artwork);

    buttons.add(button);
    imagePanel.add(button, gbc);

    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = ++row;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.LAST_LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 5, 0, 5);

    JComboBox cb = null;
    if (!artwork.getImageSizes().isEmpty()) {
      cb = new JComboBox(artwork.getImageSizes().toArray());
    }
    else {
      cb = new JComboBox(new String[] { originalImage.getWidth() + "x" + originalImage.getHeight() });
    }
    button.putClientProperty("MediaArtworkSize", cb);
    imagePanel.add(cb, gbc);

    // release memory
    originalImage.flush();

    /* show image button */
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = ++row;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    if (extraThumbs != null || extraFanarts != null) {
      gbc.gridwidth = 1;
    }
    else {
      gbc.gridwidth = 3;
    }
    gbc.insets = new Insets(0, 5, 0, 5);

    LinkLabel lblShowImage = new LinkLabel(TmmResourceBundle.getString("image.showoriginal"));
    lblShowImage.addActionListener(e -> {
      ImagePreviewDialog dialog = new ImagePreviewDialog(artwork.getOriginalUrl());
      dialog.setVisible(true);
    });
    imagePanel.add(lblShowImage, gbc);

    // should we provide an option for extrathumbs
    if (extraThumbs != null) {
      gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.LINE_END;
      gbc.insets = new Insets(0, 5, 0, 5);
      JLabel label = new JLabel("Extrathumb");
      imagePanel.add(label, gbc);

      gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = row++;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.insets = new Insets(0, 5, 0, 5);
      JCheckBox chkbx = new JCheckBox();
      button.putClientProperty("MediaArtworkExtrathumb", chkbx);
      chkbx.addActionListener(l -> updateExtrathumbSelectedCount());
      imagePanel.add(chkbx, gbc);
    }

    // should we provide an option for extrafanart
    if (extraFanarts != null) {
      gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.LINE_END;
      gbc.insets = new Insets(0, 5, 0, 5);
      JLabel label = new JLabel("Extrafanart");
      imagePanel.add(label, gbc);

      gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.insets = new Insets(0, 5, 0, 5);
      JCheckBox chkbx = new JCheckBox();
      button.putClientProperty("MediaArtworkExtrafanart", chkbx);
      chkbx.addActionListener(l -> updateExtraFanartSelectedCount());
      imagePanel.add(chkbx, gbc);
    }

    imagePanel.putClientProperty("MediaArtwork", artwork);

    imagePanels.add(imagePanel);
    imagePanels.sort((o1, o2) -> {
      Object obj1 = o1.getClientProperty("MediaArtwork");
      Object obj2 = o2.getClientProperty("MediaArtwork");

      if (!(obj1 instanceof MediaArtwork artwork1) || !(obj2 instanceof MediaArtwork artwork2)) {
        return 0;
      }

      int score1 = 0;
      int score2 = 0;
      if (mediaType == MediaType.MOVIE || mediaType == MediaType.MOVIE_SET) {
        score1 = MovieArtworkHelper.getMatchingScoreAccordingPreferences(artwork1);
        score2 = MovieArtworkHelper.getMatchingScoreAccordingPreferences(artwork2);
      }
      else if (mediaType == MediaType.TV_SHOW || mediaType == MediaType.TV_EPISODE) {
        score1 = TvShowArtworkHelper.getMatchingScoreAccordingPreferences(artwork1);
        score2 = TvShowArtworkHelper.getMatchingScoreAccordingPreferences(artwork2);
      }
      int result = Integer.compare(score2, score1);
      if (result == 0) {
        // same score - sort by likes descending
        result = Integer.compare(artwork2.getLikes(), artwork1.getLikes());
      }
      if (result == 0) {
        // last resort
        if (artwork1.getBiggestArtwork() == null || artwork2.getBiggestArtwork() == null) {
          result = 0; // cannot compare!
        }
        else {
          result = artwork2.getBiggestArtwork().compareTo(artwork1.getBiggestArtwork());
        }
      }

      return result;
    });

    // update filters
    updateSizeCombobox(artwork.getImageSizes());
    updateLanguageCombobox(artwork.getLanguage());

    filterChanged();
  }

  private void updateSizeCombobox(List<ImageSizeAndUrl> newSizes) {
    cbSize.removeActionListener(filterListener);

    List<ImageSizeAndUrl> selectedItems = cbSize.getSelectedItems();
    List<ImageSizeAndUrl> allItems = cbSize.getItems();

    // build a Set of all distinct sizes
    Set<String> sizes = new HashSet<>();

    for (ImageSizeAndUrl sizeAndUrl : allItems) {
      sizes.add(sizeAndUrl.toString());
    }

    for (ImageSizeAndUrl sizeAndUrl : ListUtils.nullSafe(newSizes)) {
      if (!sizes.contains(sizeAndUrl.toString())) {
        allItems.add(sizeAndUrl);
        sizes.add(sizeAndUrl.toString());
      }
    }

    allItems.sort(ImageSizeAndUrl::compareTo);
    allItems.sort(Collections.reverseOrder());

    cbSize.setItems(allItems);
    cbSize.setSelectedItems(selectedItems);

    cbSize.addActionListener(filterListener);
  }

  private void updateLanguageCombobox(String language) {
    cbLanguage.removeActionListener(filterListener);

    List<MediaLanguages> selectedItems = cbLanguage.getSelectedItems();
    List<MediaLanguages> allItems = cbLanguage.getItems();

    // - indicates no text
    if ("-".equals(language)) {
      if (!allItems.contains(MediaLanguages.none)) {
        allItems.add(MediaLanguages.none);
      }
    }
    else {
      MediaLanguages mediaLanguages = MediaLanguages.get(language);
      if (!allItems.contains(mediaLanguages)) {
        allItems.add(mediaLanguages);
      }
    }

    // and add them in the right order
    List<MediaLanguages> newValues = new ArrayList<>();

    // add none in the front - this will come from MediaLanguages.valuesSorted()
    newValues.add(MediaLanguages.none);

    for (MediaLanguages mediaLanguages : MediaLanguages.valuesSorted()) {
      if (allItems.contains(mediaLanguages)) {
        newValues.add(mediaLanguages);
      }
    }

    cbLanguage.setItems(newValues);
    cbLanguage.setSelectedItems(selectedItems);

    cbLanguage.addActionListener(filterListener);
  }

  private void filterChanged() {
    panelImages.removeAll();
    ButtonModel selectedButton = null;

    if (buttonGroup != null) {
      selectedButton = buttonGroup.getSelection();
    }

    buttonGroup = new NoneSelectedButtonGroup();

    for (JPanel panel : imagePanels) {
      Object obj = panel.getClientProperty("MediaArtwork");
      if (!(obj instanceof MediaArtwork)) {
        continue;
      }

      MediaArtwork artwork = (MediaArtwork) obj;

      if (cbScraper.getSelectedItems().isEmpty() && cbSize.getSelectedItems().isEmpty() && cbLanguage.getSelectedItems().isEmpty()) {
        // nothing selected - add all
        panelImages.add(panel);
        for (Component child : panel.getComponents()) {
          if (child instanceof JCheckBox) {
            // JCheckBox is a subclass of JToggleButton -> skip
            continue;
          }

          if (child instanceof JToggleButton button) {
            buttonGroup.add(button);
          }
        }
      }
      else {
        // filter
        boolean scraperMatch = true;
        boolean sizeMatch = true;
        boolean languageMatch = true;

        // on scraper
        if (!cbScraper.getSelectedItems().isEmpty()) {
          scraperMatch = false;

          for (MediaScraper scraper : cbScraper.getSelectedItems()) {
            if (scraper.getId().equals(artwork.getProviderId())) {
              scraperMatch = true;
              break;
            }
          }
        }

        // on size
        if (!cbSize.getSelectedItems().isEmpty()) {
          sizeMatch = false;
          List<String> sizes = new ArrayList<>();

          for (ImageSizeAndUrl sizeAndUrl : cbSize.getSelectedItems()) {
            sizes.add(sizeAndUrl.toString());
          }

          for (ImageSizeAndUrl imageSizeAndUrl : artwork.getImageSizes()) {
            if (sizes.contains(imageSizeAndUrl.toString())) {
              sizeMatch = true;
              break;
            }
          }
        }

        // on language
        if (!cbLanguage.getSelectedItems().isEmpty()) {
          languageMatch = false;
          List<String> languages = new ArrayList<>();

          for (MediaLanguages mediaLanguages : cbLanguage.getSelectedItems()) {
            if (mediaLanguages == MediaLanguages.none) {
              languages.add("-");
              languages.add("");
            }
            else {
              languages.add(mediaLanguages.getLanguage().toLowerCase(Locale.ROOT));
            }
          }

          if (languages.contains(artwork.getLanguage())) {
            languageMatch = true;
          }
        }

        if (scraperMatch && sizeMatch && languageMatch) {
          panelImages.add(panel);
        }
      }
    }

    if (selectedButton != null) {
      buttonGroup.setSelected(selectedButton, true);
    }

    viewport.setLocked(true);
    panelImages.revalidate();
    scrollPane.repaint();

    SwingUtilities.invokeLater(() -> viewport.setLocked(false));
  }

  private void downloadAndPreviewImage(String url) {
    Runnable task = () -> {
      try {
        final MediaArtwork art;
        switch (type) {
          case BANNER:
            art = new MediaArtwork("", MediaArtworkType.BANNER);
            break;

          case CLEARART:
            art = new MediaArtwork("", MediaArtworkType.CLEARART);
            break;

          case DISC:
            art = new MediaArtwork("", MediaArtworkType.DISC);
            break;

          case BACKGROUND:
            art = new MediaArtwork("", BACKGROUND);
            break;

          case CLEARLOGO:
          case LOGO:
            art = new MediaArtwork("", MediaArtworkType.CLEARLOGO);
            break;

          case CHARACTERART:
            art = new MediaArtwork("", MediaArtworkType.CHARACTERART);
            break;

          case POSTER:
            art = new MediaArtwork("", MediaArtworkType.POSTER);
            break;

          case SEASON_POSTER:
            art = new MediaArtwork("", MediaArtworkType.SEASON_POSTER);
            break;

          case SEASON_FANART:
            art = new MediaArtwork("", MediaArtworkType.SEASON_FANART);
            break;

          case SEASON_BANNER:
            art = new MediaArtwork("", MediaArtworkType.SEASON_BANNER);
            break;

          case SEASON_THUMB:
            art = new MediaArtwork("", MediaArtworkType.SEASON_THUMB);
            break;

          case THUMB:
            art = new MediaArtwork("", MediaArtworkType.THUMB);
            break;

          case KEYART:
            art = new MediaArtwork("", MediaArtworkType.KEYART);
            break;

          default:
            return;
        }
        art.setPreviewUrl(url);
        art.setOriginalUrl(url);

        Url previewUrl = new Url(art.getPreviewUrl());
        byte[] artworkBytes = previewUrl.getBytesWithRetry(5);

        if (artworkBytes.length > 0) {

          SwingUtilities.invokeLater(() -> {
            try {
              addImage(artworkBytes, art);
            }
            catch (Exception e) {
              LOGGER.debug("Could not add image '{}' - '{}'", art.getPreviewUrl(), e.getMessage());
            }
          });
          tfImageUrl.setText("");
        }
        else {
          JOptionPane.showMessageDialog(ImageChooserDialog.this, TmmResourceBundle.getString("message.errorloadimage"));
        }
      }
      catch (Exception e) {
        LOGGER.error("could not download manually entered image url: {} - {}", tfImageUrl.getText(), e.getMessage());
      }
    };
    task.run();
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible && task == null) {
      startScraping();
    }

    super.setVisible(visible);
  }

  /**
   * call a new image chooser dialog without extrathumbs and extrafanart usage.<br />
   * this method also checks if there are valid IDs for scraping
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param mediaType
   *          the media for which artwork has to be chosen
   * @param defaultPath
   *          the default path to open
   */
  public static String chooseImage(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      MediaType mediaType, String defaultPath) {
    return chooseImage(parent, ids, type, artworkScrapers, null, null, mediaType, defaultPath);
  }

  /**
   * call a new image chooser dialog with extrathumbs and extrafanart usage.<br />
   * this method also checks if there are valid IDs for scraping
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param extraThumbs
   *          the extra thumbs
   * @param extraFanarts
   *          the extra fanarts
   * @param mediaType
   *          the media for which artwork has to be chosen
   * @param defaultPath
   *          the default path to open
   */
  public static String chooseImage(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      List<String> extraThumbs, List<String> extraFanarts, MediaType mediaType, String defaultPath) {
    if (ids.isEmpty()) {
      return "";
    }

    ImageLabel lblImage = new ImageLabel();
    ImageChooserDialog dialog = new ImageChooserDialog(parent, ids, type, artworkScrapers, lblImage, mediaType);

    dialog.bindExtraThumbs(extraThumbs);
    dialog.bindExtraFanarts(extraFanarts);
    dialog.setOpenFolderPath(defaultPath);

    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.startScraping();
    dialog.setVisible(true);
    return lblImage.getImageUrl();
  }

  /**
   * call a new image chooser dialog with extrathumbs and extrafanart usage.<br />
   * this method also checks if there are valid IDs for scraping
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type private List<MediaArtwork> artwork;
   * @param artworkScrapers
   *          the artwork providers
   * @param extraThumbs
   *          the extra thumbs
   * @param extraFanarts
   *          the extra fanarts
   * @param artwork
   *          all pre-selected artwork to show
   * @param mediaType
   *          the media for which artwork has to be chosen
   * @param defaultPath
   *          the default path to open
   */
  public static String chooseImage(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      List<String> extraThumbs, List<String> extraFanarts, List<MediaArtwork> artwork, MediaType mediaType, String defaultPath) {
    if (ids.isEmpty() || ListUtils.isEmpty(artwork)) {
      return "";
    }

    List<MediaArtwork> filteredArtwork = artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == type).toList();

    ImageLabel lblImage = new ImageLabel();
    ImageChooserDialog dialog = new ImageChooserDialog(parent, ids, type, artworkScrapers, lblImage, mediaType);

    dialog.setArtwork(filteredArtwork);
    dialog.bindExtraThumbs(extraThumbs);
    dialog.bindExtraFanarts(extraFanarts);
    dialog.setOpenFolderPath(defaultPath);

    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.startScraping();
    dialog.setVisible(true);
    return lblImage.getImageUrl();
  }

  private class OkAction extends AbstractAction {
    public OkAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.ok"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("image.seteselected"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaArtwork artwork = null;
      ImageSizeAndUrl resolution = null;

      List<String> selectedExtraThumbs = getSelectedExtraThumbs();
      List<String> selectedExtraFanarts = getSelectedExtrafanarts();

      // get selected button
      for (JToggleButton button : buttons) {
        if (button.isSelected()) {
          Object clientProperty = button.getClientProperty("MediaArtwork");
          if (clientProperty instanceof MediaArtwork) {
            artwork = (MediaArtwork) clientProperty;
            clientProperty = button.getClientProperty("MediaArtworkSize");
            // try to get the size
            if (clientProperty instanceof JComboBox) {
              @SuppressWarnings("rawtypes")
              JComboBox cb = (JComboBox) clientProperty;
              if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
                resolution = (ImageSizeAndUrl) cb.getSelectedItem();
              }
            }
            break;
          }
        }
      }

      // nothing selected
      if (artwork == null && selectedExtraFanarts.isEmpty() && selectedExtraThumbs.isEmpty()) {
        JOptionPane.showMessageDialog(ImageChooserDialog.this, TmmResourceBundle.getString("image.noneselected"));
        return;
      }

      if (artwork != null) {
        imageLabel.clearImage();
        if (resolution != null) {
          imageLabel.setImageUrl(resolution.getUrl());
        }
        else {
          imageLabel.setImageUrl(artwork.getOriginalUrl());
        }
      }

      // extrathumbs
      if (extraThumbs != null) {
        extraThumbs.clear();
        extraThumbs.addAll(selectedExtraThumbs);
      }

      // extrafanart
      if (extraFanarts != null) {
        extraFanarts.clear();
        extraFanarts.addAll(selectedExtraFanarts);
      }

      task.cancel(true);
      setVisible(false);
    }

    /**
     * Process extra thumbs.
     */
    private List<String> getSelectedExtraThumbs() {
      List<String> selectedExtraThumbs = new ArrayList<>();

      // get extrathumbs
      for (JToggleButton button : buttons) {
        if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox
            && button.getClientProperty("MediaArtwork") instanceof MediaArtwork
            && button.getClientProperty("MediaArtworkSize") instanceof JComboBox) {
          JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
          if (chkbx.isSelected()) {
            MediaArtwork artwork = (MediaArtwork) button.getClientProperty("MediaArtwork");
            @SuppressWarnings("rawtypes")
            JComboBox cb = (JComboBox) button.getClientProperty("MediaArtworkSize");
            if (cb.getSelectedItem() instanceof ImageSizeAndUrl size) {
              selectedExtraThumbs.add(size.getUrl());
            }
            else if (cb.getSelectedItem() instanceof String) {
              selectedExtraThumbs.add(artwork.getOriginalUrl());
            }
          }
        }
      }

      return selectedExtraThumbs;
    }

    /**
     * Process extra fanart.
     */
    private List<String> getSelectedExtrafanarts() {
      List<String> selectedExtrafanarts = new ArrayList<>();

      // get extrafanart
      for (JToggleButton button : buttons) {
        if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox
            && button.getClientProperty("MediaArtwork") instanceof MediaArtwork
            && button.getClientProperty("MediaArtworkSize") instanceof JComboBox) {
          JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
          if (chkbx.isSelected()) {
            MediaArtwork artwork = (MediaArtwork) button.getClientProperty("MediaArtwork");
            @SuppressWarnings("rawtypes")
            JComboBox cb = (JComboBox) button.getClientProperty("MediaArtworkSize");
            if (cb.getSelectedItem() instanceof ImageSizeAndUrl size) {
              selectedExtrafanarts.add(size.getUrl());
            }
            else if (cb.getSelectedItem() instanceof String) {
              selectedExtrafanarts.add(artwork.getOriginalUrl());
            }
          }
        }
      }
      return selectedExtrafanarts;
    }
  }

  private class CancelAction extends AbstractAction {
    public CancelAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("Button.cancel"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      task.cancel(true);
      setVisible(false);
    }
  }

  private class DownloadTask extends SwingWorker<Void, DownloadChunk> {
    private final Map<String, Object>                ids;
    private final List<MediaScraper>                 artworkScrapers;
    private final List<MediaArtwork>                 prescrapedArtwork;

    private boolean                                  imagesFound = false;
    private ExecutorCompletionService<DownloadChunk> service;

    public DownloadTask(Map<String, Object> ids, List<MediaScraper> artworkScrapers, List<MediaArtwork> artwork) {
      this.ids = ids;
      this.artworkScrapers = artworkScrapers;
      this.prescrapedArtwork = artwork;
    }

    @Override
    public Void doInBackground() {
      if (ids.isEmpty()) {
        JOptionPane.showMessageDialog(ImageChooserDialog.this, TmmResourceBundle.getString("image.download.noid"));
        return null;
      }

      SwingUtilities.invokeLater(() -> startProgressBar(TmmResourceBundle.getString("image.download.progress")));

      if (ListUtils.isEmpty(artworkScrapers) && ListUtils.isEmpty(prescrapedArtwork)) {
        return null;
      }

      // open a thread pool to parallel download the images
      ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 10, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
      pool.allowCoreThreadTimeOut(true);
      service = new ExecutorCompletionService<>(pool);

      if (ListUtils.isEmpty(prescrapedArtwork)) {
        // get images from all artwork providers
        for (MediaScraper scraper : artworkScrapers) {
          try {
            IMediaArtworkProvider artworkProvider = (IMediaArtworkProvider) scraper.getMediaProvider();

            ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(mediaType);
            if (mediaType == MediaType.MOVIE || mediaType == MediaType.MOVIE_SET) {
              options.setLanguage(MovieModuleManager.getInstance().getSettings().getDefaultImageScraperLanguage());
              options.setFanartSize(MovieModuleManager.getInstance().getSettings().getImageFanartSize());
              options.setPosterSize(MovieModuleManager.getInstance().getSettings().getImagePosterSize());
            }
            else if (mediaType == MediaType.TV_SHOW || mediaType == MediaType.TV_EPISODE) {
              options.setLanguage(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
              options.setFanartSize(TvShowModuleManager.getInstance().getSettings().getImageFanartSize());
              options.setPosterSize(TvShowModuleManager.getInstance().getSettings().getImagePosterSize());
              options.setThumbSize(TvShowModuleManager.getInstance().getSettings().getImageThumbSize());
            }
            else {
              continue;
            }

            switch (type) {
              case POSTER:
                options.setArtworkType(MediaArtworkType.POSTER);
                break;

              case BACKGROUND:
                options.setArtworkType(BACKGROUND);
                break;

              case BANNER:
                options.setArtworkType(MediaArtworkType.BANNER);
                break;

              case SEASON_POSTER:
                options.setArtworkType(MediaArtworkType.SEASON_POSTER);
                break;

              case SEASON_FANART:
                options.setArtworkType(MediaArtworkType.SEASON_FANART);
                break;

              case SEASON_BANNER:
                options.setArtworkType(MediaArtworkType.SEASON_BANNER);
                break;

              case SEASON_THUMB:
                options.setArtworkType(MediaArtworkType.SEASON_THUMB);
                break;

              case CLEARART:
                options.setArtworkType(MediaArtworkType.CLEARART);
                break;

              case DISC:
                options.setArtworkType(MediaArtworkType.DISC);
                break;

              case CLEARLOGO:
              case LOGO:
                options.setArtworkType(MediaArtworkType.CLEARLOGO);
                break;

              case CHARACTERART:
                options.setArtworkType(MediaArtworkType.CHARACTERART);
                break;

              case KEYART:
                options.setArtworkType(MediaArtworkType.KEYART);
                break;

              case THUMB:
                options.setArtworkType(MediaArtworkType.THUMB);
                break;
            }

            // populate ids
            options.setIds(ids);

            // get the artwork
            List<MediaArtwork> artwork = artworkProvider.getArtwork(options);
            if (artwork == null || artwork.isEmpty()) {
              continue;
            }

            processImages(artwork);
            if (isCancelled()) {
              return null;
            }
          }
          catch (MissingIdException e) {
            LOGGER.debug("could not fetch artwork: {}", e.getIds());
          }
          catch (ScrapeException e) {
            LOGGER.error("getArtwork", e);
          }
          catch (Exception e) {
            if (e instanceof InterruptedException || e instanceof InterruptedIOException) { // NOSONAR
              // shutdown the pool
              pool.getQueue().clear();
              pool.shutdownNow();

              return null;
            }
            LOGGER.error("could not process artwork downloading - {}", e.getMessage());
          }
        } // end foreach scraper
      }
      else {
        // add pre-scraped artwork
        processImages(prescrapedArtwork);
      }

      // wait for all downloads to finish
      pool.shutdown();
      while (true) {
        try {
          final Future<DownloadChunk> future = service.poll(1, TimeUnit.SECONDS);
          if (future != null) {
            DownloadChunk dc = future.get();
            if (dc.imageData.length > 0) {
              publish(dc);
              imagesFound = true;
            }
          }
          else if (pool.isTerminated()) {
            // no result got and the pool is terminated -> we're finished
            break;
          }
        }
        catch (InterruptedException e) { // NOSONAR
          return null;
        }
        catch (ExecutionException e) {
          LOGGER.error("ThreadPool imageChooser: Error getting result! - {}", e.getMessage());
        }
      }

      return null;
    }

    private void processImages(List<MediaArtwork> artwork) {
      int season = MediaIdUtil.getIdAsIntOrDefault(ids, "tvShowSeason", -1);

      // display all images
      for (MediaArtwork art : artwork) {
        if (isCancelled()) {
          return;
        }
        if (art.getPreviewUrl().isEmpty()) {
          continue;
        }

        // for seasons, just use the season related artwork
        if (season > -1 && season != art.getSeason()) {
          continue;
        }

        Callable<DownloadChunk> callable = () -> {
          Url url = new Url(art.getPreviewUrl());
          DownloadChunk chunk = new DownloadChunk();
          chunk.artwork = art;
          try {
            chunk.imageData = url.getBytesWithRetry(5);
          }
          catch (Exception e) {
            LOGGER.debug("Could not add image '{}' - '{}'", art.getPreviewUrl(), e.getMessage());
          }
          return chunk;
        };

        service.submit(callable);
      }
    }

    @Override
    protected void process(List<DownloadChunk> chunks) {
      for (DownloadChunk chunk : chunks) {
        try {
          addImage(chunk.imageData, chunk.artwork);
        }
        catch (Exception e) {
          LOGGER.debug("Could not add image '{}' - '{}'", chunk.artwork.getPreviewUrl(), e.getMessage());
        }
      }
    }

    @Override
    public void done() {
      if (!imagesFound) {
        JLabel lblNothingFound = new JLabel(TmmResourceBundle.getString("image.download.nothingfound"));
        TmmFontHelper.changeFont(lblNothingFound, 1.33);
        panelImages.add(lblNothingFound);
        panelImages.validate();
        panelImages.getParent().validate();
      }
      SwingUtilities.invokeLater(ImageChooserDialog.this::stopProgressBar);
    }
  }

  private static class DownloadChunk {
    private byte[]       imageData;
    private MediaArtwork artwork;
  }

  private class LocalFileChooseAction extends AbstractAction {
    public LocalFileChooseAction() {
      putValue(NAME, TmmResourceBundle.getString("image.choose.file"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("image.choose.file"));
      putValue(SMALL_ICON, IconManager.FILE_OPEN_INV);
      putValue(LARGE_ICON_KEY, IconManager.FILE_OPEN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String path;
      if (StringUtils.isNotBlank(openFolderPath)) {
        path = openFolderPath;
      }
      else {
        path = TmmProperties.getInstance().getProperty(DIALOG_ID + ".path");
      }

      Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("image.choose"), path,
          new FileNameExtensionFilter("Image files", ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".tbn", ".webp"));
      if (file != null && Utils.isRegularFile(file)) {
        String fileName = file.toAbsolutePath().toString();
        imageLabel.clearImage();
        imageLabel.setImageUrl("file:/" + fileName);
        task.cancel(true);
        TmmProperties.getInstance().putProperty(DIALOG_ID + ".path", file.getParent().toString());
        setVisible(false);
      }
    }
  }

  private static final class LockableViewPort extends JViewport {

    private boolean locked = false;

    @Override
    public void setViewPosition(Point p) {
      if (locked) {
        return;
      }
      super.setViewPosition(p);
    }

    public boolean isLocked() {
      return locked;
    }

    public void setLocked(boolean locked) {
      this.locked = locked;
    }
  }

  private static class NoneSelectedButtonGroup extends ButtonGroup {

    @Override
    public void setSelected(ButtonModel model, boolean selected) {
      if (selected) {
        super.setSelected(model, selected);
      }
      else {
        clearSelection();
      }
    }
  }
}
