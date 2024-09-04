package org.tinymediamanager.scraper.imdb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;

/**
 * Utils class, to handle all kinds of IMDB datasets (download / extract / amp2db...)
 * 
 * @author Myron Boyle
 *
 */
public class ImdbDatasetUtils {

  private static final Logger                        LOGGER                        = LoggerFactory.getLogger(ImdbDatasetUtils.class);

  static final String                                IMDB_DATASET_NAME_BASICS      = "name.basics.tsv";
  static final String                                IMDB_DATASET_TITLE_AKAS       = "title.akas.tsv";
  static final String                                IMDB_DATASET_TITLE_BASICS     = "title.basics.tsv";
  static final String                                IMDB_DATASET_TITLE_CREW       = "title.crew.tsv";
  static final String                                IMDB_DATASET_TITLE_EPISODE    = "title.episode.tsv";
  static final String                                IMDB_DATASET_TITLE_PRINCIPALS = "title.principals.tsv";
  static final String                                IMDB_DATASET_TITLE_RATINGS    = "title.ratings.tsv";

  private static MVStore                             mvStore;
  // map inside DB
  private static MVMap<String, List<DatasetEpisode>> dbEpisodeMap;
  // java map for adding/updating list of episodes per showId key
  // doing that directly inside M2 map is waaay to slow...
  private static Map<String, List<DatasetEpisode>>   javaEpisodeMap;

  static class DatasetEpisode implements Serializable {
    private static final long serialVersionUID = 1L;
    String                    id;
    int                       s                = -1;
    int                       e                = -1;

    public DatasetEpisode(String episodeId, String seasonNr, String episodeNr) {
      id = episodeId;
      try {
        s = Integer.parseInt(seasonNr); // do not use our utils method - too slow
      }
      catch (NumberFormatException e) {
      }
      try {
        e = Integer.parseInt(episodeNr);
      }
      catch (NumberFormatException e) {
      }
    }
  }

  private static synchronized void initMap() {
    if (mvStore == null) {
      initEpisodeDataset();
    }
  }

  private static boolean downloadDataset(String dataset) throws MalformedURLException {
    LOGGER.debug("Downloading dataset {}", dataset);
    Path downloadedFile = Paths.get(Globals.CACHE_FOLDER, dataset + ".gz");
    Utils.deleteFileSafely(downloadedFile);
    Url imdbUrl = new OnDiskCachedUrl("https://datasets.imdbws.com/" + dataset + ".gz", 90, TimeUnit.DAYS);
    return imdbUrl.download(downloadedFile);
  }

  private static Path unpackDataset(String dataset) throws FileNotFoundException, IOException {
    LOGGER.debug("Unpacking dataset {}", dataset);
    Path downloadedFile = Paths.get(Globals.CACHE_FOLDER, dataset + ".gz");
    Path extractedFile = Paths.get(Globals.CACHE_FOLDER, dataset);
    Utils.deleteFileSafely(extractedFile);
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(downloadedFile.toFile()))) {
      Files.copy(gis, extractedFile);
    }
    Utils.deleteFileSafely(downloadedFile);
    return extractedFile;
  }

  /**
   * Returns a List of all known IMDB episode Ids.<br>
   * Might be outdated
   * 
   * @param showId
   * @return empty list or list of episodeIds - but never NULL
   */
  public List<MediaMetadata> getEpisodesByShowId(String showId) {
    initMap();
    try {
      List<DatasetEpisode> eps = dbEpisodeMap.get(showId);
      if (eps == null) {
        return Collections.emptyList();
      }
      List<MediaMetadata> ret = new ArrayList<>();
      eps.forEach(data -> {
        MediaMetadata md = new MediaMetadata(MediaMetadata.IMDB);
        md.setId(MediaMetadata.IMDB, data.id);
        MediaEpisodeNumber see = new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, data.s, data.e);
        md.setEpisodeNumber(see);
        md.setTitle("found S" + see.season() + " E" + see.episode() + " - title will be scraped later");
        ret.add(md);
      });
      return ret;
    }
    catch (Exception e) {
      LOGGER.warn("could not read the MVstore - '{}'", e.getMessage());
      shutdown();
      Utils.deleteFileSafely(Paths.get(Globals.CACHE_FOLDER, IMDB_DATASET_TITLE_EPISODE + ".db"));
    }
    return Collections.emptyList();
  }

  private static void initEpisodeDataset() {
    Path databaseFile = Paths.get(Globals.CACHE_FOLDER, IMDB_DATASET_TITLE_EPISODE + ".db");
    long cut = LocalDateTime.now().minusMonths(3).toEpochSecond(ZoneOffset.UTC);

    try {
      if (!Files.exists(databaseFile) || Files.getLastModifiedTime(databaseFile).to(TimeUnit.SECONDS) < cut) {
        LOGGER.info("Preparing IMDB episode dataset...");

        boolean ok = downloadDataset(IMDB_DATASET_TITLE_EPISODE);
        if (ok) {
          Path extractedFile = unpackDataset(IMDB_DATASET_TITLE_EPISODE);

          LOGGER.debug("Parsing dataset..."); // ~10 sec
          javaEpisodeMap = new TreeMap<String, List<DatasetEpisode>>(); // HashMap slooow
          try (var stream = Files.lines(extractedFile, StandardCharsets.UTF_8)) {
            stream.skip(1).forEach(line -> {
              // EP, show, S, E
              String[] data = line.split("\t");
              javaEpisodeMap.computeIfAbsent(data[1], k -> new ArrayList<DatasetEpisode>()).add(new DatasetEpisode(data[0], data[2], data[3]));
            });
          }
          catch (Exception e) {
            LOGGER.error("Error writing archive {}", e.getMessage());
          }

          // copy JavaMap to H2MVMap
          // Sorted treemap is waaay faster, since H2 is also using b-trees internally :)
          // as HASHMAP: ~60sec/270mb compressHigh / ~30sec/470mb compress / ~25sec/1600mb w/o compress)
          // as TREEMAP: ~8sec/43mb compressHigh / ~3sec/76mb compress / ~xxsec/250mb w/o compress)
          LOGGER.debug("Copying map to database...");
          mvStore = new MVStore.Builder().fileName(databaseFile.toString()).compress().autoCommitDisabled().open();
          dbEpisodeMap = mvStore.openMap("imdbEpisodes");
          dbEpisodeMap.putAll(javaEpisodeMap);
          mvStore.commit();
          javaEpisodeMap.clear();
          javaEpisodeMap = null;

          LOGGER.debug("Done!");
        }
        else {
          if (Files.exists(databaseFile)) {
            LOGGER.error("download failed - using old copy");
            mvStore = new MVStore.Builder().fileName(databaseFile.toString()).readOnly().open();
            dbEpisodeMap = mvStore.openMap("imdbEpisodes");
          }
        }
      }
      else {
        LOGGER.debug("Dataset young enough, no need to download...");
        // just open it!
        mvStore = new MVStore.Builder().fileName(databaseFile.toString()).readOnly().open();
        dbEpisodeMap = mvStore.openMap("imdbEpisodes");
      }
    }
    catch (Exception e) {
      LOGGER.error("Error initializing dataset: IMDB_DATASET_TITLE_EPISODE {}", e.getMessage());
      Utils.deleteFileSafely(databaseFile);
    }
  }

  private static synchronized void shutdown() {
    try {
      if (mvStore != null && !mvStore.isClosed()) {
        mvStore.compactMoveChunks();
        mvStore.close();
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not close MVstore - deleting the cache");
    }
    finally {
      mvStore = null;
      dbEpisodeMap = null;
      javaEpisodeMap = null;
    }
  }
}
