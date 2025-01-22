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

package org.tinymediamanager.core.tvshow.connector;

import java.util.List;
import java.util.Locale;

import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.w3c.dom.Element;

/**
 * the class {@link TvShowEpisodeToKodiConnector} is used to write a most recent Kodi compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeToKodiConnector extends TvShowEpisodeGenericXmlConnector {

  public TvShowEpisodeToKodiConnector(List<TvShowEpisode> episodes) {
    super(episodes);
  }

  /**
   * write the new rating style<br />
   * <ratings> <rating name="default" max="10" default="true"> <value>5.800000</value> <votes>2100</votes> </rating> <rating name="imdb">
   * <value>8.9</value> <votes>12345</votes> </rating> </ratings>
   */
  @Override
  protected void addRating(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element ratings = document.createElement("ratings");

    for (MediaRating r : episode.getRatings().values()) {
      // skip user ratings here
      if (MediaRating.USER.equals(r.getId())) {
        continue;
      }

      Element rating = document.createElement("rating");
      // Kodi needs themoviedb instead of tmdb
      if (MediaMetadata.TMDB.equals(r.getId())) {
        rating.setAttribute("name", "themoviedb");
      }
      else {
        rating.setAttribute("name", r.getId());
      }
      rating.setAttribute("max", String.valueOf(r.getMaxValue()));

      MediaRating mainMediaRating = episode.getRating();
      rating.setAttribute("default", r == mainMediaRating ? "true" : "false");

      Element value = document.createElement("value");
      value.setTextContent(String.format(Locale.US, "%.1f", r.getRating()));
      rating.appendChild(value);

      Element votes = document.createElement("votes");
      votes.setTextContent(Integer.toString(r.getVotes()));
      rating.appendChild(votes);

      ratings.appendChild(rating);
    }

    root.appendChild(ratings);
  }

  /**
   * votes are now in the ratings tag
   */
  @Override
  protected void addVotes(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
  }

  @Override
  protected void addOwnTags(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    addEpbookmark(episode, parser);
    addCode(episode, parser);
    addFileinfo(episode, parser);
  }

  /**
   * add the <epbookmark>xxx</epbookmark>
   */
  protected void addEpbookmark(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element epbookmark = document.createElement("epbookmark");
    if (parser != null) {
      epbookmark.setTextContent(parser.epbookmark);
    }
    root.appendChild(epbookmark);
  }

  /**
   * add the <code>xxx</code>
   */
  protected void addCode(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element code = document.createElement("code");
    if (parser != null) {
      code.setTextContent(parser.code);
    }
    root.appendChild(code);
  }

  /**
   * add the fileinfo structure <fileinfo><streamdetails><video>...</video><audio>...</audio></streamdetails></fileinfo>
   */
  protected void addFileinfo(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    if (TvShowModuleManager.getInstance().getSettings().isNfoWriteFileinfo()) {
      Element fileinfo = document.createElement("fileinfo");
      Element streamdetails = document.createElement("streamdetails");

      List<MediaFile> videos = episode.getMediaFiles(MediaFileType.VIDEO);
      if (!videos.isEmpty()) {
        MediaFile videoFile = videos.get(0);
        Element video = document.createElement("video");

        Element codec = document.createElement("codec");
        // workaround for h265/hevc since Kodi just "knows" hevc
        // https://forum.kodi.tv/showthread.php?tid=354886&pid=2955329#pid2955329
        if ("h265".equalsIgnoreCase(videoFile.getVideoCodec())) {
          codec.setTextContent("HEVC");
        }
        else {
          codec.setTextContent(videoFile.getVideoCodec());
        }
        video.appendChild(codec);

        Element aspect = document.createElement("aspect");
        aspect.setTextContent(String.valueOf(videoFile.getAspectRatio()));
        video.appendChild(aspect);

        Element width = document.createElement("width");
        width.setTextContent(String.valueOf(videoFile.getVideoWidth()));
        video.appendChild(width);

        Element height = document.createElement("height");
        height.setTextContent(String.valueOf(videoFile.getVideoHeight()));
        video.appendChild(height);

        if (episode.isVideoInHDR()) {
          // basically a TMM string to Kodi skin mapping, but only one
          Element hdr = document.createElement("hdrtype");
          if (videoFile.getHdrFormat().contains("Dolby Vision")) {
            hdr.setTextContent("dolbyvision");
          }
          else if (videoFile.getHdrFormat().contains("HLG")) {
            hdr.setTextContent("hlg");
          }
          else if (videoFile.getHdrFormat().contains("HDR10")) {
            hdr.setTextContent("hdr10");
          }
          video.appendChild(hdr);
        }

        // does not work reliable for disc style movies, MediaInfo and even Kodi write weird values in there
        if (!episode.isDisc() && !episode.getMainVideoFile().getExtension().equalsIgnoreCase("iso")) {
          Element durationinseconds = document.createElement("durationinseconds");
          durationinseconds.setTextContent(String.valueOf(episode.getRuntimeFromMediaFiles()));
          video.appendChild(durationinseconds);
        }

        Element stereomode = document.createElement("stereomode");
        // "Spec": https://github.com/xbmc/xbmc/blob/master/xbmc/guilib/StereoscopicsManager.cpp
        switch (videoFile.getVideo3DFormat()) {
          case MediaFileHelper.VIDEO_3D_SBS:
          case MediaFileHelper.VIDEO_3D_HSBS:
            stereomode.setTextContent("left_right");
            break;

          case MediaFileHelper.VIDEO_3D_TAB:
          case MediaFileHelper.VIDEO_3D_HTAB:
            stereomode.setTextContent("top_bottom");
            break;

          default:
            break;
        }
        video.appendChild(stereomode);
        streamdetails.appendChild(video);

        for (MediaFileAudioStream as : videoFile.getAudioStreams()) {
          Element audio = document.createElement("audio");

          Element audioCodec = document.createElement("codec");
          audioCodec.setTextContent(as.getCodec().replaceAll("-", "_"));
          audio.appendChild(audioCodec);

          Element language = document.createElement("language");
          language.setTextContent(as.getLanguage());
          audio.appendChild(language);

          Element channels = document.createElement("channels");
          channels.setTextContent(String.valueOf(as.getAudioChannels()));
          audio.appendChild(channels);

          streamdetails.appendChild(audio);
        }

        for (MediaFileSubtitle ss : videoFile.getSubtitles()) {
          Element subtitle = document.createElement("subtitle");

          Element language = document.createElement("language");
          language.setTextContent(ss.getLanguage());
          subtitle.appendChild(language);

          streamdetails.appendChild(subtitle);
        }

        for (MediaFile sub : episode.getMediaFiles(MediaFileType.SUBTITLE)) {
          for (MediaFileSubtitle ss : sub.getSubtitles()) {
            Element subtitle = document.createElement("subtitle");

            Element language = document.createElement("language");
            language.setTextContent(ss.getLanguage());
            subtitle.appendChild(language);

            streamdetails.appendChild(subtitle);
          }
        }
      }

      fileinfo.appendChild(streamdetails);
      root.appendChild(fileinfo);
    }
  }
}
