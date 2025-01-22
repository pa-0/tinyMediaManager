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

package org.tinymediamanager.ui.components.table;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.nio.file.Path;
import java.text.Collator;
import java.text.DateFormat;
import java.text.RuleBasedCollator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;

import ca.odell.glazedlists.gui.AdvancedTableFormat;

/**
 * The abstract TmmTableFormat is a convenience wrapper for the @see com.glazedlists.AdvancedTableFormat
 *
 * @author Manuel Laggner
 */
public abstract class TmmTableFormat<E> implements AdvancedTableFormat<E> {
  protected List<Column> columns = new ArrayList<>();

  protected FontMetrics getFontMetrics() {
    Font defaultFont = UIManager.getFont("Table.font");
    if (defaultFont == null) {
      defaultFont = UIManager.getFont("Label.font");
    }
    Canvas canvas = new Canvas();
    return canvas.getFontMetrics(defaultFont);
  }

  protected int getCellPadding() {
    // fixed padding for the column width
    // this is calculated using the current font size rather than a fixed value
    return getFontMetrics().stringWidth("xx1");
  }

  protected void addColumn(Column column) {
    columns.add(column);
  }

  @Override
  public Class<?> getColumnClass(int i) {
    return columns.get(i).columnClass;
  }

  @Override
  public Comparator<?> getColumnComparator(int i) {
    return columns.get(i).columnComparator;
  }

  @Override
  public int getColumnCount() {
    return columns.size();
  }

  @Override
  public String getColumnName(int i) {
    return columns.get(i).columnTitle;
  }

  @Override
  public Object getColumnValue(E e, int i) {
    return columns.get(i).columnValue.apply(e);
  }

  public String getHeaderTooltip(int i) {
    Column column = columns.get(i);
    if (StringUtils.isNotBlank(column.headerTooltip)) {
      return column.headerTooltip;
    }
    return null;
  }

  public String getColumnTooltip(E e, int i) {
    Column column = columns.get(i);
    if (column.cellTooltip != null) {
      return column.cellTooltip.apply(e);
    }
    return null;
  }

  public String getColumnIdentifier(int i) {
    return columns.get(i).columnIdentifier;
  }

  public TableCellRenderer getCellRenderer(int i) {
    return columns.get(i).cellRenderer;
  }

  public ImageIcon getHeaderIcon(int i) {
    return columns.get(i).headerIcon;
  }

  public boolean getColumnResizeable(int i) {
    return columns.get(i).columnResizeable;
  }

  public int getMinWidth(int i) {
    return columns.get(i).minWidth;
  }

  public int getMaxWidth(int i) {
    return columns.get(i).maxWidth;
  }

  public boolean isColumnDefaultHidden(int i) {
    return columns.get(i).defaultHidden;
  }

  protected class Column {
    private final String         columnTitle;
    private final String         columnIdentifier;
    private final Function<E, ?> columnValue;
    private final Class<?>       columnClass;

    private String               headerTooltip    = null;
    private Function<E, String>  cellTooltip      = null;
    private Comparator<?>        columnComparator = null;
    private TableCellRenderer    cellRenderer     = null;
    private ImageIcon            headerIcon       = null;
    private boolean              columnResizeable = true;
    private int                  minWidth         = 0;
    private int                  maxWidth         = 0;
    private boolean              defaultHidden    = false;

    public Column(String title, String identifier, Function<E, ?> value, Class<?> clazz) {
      columnTitle = title;
      columnIdentifier = identifier;
      columnValue = value;
      columnClass = clazz;
      minWidth = (int) (Settings.getInstance().getFontSize() * 2.3);
    }

    public void setColumnComparator(Comparator<?> comparator) {
      columnComparator = comparator;
    }

    public void setCellRenderer(TableCellRenderer renderer) {
      cellRenderer = renderer;
    }

    public void setHeaderIcon(ImageIcon icon) {
      headerIcon = icon;
    }

    public void setColumnResizeable(boolean resizeable) {
      columnResizeable = resizeable;
    }

    public void setMinWidth(int minWidth) {
      this.minWidth = minWidth;
    }

    public void setMaxWidth(int maxWidth) {
      this.maxWidth = maxWidth;
    }

    public void setHeaderTooltip(String tooltip) {
      this.headerTooltip = tooltip;
    }

    public void setCellTooltip(Function<E, String> tooltipFunction) {
      this.cellTooltip = tooltipFunction;
    }

    public void setDefaultHidden(boolean defaultHidden) {
      this.defaultHidden = defaultHidden;
    }
  }

  protected ImageIcon getCheckIcon(boolean bool) {
    if (bool) {
      return IconManager.TABLE_OK;
    }
    return IconManager.TABLE_NOT_OK;
  }

  protected ImageIcon getNewIcon(boolean bool) {
    if (bool) {
      return IconManager.NEW_GREEN;
    }
    return null;
  }

  public static class StringComparator implements Comparator<String> {
    protected Collator stringCollator;

