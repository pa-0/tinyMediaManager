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
package org.tinymediamanager.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.ui.components.toolbar.ToolbarMenu;
import org.tinymediamanager.ui.images.TmmAwesomeIcon;
import org.tinymediamanager.ui.images.TmmSvgIcon;
import org.tinymediamanager.ui.images.TmmTextIcon;

import com.kitfox.svg.app.beans.SVGIcon;

public class IconManager {
  public static final ImageIcon              EMPTY_IMAGE                  = new ImageIcon(IconManager.class.getResource("plaf/empty.png"));
  private static final Set<TmmSvgIconCache>  SVG_CACHE                    = new HashSet<>();
  private static final Set<TmmTextIconCache> TEXT_ICON_CACHE              = new HashSet<>();
  private static final Map<URI, ImageIcon>   ICON_CACHE                   = new HashMap<>();

  // toolbar icons
  public static final ImageIcon              TOOLBAR_ABOUT                = createSVGIcon("icn_about.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_ABOUT_HOVER          = createSVGIcon("icn_about_hover.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_ADD_MOVIE_SET        = createSVGIcon("icn_add_collection.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_ADD_MOVIE_SET_HOVER  = createSVGIcon("icn_add_collection_hover.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_ALERT                = createSVGIcon("icn_alert.svg", new Dimension(17, 19), true);
  public static final ImageIcon              TOOLBAR_ALERT_RED            = createSVGIcon("icn_alert_active.svg", new Dimension(17, 19), Color.RED,
      true);
  public static final ImageIcon              TOOLBAR_DOWNLOAD             = createSVGIcon("icn_download.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_EDIT                 = createSVGIcon("icn_edit.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_EDIT_HOVER           = createSVGIcon("icn_edit_hover.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_LOGO                 = createSVGIcon("tmm_logo.svg", new Dimension(100, 45));
  public static final ImageIcon              TOOLBAR_REFRESH              = createSVGIcon("icn_refresh.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_REFRESH_HOVER        = createSVGIcon("icn_refresh_hover.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_RENAME               = createSVGIcon("icn_rename.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_RENAME_HOVER         = createSVGIcon("icn_rename_hover.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_SEARCH               = createSVGIcon("icn_search.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_SEARCH_HOVER         = createSVGIcon("icn_search_hover.svg", new Dimension(36, 36));
  public static final ImageIcon              TOOLBAR_SETTINGS             = createSVGIcon("icn_settings.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_SETTINGS_HOVER       = createSVGIcon("icn_settings_hover.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_TOOLS                = createSVGIcon("icn_tools.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_TOOLS_HOVER          = createSVGIcon("icn_tools_hover.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_UPGRADE              = createSVGIcon("icn_upgrade.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_RENEW                = createSVGIcon("icn_renew.svg", new Dimension(24, 24));
  public static final ImageIcon              TOOLBAR_MENU_INDICATOR       = createSVGIcon("caret-down-solid.svg", ToolbarMenu.COLOR);
  public static final ImageIcon              TOOLBAR_MENU_INDICATOR_HOVER = createSVGIcon("caret-down-solid.svg", ToolbarMenu.COLOR_HOVER);

  public static final ImageIcon              MENU_MOVIES                  = createSVGIcon("icn_movies.svg", new Dimension(26, 26));
  public static final ImageIcon              MENU_MOVIES_ACTIVE           = createSVGIcon("icn_movies_active.svg", new Dimension(26, 26));
  public static final ImageIcon              MENU_MOVIESETS               = createSVGIcon("icn_moviesets.svg", new Dimension(38, 28));
  public static final ImageIcon              MENU_MOVIESETS_ACTIVE        = createSVGIcon("icn_moviesets_active.svg", new Dimension(38, 28));
  public static final ImageIcon              MENU_TV_SHOWS                = createSVGIcon("icn_tvshows.svg", new Dimension(29, 21));
  public static final ImageIcon              MENU_TV_SHOWS_ACTIVE         = createSVGIcon("icn_tvshows_active.svg", new Dimension(29, 21));

  // rating icons
  public static final ImageIcon              STAR_FILLED                  = createSVGIcon("star-filled.svg", new Dimension(24, 24), true);
  public static final ImageIcon              STAR_EMPTY                   = createSVGIcon("star-empty.svg", new Dimension(24, 24), true);
  public static final ImageIcon              RATING_NEUTRAL               = createSVGIcon("rating.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_EMTPY                 = createSVGIcon("rating-empty.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_USER                  = createSVGIcon("rating-user.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_IMDB                  = createSVGIcon("rating-imdb.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_TMDB                  = createSVGIcon("rating-tmdb.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_TOMATOMETER           = createSVGIcon("rating-tomatometer.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_METACRITIC            = createSVGIcon("rating-metacritic.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_THETVDB               = createSVGIcon("rating-thetvdb.svg", new Dimension(53, 36), true);
  public static final ImageIcon              RATING_TRAKTTV               = createSVGIcon("rating-trakttv.svg", new Dimension(36, 36), true);
  public static final ImageIcon              RATING_LETTERBOXD            = createSVGIcon("rating-letterboxd.svg", new Dimension(53, 36), true);
  public static final ImageIcon              RATING_MAL                   = createSVGIcon("rating-myanimelist.svg", new Dimension(53, 36), true);
  public static final ImageIcon              RATING_ROGEREBERT            = createSVGIcon("rating-roger_ebert.svg", new Dimension(53, 36), true);

  // font awesome icons for actions in the popup menu
  public static final ImageIcon              ADD                          = createMenuIcon("plus.svg");
  public static final ImageIcon              APPLY                        = createMenuIcon("circle-check.svg");
  public static final ImageIcon              ASPECT_RATIO_BLUE            = createMenuIcon("ruler-combined-solid.svg");
  public static final ImageIcon              BARCODE                      = createMenuIcon("barcode.svg");
  public static final ImageIcon              BUG                          = createMenuIcon("bug.svg");
  public static final ImageIcon              CLEAN                        = createMenuIcon("broom.svg");
  public static final ImageIcon              CONNECT                      = createMenuIcon("link.svg");
  public static final ImageIcon              COPY                         = createMenuIcon("clone.svg");
  public static final ImageIcon              DELETE                       = createMenuIcon("xmark.svg");
  public static final ImageIcon              DELETE_FOREVER               = createMenuIcon("trash-alt.svg");
  public static final ImageIcon              DISCONNECT                   = createMenuIcon("unlink.svg");
  public static final ImageIcon              DOWNLOAD                     = createMenuIcon("download.svg");
  public static final ImageIcon              FEEDBACK                     = createMenuIcon("envelope.svg");
  public static final ImageIcon              EDIT                         = createMenuIcon("edit.svg");
  public static final ImageIcon              EXPORT                       = createMenuIcon("share-square.svg");
  public static final ImageIcon              FILTER                       = createMenuIcon("filter.svg");
  public static final ImageIcon              FOLDER_OPEN                  = createMenuIcon("folder-open.svg");
  public static final ImageIcon              HELP                         = createMenuIcon("circle-question.svg");
  public static final ImageIcon              HINT                         = createMenuIcon("circle-info.svg");
  public static final ImageIcon              IMAGE                        = createMenuIcon("image.svg");
  public static final ImageIcon              KODI                         = createMenuIcon("kodi.svg");
  public static final ImageIcon              LIST                         = createMenuIcon("list.svg");
  public static final ImageIcon              LOCK_BLUE                    = createMenuIcon("lock-alt.svg");
  public static final ImageIcon              MEDIAINFO                    = createMenuIcon("info.svg");
  public static final ImageIcon              MENU                         = createMenuIcon("bars.svg");
  public static final ImageIcon              MOVIE                        = createMenuIcon("film.svg");
  public static final ImageIcon              MUSIC                        = createMenuIcon("music.svg");
  public static final ImageIcon              NFO_BLUE                     = createMenuIcon("file-lines.svg");
  public static final ImageIcon              PLAY                         = createMenuIcon("play.svg");
  public static final ImageIcon              REFRESH                      = createMenuIcon("redo.svg");
  public static final ImageIcon              REMOVE                       = createMenuIcon("minus.svg");
  public static final ImageIcon              SEARCH                       = createMenuIcon("search.svg");
  public static final ImageIcon              RATING_BLUE                  = createMenuIcon("star.svg");
  public static final ImageIcon              SUBTITLE                     = createMenuIcon("comments.svg");
  public static final ImageIcon              SYNC                         = createMenuIcon("sync.svg");
  public static final ImageIcon              THUMB                        = createMenuIcon("photo-film.svg");
  public static final ImageIcon              UNDO                         = createMenuIcon("undo.svg");
  public static final ImageIcon              UNLOCK_BLUE                  = createMenuIcon("lock-open-alt.svg");
  public static final ImageIcon              VOLUME                       = createMenuIcon("volume.svg");
  public static final ImageIcon              WATCHED_MENU                 = createMenuIcon("play.svg");

  // font awesome icons for the table/tree
  public static final ImageIcon              TABLE_OK                     = createSVGIcon("check.svg", new Color(31, 187, 0));
  public static final ImageIcon              TABLE_PROBLEM                = createSVGIcon("triangle-exclamation.svg", new Color(204, 120, 50));
  public static final ImageIcon              TABLE_NOT_OK                 = createSVGIcon("xmark.svg", new Color(204, 2, 2));

  // font awesome icons normal
  public static final ImageIcon              CANCEL                       = createSVGIcon("circle-xmark.svg");
  public static final ImageIcon              CARET_UP                     = createSVGIcon("chevron-up.svg");
  public static final ImageIcon              CARET_DOWN                   = createSVGIcon("chevron-down.svg");
  public static final ImageIcon              CLEAR_GREY                   = createSVGIcon("circle-xmark.svg");
  public static final ImageIcon              COLLAPSED                    = createSVGIcon("square-chevron-down.svg");
  public static final ImageIcon              CONFIGURE                    = createSVGIcon("wrench.svg");
  public static final ImageIcon              DATE_PICKER                  = createSVGIcon("calendar-lines.svg");
  public static final ImageIcon              DELETE_GRAY                  = createSVGIcon("trash-alt.svg");
  public static final ImageIcon              ERROR                        = createSVGIcon("circle-xmark.svg", Color.RED);
  public static final ImageIcon              EXPANDED                     = createSVGIcon("square-chevron-right.svg");
  public static final ImageIcon              WARN                         = createSVGIcon("triangle-exclamation.svg");
  public static final ImageIcon              INFO                         = createSVGIcon("circle-info.svg");
  public static final ImageIcon              FILTER_ACTIVE                = createSVGIcon("lightbulb-on.svg", new Color(255, 119, 0));
  public static final ImageIcon              NEW_GREEN                    = createSVGIcon("circle-plus.svg", new Color(31, 187, 0));
  public static final ImageIcon              NEW_ORANGE                   = createSVGIcon("circle-plus.svg", new Color(204, 120, 50));
  public static final ImageIcon              PLAY_LARGE                   = createSVGIcon("play-circle.svg", 2.33333);
  public static final ImageIcon              SAVE                         = createSVGIcon("save.svg");
  public static final ImageIcon              SEARCH_GREY                  = createSVGIcon("search.svg");
  public static final ImageIcon              STOP                         = createSVGIcon("circle-stop.svg");
  public static final ImageIcon              UNDO_GREY                    = createSVGIcon("undo.svg");

  // font awesome icons light (button usage)
  public static final ImageIcon              ADD_INV                      = createButtonIcon("plus.svg");
  public static final ImageIcon              ARROW_UP_INV                 = createButtonIcon("chevron-up.svg");
  public static final ImageIcon              ARROW_DOWN_INV               = createButtonIcon("chevron-down.svg");
  public static final ImageIcon              APPLY_INV                    = createButtonIcon("circle-check.svg");
  public static final ImageIcon              BACK_INV                     = createButtonIcon("circle-chevron-left.svg");
  public static final ImageIcon              CANCEL_INV                   = createButtonIcon("circle-xmark.svg");
  public static final ImageIcon              CHECK_ALL                    = createButtonIcon("square-check.svg");
  public static final ImageIcon              CLEAR_ALL                    = createButtonIcon("square.svg");
  public static final ImageIcon              COPY_INV                     = createButtonIcon("clone.svg");
  public static final ImageIcon              DELETE_INV                   = createButtonIcon("trash-alt.svg");
  public static final ImageIcon              EXCHANGE                     = createButtonIcon("exchange.svg");
  public static final ImageIcon              FILE_ADD_INV                 = createButtonIcon("file-plus.svg");
  public static final ImageIcon              FILE_EXPORT_INV              = createButtonIcon("file-export.svg");
  public static final ImageIcon              FILE_IMPORT_INV              = createButtonIcon("file-import.svg");
  public static final ImageIcon              FILE_OPEN_INV                = createButtonIcon("folder-open.svg");
  public static final ImageIcon              IMAGE_INV                    = createButtonIcon("image.svg");
  public static final ImageIcon              PLAY_INV                     = createButtonIcon("play.svg");
  public static final ImageIcon              REMOVE_INV                   = createButtonIcon("minus.svg");
  public static final ImageIcon              SEARCH_INV                   = createButtonIcon("search.svg");
  public static final ImageIcon              STOP_INV                     = createButtonIcon("circle-stop.svg");

  // font awesome icons - column headers
  public static final ImageIcon              ABSOLUTE                     = createTextIcon("abs", 1.0);
  public static final ImageIcon              AUDIO                        = createTableHeaderIcon("volume.svg");
  public static final ImageIcon              ASPECT_RATIO                 = createTableHeaderIcon("ruler-combined-solid.svg");
  public static final ImageIcon              ASPECT_RATIO_2               = createTableHeaderIcon("ruler-combined-2-solid.svg");
  public static final ImageIcon              CERTIFICATION                = createTableHeaderIcon("universal-access.svg");
  public static final ImageIcon              COUNT                        = createTableHeaderIcon("hashtag.svg");
  public static final ImageIcon              DATE_ADDED                   = createTableHeaderIcon("calendar-plus.svg");
  public static final ImageIcon              DATE_AIRED                   = createTableHeaderIcon("calendar-lines.svg");
  public static final ImageIcon              DATE_CREATED                 = createTableHeaderIcon("calendar-star.svg");
  public static final ImageIcon              EDITION                      = createTableHeaderIcon("compact-disc.svg");
  public static final ImageIcon              EDIT_HEADER                  = createTableHeaderIcon("edit.svg");
  public static final ImageIcon              EPISODES                     = createTextIcon("E", 1.4);
  public static final ImageIcon              FILE_SIZE                    = createTableHeaderIcon("save.svg");
  public static final ImageIcon              HDR                          = createTableHeaderIcon("delicious-brands.svg");
  public static final ImageIcon              IMAGES                       = createTableHeaderIcon("images.svg");
  public static final ImageIcon              IDCARD                       = createTableHeaderIcon("id-card.svg");
  public static final ImageIcon              LOCK                         = createTableHeaderIcon("lock-alt.svg");
  public static final ImageIcon              MUSIC_HEADER                 = createTableHeaderIcon("music.svg");
  public static final ImageIcon              MISSING                      = createTableHeaderIcon("circle-xmark.svg");
  public static final ImageIcon              NEW                          = createTableHeaderIcon("circle-plus.svg");
  public static final ImageIcon              NFO                          = createTableHeaderIcon("file-lines.svg");
  public static final ImageIcon              RATING                       = createTableHeaderIcon("star.svg");
  public static final ImageIcon              RUNTIME                      = createTableHeaderIcon("clock.svg");
  public static final ImageIcon              SEASONS                      = createTextIcon("S", 1.4);
  public static final ImageIcon              SOURCE                       = createTableHeaderIcon("location.svg");
  public static final ImageIcon              SUBTITLES                    = createTableHeaderIcon("comments.svg");
  public static final ImageIcon              TRAILER                      = createTableHeaderIcon("film.svg");
  public static final ImageIcon              USER_RATING                  = createTableHeaderIcon("star-solid.svg");
  public static final ImageIcon              VIDEO_3D                     = createTableHeaderIcon("cube.svg");
  public static final ImageIcon              VIDEO_FORMAT                 = createTableHeaderIcon("expand-wide.svg");
  public static final ImageIcon              VIDEO_BITRATE                = createTableHeaderIcon("tachometer-fast.svg");
  public static final ImageIcon              VIDEO_CODEC                  = createTableHeaderIcon("file-video.svg");
  public static final ImageIcon              VOTES                        = createTableHeaderIcon("thumbs-up.svg");
  public static final ImageIcon              WATCHED                      = createTableHeaderIcon("play.svg");

  // sort icons for glazedlists
  public static final ImageIcon              SORT_UP_PRIMARY              = createSVGIcon("chevron-up.svg", 0.833);
  public static final ImageIcon              SORT_UP_SECONDARY            = createSVGIcon("chevrons-up.svg", 0.833);
  public static final ImageIcon              SORT_DOWN_PRIMARY            = createSVGIcon("chevron-down.svg", 0.833);
  public static final ImageIcon              SORT_DOWN_SECONDARY          = createSVGIcon("chevrons-down.svg", 0.833);

  private IconManager() {
    throw new IllegalAccessError();
  }

  private static ImageIcon loadImage(String name) {
    URL file = IconManager.class.getResource("images/interface/" + name);
    if (file != null) {
      return new ImageIcon(file);
    }

    return EMPTY_IMAGE;
  }

  private static ImageIcon loadMultiResolutionImage(String name) {
    List<String> resolutions = Arrays.asList("", "@125", "@150", "@200");
    try {
      List<Image> images = new ArrayList<>();

      String extension = FilenameUtils.getExtension(name);
      String basename = FilenameUtils.getBaseName(name);

      for (String resolution : resolutions) {
        URL file = IconManager.class.getResource("images/interface/" + basename + resolution + "." + extension);
        if (file != null) {
          images.add(new ImageIcon(file).getImage());
        }
      }

      return new ImageIcon(new BaseMultiResolutionImage(images.toArray(new Image[0])));
    }
    catch (Exception ignored) {
    }

    return EMPTY_IMAGE;
  }

  /**
   * loads an image from the given url
   *
   * @param url
   *          the url pointing to the image
   * @return the image or an empty image (1x1 px transparent) if it is not loadable
   */
  public static ImageIcon loadImageFromURL(URL url) {
    URI uri = null;

    if (url == null) {
      return EMPTY_IMAGE;
    }

    try {
      uri = url.toURI();
    }
    catch (Exception e) {
      return EMPTY_IMAGE;
    }

    // read cache
    ImageIcon icon = ICON_CACHE.get(uri);

    if (icon == null) {
      try {
        icon = new ImageIcon(url);
      }
      catch (Exception ignored) {
      }

      if (icon == null) {
        icon = EMPTY_IMAGE;
      }

      ICON_CACHE.put(uri, icon);
    }

    return icon;
  }

  private static ImageIcon createMenuIcon(String name) {
    return createSVGIcon(name, "Component.focusColor", TmmFontHelper.H3);
  }

  private static ImageIcon createButtonIcon(String name) {
    return createSVGIcon(name, "Button.foreground", TmmFontHelper.H3);
  }

  private static ImageIcon createTableHeaderIcon(String name) {
    return createSVGIcon(name, "TableHeader.foreground", TmmFontHelper.H2);
  }

  private static ImageIcon createSVGIcon(String name) {
    return createSVGIcon(name, "Label.foreground", TmmFontHelper.H3);
  }

  private static ImageIcon createSVGIcon(String name, String colorReference) {
    return createSVGIcon(name, colorReference, TmmFontHelper.H3);
  }

  private static ImageIcon createSVGIcon(String name, double scaleFactor) {
    return createSVGIcon(name, "Label.foreground", scaleFactor);
  }

  private static ImageIcon createSVGIcon(String name, Color color) {
    return createSVGIcon(name, color, TmmFontHelper.H2);
  }

  private static ImageIcon createSVGIcon(String name, String colorReference, double scaleFactor) {
    // create the icon
    ImageIcon icon = createSVGIcon(name, UIManager.getColor(colorReference), scaleFactor);

    // and put it to the cache (for recoloring/rescaling if needed)
    if (icon != EMPTY_IMAGE) {
      SVG_CACHE.add(new TmmSvgIconCache((TmmSvgIcon) icon, scaleFactor, colorReference));
    }
    return icon;
  }

  private static ImageIcon createSVGIcon(String name, Dimension size) {
    return createSVGIcon(name, size, null, false);
  }

  private static ImageIcon createSVGIcon(String name, Dimension size, boolean autoSize) {
    return createSVGIcon(name, size, null, autoSize);
  }

  private static ImageIcon createSVGIcon(String name, Dimension size, String colorReference) {
    return createSVGIcon(name, size, UIManager.getColor(colorReference), false);
  }

  private static ImageIcon createSVGIcon(String name, Dimension size, Color color, boolean autoSize) {
    try {
      // create the icon
      URI uri = IconManager.class.getResource("images/svg/" + name).toURI();
      TmmSvgIcon icon = new TmmSvgIcon(uri);
      icon.setPreferredSize(size);

      if (color != null) {
        icon.setColor(color);
      }

      if (!autoSize) {
        icon.setAutosize(SVGIcon.AUTOSIZE_NONE);
      }
      return icon;
    }
    catch (Exception e) {
      return EMPTY_IMAGE;
    }
  }

  private static ImageIcon createSVGIcon(String name, Color color, double scaleFactor) {
    try {
      // create the icon
      URI uri = IconManager.class.getResource("images/svg/" + name).toURI();
      TmmAwesomeIcon icon = new TmmAwesomeIcon(uri, color);

      icon.setPreferredHeight(calculateFontIconSize(scaleFactor));

      SVG_CACHE.add(new TmmSvgIconCache(icon, scaleFactor, null));
      return icon;
    }
    catch (Exception e) {
      return EMPTY_IMAGE;
    }
  }

  private static int calculateFontIconSize(double scaleFactor) {
    try {
      return (int) Math.floor(UIManager.getFont("defaultFont").getSize() * scaleFactor);
    }
    catch (Exception e) {
      return (int) Math.floor(12 * scaleFactor);
    }
  }

  private static ImageIcon createTextIcon(String text) {
    return createTextIcon(text, 1, UIManager.getColor("Label.foreground"));
  }

  private static ImageIcon createTextIcon(String text, Color color) {
    return createTextIcon(text, 1, color);
  }

  private static ImageIcon createTextIcon(String text, double scaleFactor) {
    ImageIcon icon = createTextIcon(text, scaleFactor, UIManager.getColor("Label.foreground"));
    TEXT_ICON_CACHE.add(new TmmTextIconCache((TmmTextIcon) icon, scaleFactor, null));
    return icon;
  }

  private static ImageIcon createTextIcon(String text, double scaleFactor, Color color) {
    int size = calculateFontIconSize(scaleFactor);
    TmmTextIcon icon = new TmmTextIcon(text, size, color);

    TEXT_ICON_CACHE.add(new TmmTextIconCache(icon, scaleFactor, null));
    return icon;
  }

  private static class TmmSvgIconCache {
    private TmmSvgIcon   icon;
    private final double scaleFactor;
    private final String colorReference;

    public TmmSvgIconCache(TmmSvgIcon icon, double scaleFactor, String colorReference) {
      this.icon = icon;
      this.scaleFactor = scaleFactor;
      this.colorReference = colorReference;
    }
  }

  private static class TmmTextIconCache {
    private TmmTextIcon  icon;
    private final double scaleFactor;
    private final String colorReference;

    public TmmTextIconCache(TmmTextIcon icon, double scaleFactor, String colorReference) {
      this.icon = icon;
      this.scaleFactor = scaleFactor;
      this.colorReference = colorReference;
    }
  }

  static void updateIcons() {
    for (TmmSvgIconCache iconCache : SVG_CACHE) {
      if (StringUtils.isNotBlank(iconCache.colorReference)) {
        iconCache.icon.setColor(UIManager.getColor(iconCache.colorReference));
      }
      int size = calculateFontIconSize(iconCache.scaleFactor);
      iconCache.icon.setPreferredSize(new Dimension(size, size));
    }
    for (TmmTextIconCache iconCache : TEXT_ICON_CACHE) {
      if (StringUtils.isNotBlank(iconCache.colorReference)) {
        iconCache.icon.setColor(UIManager.getColor(iconCache.colorReference));
      }
      int size = calculateFontIconSize(iconCache.scaleFactor);
      iconCache.icon.setFontSize(size);
      iconCache.icon.update();
    }
  }
}
