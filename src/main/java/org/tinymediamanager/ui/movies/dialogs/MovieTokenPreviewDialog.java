package org.tinymediamanager.ui.movies.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TmmToStringStyle;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmRoundTextArea;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieUIModule;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.Renderer;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

public class MovieTokenPreviewDialog extends TmmDialog {

  private JComboBox                         cbMovieForPreview;

  private JTextArea                         taMovieTokens;
  private JTextArea                         taResult;
  private JLabel                            lblError;
  private JCheckBox                         chckbxPrettyPrint;

  private final EventList<MovieRenamerTab1> movieRenamerTab1EventList;
  private final EventList<RendererExample>  rendererExampleList;

  public MovieTokenPreviewDialog() {
    super(TmmResourceBundle.getString("movie.jmteexplorer"), "moviejmteexplorer");
    this.movieRenamerTab1EventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MovieRenamerTab1.class)));
    this.rendererExampleList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(RendererExample.class)));

    setModal(false);

    initComponents();
    setListeners();

    buildAndInstallMovieArray();
  }

  private void initComponents() {
    JPanel headerPanel = new JPanel(new MigLayout("", "[][]", "[][]"));
    {
      // Movie / Pattern Input
      {
        headerPanel.add(new JLabel(TmmResourceBundle.getString("tmm.movie")), "cell 0 0");

        cbMovieForPreview = new JComboBox();
        headerPanel.add(cbMovieForPreview, "cell 1 0, growx");

        headerPanel.add(new JLabel(TmmResourceBundle.getString("Settings.jmtepattern")), "cell 0 1");

        taMovieTokens = new TmmRoundTextArea();
        headerPanel.add(taMovieTokens, "cell 1 1, grow");

        chckbxPrettyPrint = new JCheckBox("pretty print result");
        headerPanel.add(chckbxPrettyPrint, "cell 0 2 2 1");
      }
    }
    setTopPanel(headerPanel);

    JSplitPane contentPanel = new JSplitPane();
    {
      // left
      {
        JTabbedPane tabbedPane = new MainTabbedPane();
        tabbedPane.add("Props Movies", createPropsMoviePanel());
        tabbedPane.add("Examples", createMovieRendererPanel());

        contentPanel.setLeftComponent(tabbedPane);
      }

      // right
      {
        JTabbedPane tabbedPane = new MainTabbedPane();
        taResult = new ReadOnlyTextArea();

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(taResult);
        tabbedPane.add("Result", scrollPane);

        contentPanel.setRightComponent(tabbedPane);
      }
    }
    add(contentPanel);

    {
      JButton btnDone = new JButton(TmmResourceBundle.getString("Button.close"));
      btnDone.setIcon(IconManager.APPLY_INV);
      btnDone.addActionListener(e -> setVisible(false));
      addDefaultButton(btnDone);
    }
    {
      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new MigLayout("hidemode 3", "[grow]", "[]"));

      lblError = new JLabel("");
      TmmFontHelper.changeFont(lblError, Font.BOLD);
      lblError.setForeground(Color.RED);
      infoPanel.add(lblError, "cell 1 0");

      setBottomInformationPanel(infoPanel);
    }

  }

  private void buildAndInstallMovieArray() {
    cbMovieForPreview.removeAllItems();
    List<Movie> allMovies = new ArrayList<>(MovieModuleManager.getInstance().getMovieList().getMovies());
    Movie sel = MovieUIModule.getInstance().getSelectionModel().getSelectedMovie();
    allMovies.sort(new MovieComparator());
    for (Movie movie : allMovies) {
      MoviePreviewContainer container = new MoviePreviewContainer();
      container.movie = movie;
      cbMovieForPreview.addItem(container);
      if (movie.equals(sel)) {
        cbMovieForPreview.setSelectedItem(container);
      }
    }
  }

  private void createRenamerExample() {
    Movie movie = null;

    if (cbMovieForPreview.getSelectedItem() instanceof MoviePreviewContainer container) {
      movie = container.movie;
    }

    if (movie != null) {
      String result = "";

      if (StringUtils.isNotBlank(taMovieTokens.getText())) {
        try {
          Engine engine = MovieRenamer.createEngine();

          engine.registerRenderer(Collection.class, new CollectionRenderer());

          Map<String, Object> root = new HashMap<>();
          root.put("movie", movie);

          // only offer movie set for movies with more than 1 movies or if setting is set
          if (movie.getMovieSet() != null && (movie.getMovieSet().getMovies().size() > 1
              || MovieModuleManager.getInstance().getSettings().isRenamerCreateMoviesetForSingleMovie())) {
            root.put("movieSet", movie.getMovieSet());
          }

          result = engine.transform(JmteUtils.morphTemplate(taMovieTokens.getText(), MovieRenamer.getTokenMap()), root);
          lblError.setText("");
        }
        catch (Exception e) {
          lblError.setText(e.getMessage());
        }

      }
      else {
        result = "";
        lblError.setText("");
      }

      try {
        taResult.setText(result);
        movieRenamerTab1EventList.clear();
        fillEventList(movie);
        createExamples(movie);
      }
      catch (Exception ignored) {
      }
    }
    else {
      taResult.setText(TmmResourceBundle.getString("Settings.movie.renamer.nomovie"));
      taResult.setToolTipText(null);
    }
  }

  private JComponent createPropsMoviePanel() {
    TmmTable table = new TmmTable(
        new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(movieRenamerTab1EventList), new MovieRenamerTab1TableFormat()));

    JScrollPane scrollPane = new JScrollPane();
    table.configureScrollPane(scrollPane);

    return scrollPane;
  }

  private JComponent createMovieRendererPanel() {
    TmmTable table = new TmmTable(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(rendererExampleList), new RendererExampleTableFormat()));

    JScrollPane scrollPane = new JScrollPane();
    table.configureScrollPane(scrollPane);

    return scrollPane;
  }

  /*****************************************************************************
   * helper classes
   *****************************************************************************/
  private static class CollectionRenderer implements Renderer<Collection> {

    @Override
    public String render(Collection o, Locale locale, Map<String, Object> model) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");

      for (var entry : o) {
        sb.append(ToStringBuilder.reflectionToString(entry, TmmToStringStyle.TMM_STYLE, false));
      }

      sb.append("]");
      return sb.toString();
    }
  }

  private static class MoviePreviewContainer {
    Movie movie;

    @Override
    public String toString() {
      return movie.getTitle();
    }
  }

  private static class MovieComparator implements Comparator<Movie> {

    @Override
    public int compare(Movie arg0, Movie arg1) {
      return arg0.getTitle().compareTo(arg1.getTitle());
    }
  }

  private static class MovieRenamerTab1 extends AbstractModelObject {
    private final String title;
    private final String shortcut;
    private final String result;

    private MovieRenamerTab1(String title, String shortcut, String result) {
      this.title = title;
      this.shortcut = shortcut;
      this.result = result;
    }
  }

  private static class MovieRenamerTab1TableFormat extends TmmTableFormat<MovieRenamerTab1> {
    public MovieRenamerTab1TableFormat() {
      Column title = new Column(TmmResourceBundle.getString("Titel"), "title", movieRenamerTab1 -> movieRenamerTab1.title, String.class);
      addColumn(title);

      Column shortcut = new Column(TmmResourceBundle.getString("Shortcut"), "shortcut", movieRenamerTab1 -> movieRenamerTab1.shortcut, String.class);
      addColumn(shortcut);

      Column result = new Column(TmmResourceBundle.getString("Result"), "result", movieRenamerTab1 -> movieRenamerTab1.result, String.class);
      addColumn(result);
    }
  }

  private static class RendererExample extends AbstractModelObject {
    private final String token;
    private final String result;

    private RendererExample(String token, String result) {
      this.token = token;
      this.result = result;
    }
  }

  private static class RendererExampleTableFormat extends TmmTableFormat<RendererExample> {
    public RendererExampleTableFormat() {
      Column title = new Column(TmmResourceBundle.getString(""), "token", example -> example.token, String.class);
      addColumn(title);

      Column result = new Column(TmmResourceBundle.getString(""), "result", example -> example.result, String.class);
      addColumn(result);
    }
  }

  /**
   * Get the short token
   * 
   * @param prefix
   *          the prefix of the token
   * @param name
   *          the name
   * @return the short token
   */
  private String getShortToken(String prefix, String name) {
    String shortToken = MovieRenamer.getTokenMapReversed().get(prefix + name);
    if (shortToken == null) {
      shortToken = TvShowRenamer.getTokenMapReversed().get(prefix + name);
    }
    if (shortToken != null && prefix.length() > 3) {
      shortToken = "${" + shortToken + "}";
    }
    else {
      shortToken = "";
    }
    return shortToken;
  }

  /**
   * Get the full token
   * 
   * @param prefix
   *          prefix to use for the token
   * @param name
   *          name
   * @return the full token
   */
  private String getFullToken(String prefix, String name) {
    String fullToken = name;
    if (prefix.length() > 3) {
      fullToken = "${" + prefix + fullToken + "}";
    }
    return fullToken;
  }

  private void setListeners() {
    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }
    };

    ActionListener actionCreateRenamerExample = e -> createRenamerExample();
    taMovieTokens.getDocument().addDocumentListener(documentListener);
    cbMovieForPreview.addActionListener(actionCreateRenamerExample);
  }

  private void fillEventList(Movie movie) throws IntrospectionException {
    PropertyDescriptor[] pds = Introspector.getBeanInfo(Movie.class).getPropertyDescriptors();
    movieRenamerTab1EventList.clear();
    for (PropertyDescriptor descriptor : pds) {

      if ("class".equals(descriptor.getDisplayName())) {
        continue;
      }

      if ("declaringClass".equals(descriptor.getDisplayName())) {
        continue;
      }

      if (descriptor.getReadMethod() != null) {

        String shortcut = getShortToken("movie.", descriptor.getDisplayName());
        String title = getFullToken("movie.", descriptor.getDisplayName());
        String result = MovieRenamer.createDestination(title, movie, true);

        movieRenamerTab1EventList.add(new MovieRenamerTab1(title, shortcut, result));
      }
    }
  }

  private void createExamples(Movie movie) {
    rendererExampleList.clear();

    // case renderers
    rendererExampleList.add(createRendererExample("--- case renderers ---", null));
    rendererExampleList.add(createRendererExample("${movie.title;upper}", movie));
    rendererExampleList.add(createRendererExample("${movie.title;lower}", movie));
    rendererExampleList.add(createRendererExample("${movie.title;first}", movie));

    // array handling
    rendererExampleList.add(createRendererExample("--- arrays ---", null));
    rendererExampleList.add(createRendererExample("${movie.genres}", movie));
    rendererExampleList.add(createRendererExample("${movie.genres;array}", movie));
    rendererExampleList.add(createRendererExample("${movie.genres[0]}", movie));
    rendererExampleList.add(createRendererExample("${movie.genres;split(0,2)}", movie));
    rendererExampleList.add(createRendererExample("${movie.mediaInfoAudioLanguageList;uniqueArray}", movie));

  }

  private RendererExample createRendererExample(String token, Movie movie) {
    if (movie == null) {
      return new RendererExample(token, "");
    }
    else {
      return new RendererExample(token, MovieRenamer.createDestination(token, movie, true));
    }
  }
}
