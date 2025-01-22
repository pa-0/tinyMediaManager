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

package org.tinymediamanager.ui.tvshows.dialogs;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.tvshows.TvShowChooserModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * the class {@link TvShowChooserEpisodeListDialog} is used to compare local files to available episode lists
 * 
 * @author Manuel Laggner
 */
class TvShowChooserEpisodeListDialog extends TmmDialog {
  private final TvShowChooserModel           tvShowChooserModel;
  private final EventList<EpisodeContainer>  episodes;
  private final JComboBox<MediaEpisodeGroup> cbEpisodeGroup;
  private final TmmTable                     tableEpisodes;

  public TvShowChooserEpisodeListDialog(JDialog owner, TvShow tvShow, TvShowChooserModel model) {
    super(owner, TmmResourceBundle.getString("tvshowchooser.episodelist"), "tvShowChooserEpisodeList");
    this.tvShowChooserModel = model;

    episodes = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(EpisodeContainer.class));

    /* UI components */
    JPanel contentPanel = new JPanel();
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("", "[600lp:900lp,grow]", "[][400lp:500lp,grow]"));
    {
      cbEpisodeGroup = new JComboBox();
      cbEpisodeGroup.addItemListener((event) -> mixinEpisodeNumbers());
      contentPanel.add(cbEpisodeGroup, "cell 0 0");
    }
    {
      TmmTableModel<EpisodeContainer> tableModel = new TmmTableModel<>(episodes, new EpisodeContainerTableFormat());
      tableEpisodes = new TmmTable(tableModel);
      tableEpisodes.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

      JScrollPane scrollPaneFiles = new JScrollPane();
      tableEpisodes.configureScrollPane(scrollPaneFiles);
      contentPanel.add(scrollPaneFiles, "cell 0 1,grow");

      scrollPaneFiles.setViewportView(tableEpisodes);
    }

    // data init
    model.getEpisodeGroups().forEach(cbEpisodeGroup::addItem);
    cbEpisodeGroup.setSelectedItem(model.getEpisodeGroup());

    List<TvShowEpisode> epl = new ArrayList<>(tvShow.getEpisodes());
    epl.sort(Comparator.comparingInt((TvShowEpisode o) -> o.getSeason()).thenComparingInt(TvShowEpisode::getEpisode));

    for (TvShowEpisode episode : epl) {
      EpisodeContainer container = new EpisodeContainer(episode);
      episodes.add(container);
    }

    mixinEpisodeNumbers();
  }

  private void mixinEpisodeNumbers() {
    MediaEpisodeGroup episodeGroup = (MediaEpisodeGroup) cbEpisodeGroup.getSelectedItem();

    for (EpisodeContainer episodeContainer : episodes) {
      episodeContainer.clearData();

      if (episodeGroup != null) {
        // try to find the best matching episode in the given episode group
        for (MediaMetadata md : tvShowChooserModel.getEpisodeList()) {
          MediaEpisodeNumber episodeNumber = md.getEpisodeNumber(episodeGroup);
          if (episodeNumber == null) {
            continue;
          }

          if (episodeContainer.tvShowEpisode.getSeason() == episodeNumber.season()
              && episodeContainer.tvShowEpisode.getEpisode() == episodeNumber.episode()) {
            episodeContainer.setTitle(md.getTitle());
            break;
          }

          // same with matching date
          if (!(episodeContainer.tvShowEpisode.getSeason() > 0 && episodeContainer.tvShowEpisode.getEpisode() > 0)
              && episodeContainer.tvShowEpisode.getFirstAired() != null && md.getReleaseDate() != null) {
            LocalDate epdate = episodeContainer.getFirstAired();
            LocalDate mddate = LocalDate.ofInstant(md.getReleaseDate().toInstant(), ZoneId.systemDefault());
            if (epdate.equals(mddate)) {
              episodeContainer.setTitle(md.getTitle());
              break;
            }
          }
        }
      }
    }
    tableEpisodes.adjustColumnPreferredWidths(6);
  }

  private static class EpisodeContainer extends AbstractModelObject {
    private final TvShowEpisode tvShowEpisode;
    String                      title;
    float                       score;

    public EpisodeContainer(TvShowEpisode tvShowEpisode) {
      this.tvShowEpisode = tvShowEpisode;
    }

    public void clearData() {
      setTitle("");
    }

    public String getFilename() {
      return tvShowEpisode.getMainVideoFile().getFilename();
    }

    public int getEpisode() {
      return tvShowEpisode.getEpisode();
    }

    public int getSeason() {
      return tvShowEpisode.getSeason();
    }

    public String getEpisodeTitle() {
      return tvShowEpisode.getTitle();
    }

    public LocalDate getFirstAired() {
      return tvShowEpisode.getFirstAired() == null ? null : LocalDate.ofInstant(tvShowEpisode.getFirstAired().toInstant(), ZoneId.systemDefault());
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String newValue) {
      String oldValue = this.title;
      this.title = newValue;
      firePropertyChange("title", oldValue, newValue);

      calculateScore();
    }

    public String getScore() {
      return String.format("%.0f %%", score * 100);
    }

    public void calculateScore() {
      float score = 0;
      if (StringUtils.isNotBlank(title)) {
        float titleScore = MetadataUtil.calculateScore(title, tvShowEpisode.getTitle());

        String cleanedFilename = TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(tvShowEpisode.getMainVideoFile().getBasename(),
            tvShowEpisode.getTvShow().getTitle());
        float filenameScore = MetadataUtil.calculateScore(title, cleanedFilename);

        score = Math.max(titleScore, filenameScore);
      }

      float oldValue = this.score;
      this.score = score;
      firePropertyChange("score", oldValue, this.score);
    }
  }

  private static class EpisodeContainerTableFormat extends TmmTableFormat<EpisodeContainer> {
    public EpisodeContainerTableFormat() {
      /*
       * season
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.season"), "season", EpisodeContainer::getSeason, Integer.class);
      addColumn(col);

      /*
       * episode
       */
      col = new Column(TmmResourceBundle.getString("metatag.episode"), "episode", EpisodeContainer::getEpisode, Integer.class);
      addColumn(col);

      /*
       * release date
       */
      col = new Column(TmmResourceBundle.getString("metatag.aired"), "aired", EpisodeContainer::getFirstAired, LocalDate.class);
      addColumn(col);

      /*
       * filename
       */
      col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", EpisodeContainer::getFilename, String.class);
      addColumn(col);

      /*
       * local EP title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "localTitle", EpisodeContainer::getEpisodeTitle, String.class);
      addColumn(col);

      /*
       * EP title from EG
       */
      col = new Column(TmmResourceBundle.getString("metatag.episode.group"), "title", EpisodeContainer::getTitle, String.class);
      addColumn(col);

      /*
       * score
       */
      col = new Column(TmmResourceBundle.getString("tmm.similarityscore"), "score", EpisodeContainer::getScore, String.class);
      col.setHeaderIcon(IconManager.VIDEO_BITRATE);
      col.setCellRenderer(new RightAlignTableCellRenderer());
      addColumn(col);
    }
  }
}
