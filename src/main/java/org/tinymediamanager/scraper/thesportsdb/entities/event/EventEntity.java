package org.tinymediamanager.scraper.thesportsdb.entities.event;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EventEntity {

    @SerializedName("events")
    List<Event> events;

}
