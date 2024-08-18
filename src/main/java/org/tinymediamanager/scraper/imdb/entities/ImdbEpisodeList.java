package org.tinymediamanager.scraper.imdb.entities;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroupType;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbEpisodeList {
  public String                  currentSeason        = "";
  public String                  currentYear          = "";
  public ImdbEpisodeListEpisodes episodes             = null;
  public List<ImdbIdValueType>   seasons              = null;
  public List<ImdbIdValueType>   years                = null;

  @JsonIgnore
  private Map<String, Object>    additionalProperties = new HashMap<>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public List<MediaMetadata> getEpisodes() {
    List<MediaMetadata> eps = new ArrayList<>();

    if (episodes != null) {
      for (ImdbEpisodeListEpisode ep : ListUtils.nullSafe(episodes.items)) {
        MediaMetadata md = new MediaMetadata(MediaMetadata.IMDB);
        md.setTitle(ep.titleText);
        md.setPlot(ep.plot);
        md.setId(MediaMetadata.IMDB, ep.id);

        MediaEpisodeGroup eg = new MediaEpisodeGroup(EpisodeGroupType.AIRED);
        int s = MetadataUtil.parseInt(ep.season, -1);
        int e = MetadataUtil.parseInt(ep.episode, -1);
        if (e < 0 || s < 0) {
          continue;
        }
        md.setEpisodeNumber(new MediaEpisodeNumber(eg, s, e));

        if (ep.image != null) {
          MediaArtwork img = new MediaArtwork(MediaMetadata.IMDB, THUMB);
          img.setOriginalUrl(ep.image.url);
          img.setSeason(s);
          img.addImageSize(ep.image.getWidth(), ep.image.getHeight(), ep.image.url, MediaArtwork.ThumbSizes.getSizeOrder(ep.image.getWidth()));
          md.addMediaArt(img);
        }

        if (ep.aggregateRating > 0.0) {
          MediaRating rat = new MediaRating(MediaMetadata.IMDB);
          rat.setMaxValue(10);
          rat.setRating(ep.aggregateRating);
          rat.setVotes(ep.voteCount);
          md.addRating(rat);
        }

        md.setYear(ep.releaseYear);
        if (ep.releaseDate != null) {
          md.setReleaseDate(ep.releaseDate.toDate());
        }

        eps.add(md);
      }
    }
    return eps;
  }
}
