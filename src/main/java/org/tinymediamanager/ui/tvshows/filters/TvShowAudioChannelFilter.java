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

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a audio channel filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowAudioChannelFilter extends AbstractCheckComboBoxTvShowUIFilter<String> {
  private final TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

  public TvShowAudioChannelFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.substring(0, 1).equals(s2.substring(0, 1))); // first char is channel
    buildAudioChannelArray();
    tvShowList.addPropertyChangeListener(Constants.AUDIO_CHANNEL, evt -> SwingUtilities.invokeLater(this::buildAudioChannelArray));
  }

  @Override
  public String getId() {
    return "tvShowAudioChannel";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<String> audioChannels = new ArrayList<String>();
    for (String values : checkComboBox.getSelectedItems()) {
      audioChannels.add(values.substring(0, 1)); // MI does not return more than 8 channels, so 16 is no issue ;)
    }

    // search codec in the episodes
    for (TvShowEpisode episode : episodes) {
      List<MediaFile> mfs = episode.getMediaFiles(VIDEO, AUDIO);
      for (MediaFile mf : mfs) {
        for (String channels : mf.getAudioChannelsList()) {
          if (audioChannels.contains(channels.substring(0, 1))) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private void buildAudioChannelArray() {
    List<String> audioChannel = new ArrayList<>();
    for (int channel : tvShowList.getAudioChannelsInEpisodes()) {
      audioChannel.add(audioChannelNotation(channel));
    }
    Collections.sort(audioChannel);
    setValues(audioChannel);
  }

  private String audioChannelNotation(int channels) {
    String ret = "";
    switch (channels) {
      case 0:
        ret = "0 (no audio)";
        break;

      case 1:
        ret = "1 (Mono)";
        break;

      case 2:
        ret = "2 (Stereo)";
        break;

      default:
        ret = channels + " (" + MediaFileHelper.audioChannelInDotNotation(channels) + ")";
        break;
    }
    return ret;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.channels"));
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }
}
