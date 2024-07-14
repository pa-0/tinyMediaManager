package org.tinymediamanager.scraper.thesportsdb.entities.player;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlayerEntity {

    @SerializedName("players")
    List<Player> players;

}
