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
package org.tinymediamanager.ui.tvshows.filters;

import static org.tinymediamanager.core.MediaFileType.AUDIO;
import static org.tinymediamanager.core.MediaFileType.VIDEO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowAudioLanguageFilter} is a filter for audio languages for TV shows
 * 
 * @author Wolfgang Janes
 */
public class TvShowAudioLanguageFilter extends AbstractCheckComboBoxTvShowUIFilter<String> {
  private final TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

  public TvShowAudioLanguageFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAudioLanguageArray();
    tvShowList.addPropertyChangeListener(Constants.AUDIO_LANGUAGES, evt -> SwingUtilities.invokeLater(this::buildAudioLanguageArray));

  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    List<String> selectedItems = checkComboBox.getSelectedItems();

    for (TvShowEpisode episode : episodes) {
      List<MediaFile> mfs = episode.getMediaFiles(VIDEO, AUDIO);
      for (MediaFile mf : mfs) {
        // check for explicit empty search
        if (!invert && (selectedItems.isEmpty() && mf.getAudioLanguagesList().isEmpty())) {
          return true;
        }
        else if (invert && (selectedItems.isEmpty() && !mf.getAudioLanguagesList().isEmpty())) {
          return true;
        }

        if (invert == Collections.disjoint(selectedItems, mf.getAudioLanguagesList())) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.language"));
  }

  @Override
  public String getId() {
    return "tvShowAudioLanguage";
  }

  private void buildAudioLanguageArray() {
    List<String> audios = new ArrayList<>(tvShowList.getAudioLanguagesInEpisodes());
    Collections.sort(audios);
    setValues(audios);
  }
}
