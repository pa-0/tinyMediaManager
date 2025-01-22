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
package org.tinymediamanager.ui.movies.filters;

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
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a audio channel movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieAudioChannelFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieAudioChannelFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.substring(0, 1).equals(s2.substring(0, 1))); // first char is channel
    buildAudioChannelArray();
    movieList.addPropertyChangeListener(Constants.AUDIO_CHANNEL, evt -> SwingUtilities.invokeLater(this::buildAudioChannelArray));
  }

  @Override
  public String getId() {
    return "movieAudioChannel";
  }

  @Override
  public boolean accept(Movie movie) {
    List<String> audioChannels = new ArrayList<String>();
    for (String values : checkComboBox.getSelectedItems()) {
      audioChannels.add(values.substring(0, 1)); // MI does not return more than 8 channels, so 16 is no issue ;)
    }

    // check all audio channels of all VIDEO and AUDIO files
    List<MediaFile> mediaFiles = movie.getMediaFiles(VIDEO, AUDIO);
    for (MediaFile mf : mediaFiles) {
      for (String channels : mf.getAudioChannelsList()) {
        if (audioChannels.contains(channels.substring(0, 1))) {
          return true;
        }
      }
    }

    return false;
  }

  private void buildAudioChannelArray() {
    List<String> audioChannel = new ArrayList<>();
    for (int channel : movieList.getAudioChannelsInMovies()) {
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
