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

package org.tinymediamanager.ui.tvshows.dialogs;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * the class {@link TvShowMissingEpisodeListDialog} is used to display a table full of missing episodes for the given TV shows
 *
 * @author Wolfgang Janes
 */
public class TvShowMissingEpisodeListDialog extends TmmDialog {

  private static final Logger               LOGGER            = LoggerFactory.getLogger(TvShowMissingEpisodeListDialog.class);

  private final JButton                     btnClose;
  private final JCheckBox                   chckbxShowMissingSpecials;
  private final JCheckBox                   chckbxIncludeNotAired;
  private final JProgressBar                pbListEpisodes;
  private final EventList<EpisodeContainer> results;
  private final TmmTable                    tblMissingEpisodeList;

  private SwingWorker<Void, Void>           episodeListWorker = null;

  public TvShowMissingEpisodeListDialog(List<TvShow> tvShows) {
    super(TmmResourceBundle.getString("tvshow.missingepisodelist"), "missingepisodelist");

    results = new SortedList<>(GlazedListsSwing.swingThreadProxyList(GlazedLists.threadSafeList(new BasicEventList<>())),
        new EpisodeContainerComparator());
    TmmTableModel<EpisodeContainer> missingEpisodeListModel = new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(results),
        new MissingEpisodeListTableFormat());

    // UI
    {
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("", "[700lp,grow]", "[grow]"));
      getContentPane().add(panelContent, BorderLayout.CENTER);

