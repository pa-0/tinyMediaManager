package org.tinymediamanager.scraper.thesportsdb.entities.tv_highlights;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TvHighlights {

    @SerializedName("idEvent")
    @Expose
    public String idEvent;
    @SerializedName("strEvent")
    @Expose
    public String strEvent;
    @SerializedName("strSport")
    @Expose
    public String strSport;
    @SerializedName("idLeague")
    @Expose
    public String idLeague;
    @SerializedName("strLeague")
    @Expose
    public String strLeague;
    @SerializedName("dateEvent")
    @Expose
    public String dateEvent;
    @SerializedName("strVideo")
    @Expose
    public String strVideo;
    @SerializedName("strPoster")
    @Expose
    public String strPoster;
    @SerializedName("strThumb")
    @Expose
    public String strThumb;
    @SerializedName("strFanart")
    @Expose
    public Object strFanart;
    @SerializedName("strSeason")
    @Expose
    public String strSeason;

}
