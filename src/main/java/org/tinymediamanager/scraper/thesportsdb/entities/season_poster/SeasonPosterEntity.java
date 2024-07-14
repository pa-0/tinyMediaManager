package org.tinymediamanager.scraper.thesportsdb.entities.season_poster;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SeasonPosterEntity {

    @SerializedName("seasonposters")
    List<SeasonPoster> seasonPosters;

}