      tblMissingEpisodeList = new TmmTable(missingEpisodeListModel);
      JScrollPane scrollPane = new JScrollPane();
      tblMissingEpisodeList.configureScrollPane(scrollPane);
      panelContent.add(scrollPane, "cell 0 0, grow");
    }
    {
      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new MigLayout("", "[][grow]", "[]"));

      chckbxShowMissingSpecials = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingespecials"));
      chckbxShowMissingSpecials.setSelected(TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials());

      chckbxShowMissingSpecials.addItemListener(e -> {
        if (episodeListWorker != null && !episodeListWorker.isDone()) {
          episodeListWorker.cancel(true);
        }
        results.clear();
        episodeListWorker = new EpisodeListWorker(tvShows);
        episodeListWorker.execute();
      });
      infoPanel.add(chckbxShowMissingSpecials, "cell 0 0");

      chckbxIncludeNotAired = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingnotaired"));
      chckbxIncludeNotAired.setSelected(TvShowModuleManager.getInstance().getSettings().isDisplayMissingNotAired());

      chckbxIncludeNotAired.addItemListener(e -> {
        if (episodeListWorker != null && !episodeListWorker.isDone()) {
          episodeListWorker.cancel(true);
        }
        results.clear();
        episodeListWorker = new EpisodeListWorker(tvShows);
        episodeListWorker.execute();
      });
      infoPanel.add(chckbxIncludeNotAired, "cell 1 0");

      pbListEpisodes = new JProgressBar();
      infoPanel.add(pbListEpisodes, "cell 1 0");

      setBottomInformationPanel(infoPanel);
    }
    {
      btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(e -> setVisible(false));
      this.addDefaultButton(btnClose);
    }

    episodeListWorker = new EpisodeListWorker(tvShows);
    episodeListWorker.execute();
  }

  private static class EpisodeContainer {
    String tvShowTitle;
    int    season;
    int    episode;
    String episodeTitle;
    Date   airedDate;
  }

  private static class EpisodeContainerComparator implements Comparator<EpisodeContainer> {

    @Override
    public int compare(EpisodeContainer o1, EpisodeContainer o2) {
      if (!o1.tvShowTitle.equals(o2.tvShowTitle)) {
        return o1.tvShowTitle.compareTo(o2.tvShowTitle);
      }

      if (o1.season != o2.season) {
        return o1.season - o2.season;
      }

      return o1.episode - o2.episode;
    }
  }

  private static class MissingEpisodeListTableFormat extends TmmTableFormat<EpisodeContainer> {
    MissingEpisodeListTableFormat() {
      Comparator<String> stringComparator = new StringComparator();
      Comparator<Integer> integerComparator = new IntegerComparator();
      Comparator<Date> dateComparator = new DateComparator();
      FontMetrics fontMetrics = getFontMetrics();

      /*
       * title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.tvshow"), "title", container -> container.tvShowTitle, String.class);
      col.setColumnComparator(stringComparator);
      addColumn(col);

      /*
       * season
       */
      col = new Column(TmmResourceBundle.getString("metatag.season"), "season", container -> container.season, Integer.class);
      col.setColumnComparator(integerComparator);
      col.setCellRenderer(new RightAlignTableCellRenderer());
      col.setColumnResizeable(false);
      int seasonWidth = fontMetrics.stringWidth(TmmResourceBundle.getString("metatag.season"));
      col.setMinWidth((int) (seasonWidth * 1.2f));
      col.setMaxWidth((int) (seasonWidth * 1.5f));
      addColumn(col);

      /*
       * episode
       */
      col = new Column(TmmResourceBundle.getString("metatag.episode"), "episode", container -> container.episode, Integer.class);
      col.setColumnComparator(integerComparator);
      col.setCellRenderer(new RightAlignTableCellRenderer());
      col.setColumnResizeable(false);
      int episodeWidth = fontMetrics.stringWidth(TmmResourceBundle.getString("metatag.episode"));
      col.setMinWidth((int) (episodeWidth * 1.2f));
      col.setMaxWidth((int) (episodeWidth * 1.5f));
      addColumn(col);

      /*
       * episode aired date
       */
      col = new Column(TmmResourceBundle.getString("metatag.aired"), "airedDate", container -> container.airedDate, Date.class);
      col.setColumnComparator(dateComparator);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * episode title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "episodeTitle", container -> container.episodeTitle, String.class);
      col.setColumnComparator(stringComparator);
      col.setColumnResizeable(true);
      addColumn(col);
    }
  }

  private class EpisodeListWorker extends SwingWorker<Void, Void> {

    private final List<TvShow> tvShows;

    EpisodeListWorker(List<TvShow> tvShows) {
      this.tvShows = tvShows;
    }

    @Override
    protected Void doInBackground() {

      btnClose.setEnabled(false);
      chckbxShowMissingSpecials.setEnabled(false);
      chckbxIncludeNotAired.setEnabled(false);
      startProgressBar();
      compareTvShows();

      return null;
    }

    private List<MediaMetadata> getEpisodes(TvShow tvShow) {
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setLanguage(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
      options.setCertificationCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry());
      options.setReleaseDateCountry(TvShowModuleManager.getInstance().getSettings().getReleaseDateCountry());

      MediaScraper mediaScraper = TvShowModuleManager.getInstance().getTvShowList().getDefaultMediaScraper();
      MediaMetadata md = new MediaMetadata(mediaScraper.getMediaProvider().getProviderInfo().getId());
      options.setMetadata(md);

      for (Map.Entry<String, Object> entry : tvShow.getIds().entrySet()) {
        options.setId(entry.getKey(), entry.getValue().toString());
      }

      try {
        return ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getEpisodeList(options);
      }
      catch (MissingIdException e) {
        LOGGER.warn("missing id for scrape");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, tvShow, "scraper.error.missingid"));
      }
      catch (ScrapeException e) {
        LOGGER.error("getMetadata", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, tvShow, "message.scrape.metadataepisodefailed", new String[] { ":", e.getLocalizedMessage() }));
      }
      return null;
    }

    /**
     * Compare the episodes of the selected TvShow in TMM with the scraped List
     */
    private void compareTvShows() {

      for (TvShow tvshow : tvShows) {
        if (tvshow.getIds().isEmpty()) {
          LOGGER.info("we cannot scrape (no ID): {}", tvshow.getTitle());
          continue;
        }

        List<TvShowEpisode> scrapedEpisodes = tvshow.getEpisodes();
        List<MediaMetadata> mediaEpisodes = getEpisodes(tvshow);

        for (MediaMetadata mediaEpisode : ListUtils.nullSafe(mediaEpisodes)) {

          boolean entryFound = false;

          EpisodeContainer container = new EpisodeContainer();
          container.tvShowTitle = tvshow.getTitle();
          container.episodeTitle = mediaEpisode.getTitle();

          MediaEpisodeNumber episodeNumber = mediaEpisode.getEpisodeNumber(tvshow.getEpisodeGroup());
          if (episodeNumber != null) {
            container.season = episodeNumber.season();
            container.episode = episodeNumber.episode();
          }

          container.airedDate = mediaEpisode.getReleaseDate();

          boolean alreadyAired = mediaEpisode.getReleaseDate() != null && mediaEpisode.getReleaseDate().compareTo(new Date()) <= 0;
          if (!alreadyAired && !chckbxIncludeNotAired.isSelected()) {
            continue;
          }

          for (TvShowEpisode scrapedEpisode : scrapedEpisodes) {

            boolean showMissingSpecials = chckbxShowMissingSpecials.isSelected();
            if (!showMissingSpecials) {
              if (container.season <= 0) {
                entryFound = true;
                break;
              }
            }

            // compare on active episode group
            if (scrapedEpisode.getEpisode() == container.episode && scrapedEpisode.getSeason() == container.season) {
              entryFound = true;
              break;
            }

            // also compare IDs
            for (var entry : scrapedEpisode.getIds().entrySet()) {
              if (entry.getValue().equals(mediaEpisode.getId(entry.getKey()))) {
                entryFound = true;
                break;
              }
            }
          }

          if (!entryFound) {
            results.add(container);
          }
        }
      }
    }

    @Override
    protected void done() {
      stopProgressBar();
      btnClose.setEnabled(true);
      chckbxShowMissingSpecials.setEnabled(true);
      chckbxIncludeNotAired.setEnabled(true);
      tblMissingEpisodeList.adjustColumnPreferredWidths(3);
    }
  }

  private void startProgressBar() {
    SwingUtilities.invokeLater(() -> {
      pbListEpisodes.setVisible(true);
      pbListEpisodes.setIndeterminate(true);
    });
  }

  private void stopProgressBar() {
    SwingUtilities.invokeLater(() -> {
      pbListEpisodes.setVisible(false);
      pbListEpisodes.setIndeterminate(false);
    });
  }
}