    public StringComparator() {
      RuleBasedCollator defaultCollator = (RuleBasedCollator) Collator.getInstance();
      try {
        // default collator ignores whitespaces
        // using hack from http://stackoverflow.com/questions/16567287/java-collation-ignores-space
        stringCollator = new RuleBasedCollator(defaultCollator.getRules().replace("<'\u005f'", "<' '<'\u005f'"));
      }
      catch (Exception e) {
        stringCollator = defaultCollator;
      }
    }

    @Override
    public int compare(String arg0, String arg1) {
      if (StringUtils.isAllBlank(arg0, arg1)) {
        return 0;
      }
      if (StringUtils.isBlank(arg0)) {
        return -1;
      }
      if (StringUtils.isBlank(arg1)) {
        return 1;
      }

      if (stringCollator != null) {
        String first = StrgUtils.normalizeString(arg0.toLowerCase(Locale.ROOT));
        String second = StrgUtils.normalizeString(arg1.toLowerCase(Locale.ROOT));
        return stringCollator.compare(first, second);
      }

      return arg0.toLowerCase(Locale.ROOT).compareTo(arg1.toLowerCase(Locale.ROOT));
    }
  }

  public static class BooleanComparator implements Comparator<Boolean> {
    @Override
    public int compare(Boolean arg0, Boolean arg1) {
      return Boolean.compare(arg0, arg1);
    }
  }

  public static class PathComparator implements Comparator<Path> {
    @Override
    public int compare(Path arg0, Path arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0.toAbsolutePath().compareTo(arg1.toAbsolutePath());
    }
  }

  public static class IntegerComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer arg0, Integer arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0 - arg1;
    }
  }

  public static class FloatComparator implements Comparator<Float> {
    @Override
    public int compare(Float arg0, Float arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0.compareTo(arg1);
    }
  }

  public static class ImageComparator implements Comparator<ImageIcon> {
    @Override
    public int compare(ImageIcon arg0, ImageIcon arg1) {
      return Integer.compare(getImageValue(arg0), getImageValue(arg1)) * -1; // reverse
    }

    private int getImageValue(ImageIcon imageIcon) {
      if (imageIcon == IconManager.TABLE_OK) {
        return 8;
      }
      else if (imageIcon == IconManager.TABLE_PROBLEM) {
        return 4;
      }
      else if (imageIcon == IconManager.TABLE_NOT_OK) {
        return 2;
      }
      else if (imageIcon != null) {
        return 1;
      }
      else {
        return -1;
      }
    }
  }

  public static class DateComparator implements Comparator<Date> {
    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    public int compare(Date arg0, Date arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }

      // compare date objects on the day only (not time)
      String date0 = dateFormat.format(arg0);
      String date1 = dateFormat.format(arg1);
      return date0.compareTo(date1);
    }
  }

  public static class DateTimeComparator implements Comparator<Date> {

    @Override
    public int compare(Date arg0, Date arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }

      return arg0.compareTo(arg1);
    }
  }

  public static class VideoFormatComparator implements Comparator<String> {
    @Override
    public int compare(String arg0, String arg1) {
      int value1;
      int value2;

      if (StringUtils.isBlank(arg0)) {
        value1 = -1;
      }
      else {
        value1 = MetadataUtil.parseInt(arg0.replace("p", ""), -1);
      }

      if (StringUtils.isBlank(arg1)) {
        value2 = -1;
      }
      else {
        value2 = MetadataUtil.parseInt(arg1.replace("p", ""), -1);
      }

      return Integer.compare(value1, value2);
    }
  }

  public static class FileSizeComparator implements Comparator<String> {
    Pattern pattern = Pattern.compile("(.*) ([kMGT]?)");

    @Override
    public int compare(String arg0, String arg1) {
      long size0 = parseSize(arg0);
      long size1 = parseSize(arg1);

      return Long.compare(size0, size1);
    }

    private long parseSize(String sizeAsString) {
      long size = 0;

      if (StringUtils.isBlank(sizeAsString)) {
        return size;
      }

      Matcher matcher = pattern.matcher(sizeAsString);
      if (matcher.find()) {
        try {
          float value = Float.parseFloat(matcher.group(1).replace(",", ".")); // fix DE format
          String unit = matcher.group(2);
          if ("T".equals(unit)) {
            size = (long) (value * 1000 * 1000 * 1000 * 1000);
          }
          else if ("G".equals(unit)) {
            size = (long) (value * 1000 * 1000 * 1000);
          }
          else if ("M".equals(unit)) {
            size = (long) (value * 1000 * 1000);
          }
          else if ("k".equals(unit)) {
            size = (long) (value * 1000);
          }
          else {
            size = (long) value;
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return size;
    }
  }

  public static class CertificationComparator implements Comparator<MediaCertification> {
    @Override
    public int compare(MediaCertification arg0, MediaCertification arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0.toString().compareTo(arg1.toString());
    }
  }
}
