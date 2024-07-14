package org.tinymediamanager.scraper.thesportsdb.entities.season;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SeasonEntity {

    @SerializedName("seasons")
    public List<Season> seasons;

}
