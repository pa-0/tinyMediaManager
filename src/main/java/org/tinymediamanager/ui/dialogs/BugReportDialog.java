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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.net.URLEncoder;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowComparator;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.ExportLogAction;

import net.miginfocom.swing.MigLayout;

/**
 * The Class BugReportDialog, to send bug reports directly from inside tmm.
 * 
 * @author Manuel Laggner
 */
public class BugReportDialog extends TmmDialog {
  private static final Logger LOGGER    = LoggerFactory.getLogger(BugReportDialog.class);

  private static final String DIALOG_ID = "bugReportdialog";

  /**
   * Instantiates a new feedback dialog.
   */
  public BugReportDialog() {
    super(TmmResourceBundle.getString("BugReport"), DIALOG_ID);

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("", "[][][450lp,grow]", "[][20lp][][][][][20lp][][][]"));

    final JTextArea taDescription = new JTextArea();
    taDescription.setOpaque(false);
    taDescription.setWrapStyleWord(true);
    taDescription.setLineWrap(true);
    taDescription.setEditable(false);
    taDescription.setText(TmmResourceBundle.getString("BugReport.description"));
    panelContent.add(taDescription, "cell 0 0 3 1,growx");

    final JLabel lblStep1 = new JLabel(TmmResourceBundle.getString("BugReport.step1"));
    panelContent.add(lblStep1, "cell 0 2");

    final JTextArea taStep1 = new JTextArea();
    taStep1.setWrapStyleWord(true);
    taStep1.setLineWrap(true);
    taStep1.setText(TmmResourceBundle.getString("BugReport.step1.description"));
    taStep1.setOpaque(false);
    taStep1.setEditable(false);
    panelContent.add(taStep1, "cell 2 2,growx");

    JComboBox<EntityContainer> cbMovieList = new JComboBox<EntityContainer>();
    Movie dummym = new Movie();
    dummym.setTitle("- select movie -");
    dummym.setDbId(null);
    cbMovieList.addItem(new EntityContainer(dummym)); // fix first entry!
    List<Movie> movies = MovieModuleManager.getInstance().getMovieList().getMovies();
    movies.sort(new MovieComparator());
    for (MediaEntity m : movies) {
      cbMovieList.addItem(new EntityContainer(m));
    }
    panelContent.add(cbMovieList, "cell 2 3,growx");
    JComboBox<EntityContainer> cbTvshowList = new JComboBox<EntityContainer>();
    TvShow dummys = new TvShow();
    dummys.setTitle("- select tvShow -");
    dummys.setDbId(null);
    cbTvshowList.addItem(new EntityContainer(dummys)); // fix first entry!
    List<TvShow> tvShows = TvShowModuleManager.getInstance().getTvShowList().getTvShows();
    tvShows.sort(new TvShowComparator());
    for (TvShow s : tvShows) {
      cbTvshowList.addItem(new EntityContainer(s));
    }
    panelContent.add(cbTvshowList, "cell 2 4,growx");

    final JButton btnSaveLogs = new JButton(TmmResourceBundle.getString("BugReport.createlogs"));
    btnSaveLogs.addActionListener(e -> {
      Movie m = (Movie) ((EntityContainer) cbMovieList.getSelectedItem()).entity;
      TvShow s = (TvShow) ((EntityContainer) cbTvshowList.getSelectedItem()).entity;
      ExportLogAction ela = new ExportLogAction(m, s);
      ela.actionPerformed(e); // run
    });
    panelContent.add(btnSaveLogs, "cell 2 5");

    final JLabel lblStep2 = new JLabel(TmmResourceBundle.getString("BugReport.step2"));
    panelContent.add(lblStep2, "cell 0 7");

    final JTextArea taStep2 = new JTextArea();
    taStep2.setLineWrap(true);
    taStep2.setWrapStyleWord(true);
    taStep2.setOpaque(false);
    taStep2.setEditable(false);
    taStep2.setText(TmmResourceBundle.getString("BugReport.step2.description"));
    panelContent.add(taStep2, "cell 2 7,growx");

    final JButton btnCreateIssue = new JButton(TmmResourceBundle.getString("BugReport.craeteissue"));
    btnCreateIssue.addActionListener(e -> {
      // create the url for github
      String baseUrl = "https://gitlab.com/tinyMediaManager/tinyMediaManager/issues/new?issuable_template=Bug&issue[description]=";
      String params = "Version: " + ReleaseInfo.getRealVersion() + "  ";
      params += "\nBuild: " + ReleaseInfo.getRealBuildDate() + "  ";
      params += "\nOS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "  ";
      params += "\nJDK: " + System.getProperty("java.version") + " " + System.getProperty("os.arch") + " " + System.getProperty("java.vendor") + "  ";

      String url = "";
      try {
        url = baseUrl + URLEncoder.encode(params, "UTF-8");
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception e1) {
        LOGGER.error("FAQ", e1);
        MessageManager.instance
            .pushMessage(new Message(MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e1.getLocalizedMessage() }));
      }
    });
    panelContent.add(btnCreateIssue, "cell 2 8,alignx left,aligny center");

    final JLabel lblHintIcon = new JLabel(IconManager.HINT);
    panelContent.add(lblHintIcon, "cell 1 9,alignx left,aligny center");

    final JLabel lblHint = new JLabel(TmmResourceBundle.getString("BugReport.languagehint"));
    panelContent.add(lblHint, "cell 2 9,growx,aligny top");

    JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
    btnClose.setIcon(IconManager.CANCEL_INV);
    btnClose.addActionListener(e -> setVisible(false));
    addDefaultButton(btnClose);
  }

  class EntityContainer {
    MediaEntity entity;

    EntityContainer(MediaEntity entity) {
      this.entity = entity;
    }

    @Override
    public String toString() {
      return entity.getTitle();
    }
  }

}
