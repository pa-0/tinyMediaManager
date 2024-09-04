package org.tinymediamanager.scraper.imdb.entities;

import java.util.Date;
import java.util.GregorianCalendar;

public class ImdbReleaseDate extends ImdbAll {
  public int day;
  public int month;
  public int year;

  /**
   * @return Date or NULL
   */
  public Date toDate() {
    try {
      Date date = new GregorianCalendar(year, month - 1, day).getTime();
      return date;
    }
    catch (Exception e) {
      return null;
    }
  }
}
