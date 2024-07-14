package org.tinymediamanager.scraper.thesportsdb.entities.player;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Player {

    @SerializedName("idPlayer")
    @Expose
    public String idPlayer;
    @SerializedName("strPlayer")
    @Expose
    public String strPlayer;
    @SerializedName("idTeam")
    @Expose
    public String idTeam;
    @SerializedName("strTeam")
    @Expose
    public String strTeam;
    @SerializedName("strThumb")
    @Expose
    public String strThumb;
    @SerializedName("strCutout")
    @Expose
    public String strCutout;
    @SerializedName("strRender")
    @Expose
    public String strRender;
    @SerializedName("dateBorn")
    @Expose
    public String dateBorn;
    @SerializedName("strPosition")
    @Expose
    public String strPosition;

}
