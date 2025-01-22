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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle.TaskType;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeNfoParser;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowReadEpisodeNfoAction. Used to rewrite the NFOs for selected episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowReadEpisodeNfoAction extends TmmAction {
  public TvShowReadEpisodeNfoAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshowepisode.readnfo"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshowepisode.readnfo.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    final List<TvShowEpisode> selectedEpisodes = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();
    if (selectedEpisodes.isEmpty()) {
      return;
    }

    // reload selected NFOs
    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("tvshowepisode.readnfo"), selectedEpisodes.size(), TaskType.BACKGROUND_TASK) {
          @Override
          protected void doInBackground() {
            int i = 0;
            for (TvShowEpisode episode : selectedEpisodes) {
              TvShowEpisode tempEpisode = null;

              // process all registered NFOs
              for (MediaFile mf : episode.getMediaFiles(MediaFileType.NFO)) {
                try {
                  List<TvShowEpisode> episodesFromNfo = TvShowEpisodeNfoParser.parseNfo(mf.getFileAsPath()).toTvShowEpisodes();

                  // at the first NFO we get a episode object
                  if (tempEpisode == null) {
                    if (episodesFromNfo.size() == 1) {
                      tempEpisode = episodesFromNfo.get(0);
                    }
                    else {
                      for (TvShowEpisode ep : episodesFromNfo) {
                        if (episode.getSeason() == ep.getSeason() && episode.getEpisode() == ep.getEpisode()) {
                          tempEpisode = ep;
                          break;
                        }
                      }
                    }
                    continue;
                  }

                  // every other NFO gets merged into that temp. episode object
                  // but only if we have detected an episode# at first... (do not match -1 EPs)
                  if (tempEpisode != null && episodesFromNfo.size() > 1) {
                    for (TvShowEpisode ep : episodesFromNfo) {
                      if (episode.getEpisode() > 0 && episode.getSeason() == ep.getSeason() && episode.getEpisode() == ep.getEpisode()) {
                        tempEpisode.merge(ep);
                        break;
                      }
                    }
                  }
                }
                catch (Exception ignored) {
                }
              }

              // no MF (yet)? try to find NFO...
              // it might have been added w/o UDS, and since we FORCE a read...
              if (tempEpisode == null) {
                MediaFile vid = episode.getMainVideoFile();
                String name = vid.getFilenameWithoutStacking();
                name = FilenameUtils.getBaseName(name) + ".nfo";
                Path nfo = vid.getFileAsPath().getParent().resolve(name);
                if (Files.exists(nfo)) {
                  try {
                    episode.addToMediaFiles(new MediaFile(nfo));
                    List<TvShowEpisode> episodesFromNfo = TvShowEpisodeNfoParser.parseNfo(nfo).toTvShowEpisodes();
                    if (episodesFromNfo.size() == 1) {
                      tempEpisode = episodesFromNfo.get(0);
                    }
                    else {
                      for (TvShowEpisode ep : episodesFromNfo) {
                        if (episode.getSeason() == ep.getSeason() && episode.getEpisode() == ep.getEpisode()) {
                          tempEpisode = ep;
                          break;
                        }
                      }
                    }
                  }
                  catch (Exception ignored) {
                  }
                }
              }

              // did we get movie data from our NFOs
              if (tempEpisode != null) {
                // force merge it to the actual episode object
                episode.forceMerge(tempEpisode);
                episode.saveToDb();
              }

              publishState(++i);
              if (cancel) {
                break;
              }
            }

          }
        });
  }
}
