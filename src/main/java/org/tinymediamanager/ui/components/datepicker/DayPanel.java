/*
 * Copyright 2012 - 2025 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.components.datepicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * The class DayPanel is used to display a panel for day choosing
 * 
 * @author Manuel Laggner
 */
class DayPanel extends JPanel {
  private final Calendar today;
  private final JLabel[] dayNames;
  private final JLabel[] days;
  private final Color    transparentBackgroundColor;
  private final Color    selectedColor;
  private final Color    sundayForeground;
  private final Color    weekdayForeground;

  private int            day;
  private Calendar       calendar;
  private Locale         locale;

  private JLabel         selectedDay;

  DayPanel() {
    setBackground(Color.blue);

    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        JLabel label = (JLabel) e.getSource();
        String buttonText = label.getText();
        setDay(Integer.parseInt(buttonText));
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    };

    locale = Locale.getDefault();
    dayNames = new JLabel[7];
    days = new JLabel[42];
    selectedDay = null;
    calendar = Calendar.getInstance(locale);
    today = (Calendar) calendar.clone();

    setLayout(new BorderLayout());

    JPanel dayPanel = new JPanel();
    dayPanel.setLayout(new GridLayout(7, 7));

    sundayForeground = new Color(164, 0, 0);
    weekdayForeground = UIManager.getColor("Component.linkColor");
    Color decorationBackgroundColor = UIManager.getColor("DatePicker.headerBackground");
    selectedColor = UIManager.getColor("Component.focusColor");
    transparentBackgroundColor = new Color(255, 255, 255, 0);

    for (int i = 0; i < 7; i++) {
      dayNames[i] = new JLabel("");
      dayNames[i].setHorizontalAlignment(SwingConstants.CENTER);
      dayNames[i].setOpaque(true);
      dayNames[i].setBackground(decorationBackgroundColor);
      dayNames[i].setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
      dayPanel.add(dayNames[i]);
    }

    for (int row = 0; row < 6; row++) {
      for (int column = 0; column < 7; column++) {
        int index = column + (7 * row);

        days[index] = new JLabel("");
        days[index].setHorizontalAlignment(SwingConstants.CENTER);
        days[index].addMouseListener(mouseAdapter);
        days[index].setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        dayPanel.add(days[index]);
      }
    }

    init();

    setDay(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    add(dayPanel, BorderLayout.CENTER);

    updateUI();
  }

  /**
   * Initializes the locale specific names for the days of the week.
   */
  protected void init() {
    Date date = calendar.getTime();
    calendar = Calendar.getInstance(locale);
    calendar.setTime(date);

    drawDayNames();
    drawDays();
  }

  /**
   * Draws the day names of the day columns
   */
  private void drawDayNames() {
    int firstDayOfWeek = calendar.getFirstDayOfWeek();
    DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);
    String[] dayNames = dateFormatSymbols.getShortWeekdays();

    int day = firstDayOfWeek;

    for (int i = 0; i < 7; i++) {
      this.dayNames[i].setText(dayNames[day]);

      if (day == 1) {
        this.dayNames[i].setForeground(sundayForeground);
      }
      else {
        this.dayNames[i].setForeground(weekdayForeground);
      }

      if (day < 7) {
        day++;
      }
      else {
        day -= 6;
      }
    }
  }

  /**
   * Draws the day buttons
   */
  private void drawDays() {
    Calendar tmpCalendar = (Calendar) calendar.clone();
    tmpCalendar.set(Calendar.HOUR_OF_DAY, 0);
    tmpCalendar.set(Calendar.MINUTE, 0);
    tmpCalendar.set(Calendar.SECOND, 0);
    tmpCalendar.set(Calendar.MILLISECOND, 0);

    int firstDayOfWeek = tmpCalendar.getFirstDayOfWeek();
    tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);

    int firstDay = tmpCalendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek;

    if (firstDay < 0) {
      firstDay += 7;
    }

    // draw last days of previous month
    tmpCalendar.add(Calendar.MONTH, -1);
    int lastDayOfPreviousMonth = tmpCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    tmpCalendar.add(Calendar.MONTH, 1);

    int i;
    for (i = 0; i < firstDay; i++) {
      days[i].setEnabled(false);
      days[i].setText(Integer.toString(lastDayOfPreviousMonth - firstDay + i + 1));
      days[i].setVisible(true);
    }

    tmpCalendar.add(Calendar.MONTH, 1);
    Date firstDayInNextMonth = tmpCalendar.getTime();
    tmpCalendar.add(Calendar.MONTH, -1);

    Date day = tmpCalendar.getTime();
    int n = 0;
    Color foregroundColor = getForeground();

    while (day.before(firstDayInNextMonth)) {
      days[i + n].setText(Integer.toString(n + 1));
      days[i + n].setVisible(true);

      if ((tmpCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
          && (tmpCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR))) {
        days[i + n].setForeground(sundayForeground);
      }
      else if ((n + 1) == this.day) {
        days[i + n].setForeground(selectedColor);
        selectedDay = days[i + n];
      }
      else {
        days[i + n].setForeground(foregroundColor);
      }

      days[i + n].setEnabled(true);

      n++;
      tmpCalendar.add(Calendar.DATE, 1);
      day = tmpCalendar.getTime();
    }

    // fill up the last row with the days from the next month
    int actualDays = n;
    while ((n + i) % 7 != 0) {
      days[i + n].setText(Integer.toString(n + 1 - actualDays));
      days[i + n].setEnabled(false);
      days[i + n].setVisible(true);
      n++;
    }

    // and hide the last line if it has not been started
    for (int k = n + i; k < 42; k++) {
      days[k].setVisible(false);
      days[k].setText("");
    }
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public void setLocale(Locale locale) {
    this.locale = locale;
    super.setLocale(locale);
    init();
  }

  /**
   * Set the selected day
   * 
   * @param newDay
   *          the day to select
   */
  public void setDay(int newDay) {
    if (newDay < 1) {
      newDay = 1;
    }
    Calendar tmpCalendar = (Calendar) calendar.clone();
    tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
    tmpCalendar.add(Calendar.MONTH, 1);
    tmpCalendar.add(Calendar.DATE, -1);

    int maxDaysInMonth = tmpCalendar.get(Calendar.DATE);

    if (newDay > maxDaysInMonth) {
      newDay = maxDaysInMonth;
    }

    day = newDay;

    if (selectedDay != null) {
      selectedDay.setBackground(transparentBackgroundColor);
      selectedDay.repaint();
    }

    for (int i = 0; i < 42; i++) {
      if (days[i].getText().equals(Integer.toString(day))) {
        selectedDay = days[i];
        selectedDay.setForeground(selectedColor);
        break;
      }
    }

    firePropertyChange("day", 0, day);
  }

  /**
   * Returns the selected day.
   *
   * @return the day value
   */
  public int getDay() {
    return day;
  }

  /**
   * Sets a specific month. This is needed for correct graphical representation of the days.
   *
   * @param month
   *          the new month
   */
  void setMonth(int month) {
    calendar.set(Calendar.MONTH, month);
    int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

    if (day > maxDays) {
      day = maxDays;
    }

    drawDays();
  }

  /**
   * Sets a specific year. This is needed for correct graphical representation of the days.
   *
   * @param year
   *          the new year
   */
  public void setYear(int year) {
    calendar.set(Calendar.YEAR, year);
    drawDays();
  }

  /**
   * Sets a specific calendar. This is needed for correct graphical representation of the days.
   *
   * @param calendar
   *          the new calendar
   */
  public void setCalendar(Calendar calendar) {
    this.calendar = calendar;
    drawDays();
  }
}
