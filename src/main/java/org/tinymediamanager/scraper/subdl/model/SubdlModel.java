package org.tinymediamanager.scraper.subdl.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubdlModel {

    @SerializedName("status")
    public boolean status;

    @SerializedName("results")
    public List<Results> results;

    @SerializedName("subtitles")
    public List<Subtitles> subtitles;

}
