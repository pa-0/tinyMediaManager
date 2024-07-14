package org.tinymediamanager.scraper.thesportsdb;

import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;

import java.util.ArrayList;
import java.util.List;

public class SportsDbTvShowArtworkProvider extends SportsDbMetadataProvider implements ITvShowArtworkProvider {
    @Override
    public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {

        List<MediaArtwork> mediaArtworkList = new ArrayList<>();
        MediaMetadata md = options.getMetadata();

        return List.of();
    }

    @Override
    protected String getSubId() {
        return "tvshow_artwork";
    }
}
