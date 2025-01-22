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
package org.tinymediamanager.ui.movies;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.floreysoft.jmte.Engine;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * a text field based filter for filtering on movie titles
 *
 * @author Manuel Laggner
 */
public class MovieTextMatcherEditor extends AbstractMatcherEditor<Movie> {
  private final MovieSettings  settings = MovieModuleManager.getInstance().getSettings();
  private final JTextComponent textComponent;
  private static final Engine  ENGINE   = MovieRenamer.createEngine();

  private String               normalizedFilterText;
  private Pattern              filterPattern;

  public MovieTextMatcherEditor(JTextComponent textComponent) {
    this.textComponent = textComponent;
    this.textComponent.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) {
        refilter();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        refilter();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        refilter();
      }
    });
  }

  private void refilter() {
    normalizedFilterText = StrgUtils.normalizeString(textComponent.getText());
    try {
      filterPattern = Pattern.compile(normalizedFilterText, Pattern.CASE_INSENSITIVE);
      fireChanged(new MovieMatcher());
    }
    catch (PatternSyntaxException ignore) {
      filterPattern = null;
    }
  }

  /*
   * helper class for running this filter against the given movie
   */
  private class MovieMatcher implements Matcher<Movie> {
    @Override
    public boolean matches(Movie movie) {
      if (StringUtils.isBlank(normalizedFilterText) || filterPattern == null) {
        return true;
      }

      if (settings.getTitle() && StringUtils.isNotBlank(movie.getTitle())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getTitle()));
        if (matcher.find()) {
          return true;
        }
      }
      if (settings.getSortableTitle() && StringUtils.isNotBlank(movie.getTitleSortable())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getTitleSortable()));
        if (matcher.find()) {
          return true;
        }
      }

      if (settings.getOriginalTitle() && StringUtils.isNotBlank(movie.getOriginalTitle())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getOriginalTitle()));
        if (matcher.find()) {
          return true;
        }
      }
      if (settings.getSortableOriginalTitle() && StringUtils.isNotBlank(movie.getOriginalTitleSortable())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getOriginalTitleSortable()));
        if (matcher.find()) {
          return true;
        }
      }
      if (settings.getSortTitle() && StringUtils.isNotBlank(movie.getSortTitle())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getSortTitle()));
        if (matcher.find()) {
          return true;
        }
      }
      // match by field:value (eg search by ids:160, actors:mel gibson))
      if (normalizedFilterText.matches("\\w+:\\w[\\w\\s]+")) {
        String[] kv = normalizedFilterText.split(":");
        try {
          PropertyDescriptor pd = new PropertyDescriptor(kv[0], Movie.class);
          Method getter = pd.getReadMethod();
          Object f = getter.invoke(movie);

          String res = String.valueOf(f).toLowerCase(Locale.ROOT);
          if (res.contains(kv[1].toLowerCase(Locale.ROOT))) {
            // System.out.println("Found " + kv[1] + " in " + res);
            return true;
          }
        }
        catch (Exception e) {
          // Fallback: try field via JMTE
          if (MovieRenamer.getTokenMap().containsKey(kv[0])) {
            Map<String, Object> root = new HashMap<>();
            root.put("movie", movie);
            String val = ENGINE.transform(JmteUtils.morphTemplate("${" + kv[0] + "}", MovieRenamer.getTokenMap()), root);
            if (StringUtils.containsIgnoreCase(val, kv[1])) {
              return true;
            }
          }
        }
      }

      return false;
    }
  }
}
