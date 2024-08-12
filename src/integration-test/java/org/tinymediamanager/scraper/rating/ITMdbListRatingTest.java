package org.tinymediamanager.scraper.rating;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaType;

public class ITMdbListRatingTest {

  @Test
  public void testMovieRatings() throws Exception {
    Map<String, Object> ids = new HashMap<>();
    ids.put(MediaMetadata.IMDB, "tt6718170");
    MdbListRating ratings = new MdbListRating();
    List<MediaRating> mediaRatings = ratings.getRatings(MediaType.MOVIE, ids); // Super Mario Bros Movie
    assertThat(mediaRatings).isNotEmpty();
  }

  @Test
  public void testShowRatings() throws Exception {
    Map<String, Object> ids = new HashMap<>();
    ids.put(MediaMetadata.TMDB, 8475);
    MdbListRating ratings = new MdbListRating();
    List<MediaRating> mediaRatings = ratings.getRatings(MediaType.TV_SHOW, ids);
    assertThat(mediaRatings).isNotEmpty();

    // since "episode" is not a valid type on their side, it would fallback to "movie"... and scrape something else ;)
    ids.put(MediaMetadata.TMDB, 396535);
    mediaRatings = ratings.getRatings(MediaType.TV_EPISODE, ids);
    assertThat(mediaRatings).isEmpty();
  }
}
