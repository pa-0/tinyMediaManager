package org.tinymediamanager.scraper.thesportsdb.entities.tv_highlights;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TvHighlightEntity {

    @SerializedName("tvhighlights")
    public List<TvHighlights> tvHighlights;

}
