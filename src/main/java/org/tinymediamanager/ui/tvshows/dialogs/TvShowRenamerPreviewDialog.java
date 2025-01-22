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
package org.tinymediamanager.ui.tvshows.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowComparator;
import org.tinymediamanager.core.tvshow.TvShowRenamerPreview;
import org.tinymediamanager.core.tvshow.TvShowRenamerPreviewContainer;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowRenamerPreviewDialog} is used to generate a preview which TV shows/episode have to be renamed.
 * 
 * @author Manuel Laggner
 */
public class TvShowRenamerPreviewDialog extends TmmDialog {
  private final EventList<TvShowRenamerPreviewContainer> results;
  private final ResultSelectionModel                     resultSelectionModel;
  private final EventList<MediaFileContainer>            oldMediaFileEventList;
  private final EventList<MediaFileContainer>            newMediaFileEventList;

  /** UI components */
  private final TmmTable                                 tableMovies;
  private final JLabel                                   lblTitle;
  private final JLabel                                   lblDatasource;
  private final JLabel                                   lblFolderOld;
  private final JLabel                                   lblFolderNew;
  private final JCheckBox                                cbFilter;

  private final TvShowPreviewWorker                      worker;

  public TvShowRenamerPreviewDialog(final List<TvShow> selectedTvShows) {
    super(TmmResourceBundle.getString("movie.renamerpreview"), "tvShowRenamerPreview");

    oldMediaFileEventList = GlazedLists.eventList(new ArrayList<>());
    newMediaFileEventList = GlazedLists.eventList(new ArrayList<>());

    results = GlazedListsSwing.swingThreadProxyList(GlazedLists.threadSafeList(new BasicEventList<>()));
    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[950lp,grow]", "[600lp,grow]"));
      {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setName(getName() + ".splitPane");
        TmmUILayoutStore.getInstance().install(splitPane);
        splitPane.setResizeWeight(0.3);
        panelContent.add(splitPane, "cell 0 0,grow");
        {
          TmmTableModel<TvShowRenamerPreviewContainer> movieTableModel = new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(results),
              new ResultTableFormat());
          tableMovies = new TmmTable(movieTableModel);

          DefaultEventSelectionModel<TvShowRenamerPreviewContainer> tableSelectionModel = new DefaultEventSelectionModel<>(results);
          resultSelectionModel = new ResultSelectionModel();
          tableSelectionModel.addListSelectionListener(resultSelectionModel);
          resultSelectionModel.selectedResults = tableSelectionModel.getSelected();
          tableMovies.setSelectionModel(tableSelectionModel);

          movieTableModel.addTableModelListener(arg0 -> {
            // select first movie if nothing is selected
            ListSelectionModel selectionModel = tableMovies.getSelectionModel();
            if (selectionModel.isSelectionEmpty() && movieTableModel.getRowCount() > 0) {
              selectionModel.setSelectionInterval(0, 0);
            }
            if (selectionModel.isSelectionEmpty() && movieTableModel.getRowCount() == 0) {
              resultSelectionModel.setSelectedResult(null);
            }
          });

          JScrollPane scrollPaneMovies = new JScrollPane();
          tableMovies.configureScrollPane(scrollPaneMovies);
          splitPane.setLeftComponent(scrollPaneMovies);
        }
        {
          JPanel panelDetails = new JPanel();
          splitPane.setRightComponent(panelDetails);
          panelDetails.setLayout(new MigLayout("", "[][][300lp,grow]", "[][][][][][][][grow]"));
          {
            lblTitle = new JLabel("");
            TmmFontHelper.changeFont(lblTitle, 1.33, Font.BOLD);
            panelDetails.add(lblTitle, "cell 0 0 3 1,growx");
          }
          {
            JLabel lblDatasourceT = new TmmLabel(TmmResourceBundle.getString("metatag.datasource"));
            panelDetails.add(lblDatasourceT, "cell 0 2");

            lblDatasource = new JLabel("");
            panelDetails.add(lblDatasource, "cell 2 2,growx,aligny center");
          }
          {
            JLabel lblFolderOldT = new TmmLabel(TmmResourceBundle.getString("renamer.oldfolder"));
            panelDetails.add(lblFolderOldT, "cell 0 4");

            lblFolderOld = new JLabel("");
            panelDetails.add(lblFolderOld, "cell 2 4,growx,aligny center");
          }
          {
            JLabel lblFolderNewT = new TmmLabel(TmmResourceBundle.getString("renamer.newfolder"));
            panelDetails.add(lblFolderNewT, "cell 0 5");

            lblFolderNew = new JLabel("");
            panelDetails.add(lblFolderNew, "cell 2 5,growx,aligny center");
          }
          {
            JPanel panelMediaFiles = new JPanel();
            panelDetails.add(panelMediaFiles, "cell 0 7 3 1,grow");
            panelMediaFiles.setLayout(new MigLayout("", "[grow][grow]", "[15px][grow]"));
            {
              JLabel lblOldfilesT = new TmmLabel(TmmResourceBundle.getString("renamer.oldfiles"));
              panelMediaFiles.add(lblOldfilesT, "cell 0 0,alignx center");

              JLabel lblNewfilesT = new TmmLabel(TmmResourceBundle.getString("renamer.newfiles"));
              panelMediaFiles.add(lblNewfilesT, "cell 1 0,alignx center");
            }
            {
              TmmTable tableMediaFilesOld = new TmmTable(
                  new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(oldMediaFileEventList), new MediaFileTableFormat()));
              JScrollPane scrollPaneMediaFilesOld = new JScrollPane();
              tableMediaFilesOld.configureScrollPane(scrollPaneMediaFilesOld);
              panelMediaFiles.add(scrollPaneMediaFilesOld, "cell 0 1,grow");
              tableMediaFilesOld.getColumnModel().getColumn(0).setMaxWidth(40);
            }
            {
              TmmTable tableMediaFilesNew = new TmmTable(
                  new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(newMediaFileEventList), new MediaFileTableFormat()));
              JScrollPane scrollPaneMediaFilesNew = new JScrollPane(tableMediaFilesNew);
              tableMediaFilesNew.configureScrollPane(scrollPaneMediaFilesNew);
              panelMediaFiles.add(scrollPaneMediaFilesNew, "cell 1 1,grow");
              tableMediaFilesNew.getColumnModel().getColumn(0).setMaxWidth(40);
            }
          }
        }
      }
    }
    {
      {
        cbFilter = new JCheckBox(TmmResourceBundle.getString("renamer.hideunchanged"));
        cbFilter.addActionListener(l -> resultSelectionModel.updateSelectedResult());

        JPanel bottomPanel = new JPanel(new MigLayout("", "[]", "[]"));
        bottomPanel.add(cbFilter, "cell 0 0");

        setBottomInformationPanel(bottomPanel);
      }
    }
    {
      JButton btnRename = new JButton(TmmResourceBundle.getString("Button.rename"));
      btnRename.setToolTipText(TmmResourceBundle.getString("movie.rename"));
      btnRename.addActionListener(arg0 -> {
        List<TvShow> selectedTvShows1 = new ArrayList<>();
        List<TvShowEpisode> selectedEpisodes = new ArrayList<>();
        List<TvShowRenamerPreviewContainer> selectedResults = new ArrayList<>(resultSelectionModel.selectedResults);
        for (TvShowRenamerPreviewContainer result : selectedResults) {
          selectedTvShows1.add(result.getTvShow());
          selectedEpisodes.addAll(result.getTvShow().getEpisodes());
        }

        // rename
        TmmThreadPool renameTask = new TvShowRenameTask(selectedTvShows1, selectedEpisodes);
        TmmTaskManager.getInstance().addMainTask(renameTask);
        results.removeAll(selectedResults);
      });
      addButton(btnRename);

      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }

    // start calculation of the preview
    worker = new TvShowPreviewWorker(selectedTvShows);
    worker.execute();
  }

  @Override
  public void setVisible(boolean visible) {
    if (!visible) {
      // window closing
      if (worker != null && !worker.isDone()) {
        worker.cancel(true);
      }
    }

    super.setVisible(visible);
  }

  /**********************************************************************
   * helper classes
   *********************************************************************/
  private static class ResultTableFormat extends TmmTableFormat<TvShowRenamerPreviewContainer> {

    public ResultTableFormat() {
      /*
       * movie title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.tvshow"), "title", container -> container.getTvShow().getTitleSortable(),
          String.class);
      col.setCellTooltip(container -> container.getTvShow().getTitleSortable());
      addColumn(col);
    }
  }

  private static class MediaFileTableFormat extends TmmTableFormat<MediaFileContainer> {
    public MediaFileTableFormat() {
      /*
       * indicator
       */
      Column col = new Column("", "indicator", container -> container.icon, ImageIcon.class);
      addColumn(col);

      /*
       * filename
       */
      col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", container -> container.filename, String.class);
      col.setCellTooltip(container -> container.filename);
      addColumn(col);
    }
  }

  private class TvShowPreviewWorker extends SwingWorker<Void, Void> {
    private final List<TvShow> tvShowsToProcess;

    private TvShowPreviewWorker(List<TvShow> tvShows) {
      this.tvShowsToProcess = new ArrayList<>(tvShows);
    }

    @Override
    protected Void doInBackground() {
      // sort movies
      tvShowsToProcess.sort(new TvShowComparator());
      // rename them
      for (TvShow tvShow : tvShowsToProcess) {
        if (isCancelled()) {
          return null;
        }

        TvShowRenamerPreviewContainer container = new TvShowRenamerPreview(tvShow).generatePreview();
        if (container.isNeedsRename()) {
          results.add(container);
        }
      }

      SwingUtilities.invokeLater(() -> {
        if (results.isEmpty()) { // check has to be in here, since it needs some time to propagate
          JOptionPane.showMessageDialog(TvShowRenamerPreviewDialog.this, TmmResourceBundle.getString("movie.renamerpreview.nothingtorename"));
          setVisible(false);
        }
      });

      return null;
    }
  }

  private class ResultSelectionModel extends AbstractModelObject implements ListSelectionListener {
    private final TvShowRenamerPreviewContainer emptyResult;

    private TvShowRenamerPreviewContainer       selectedResult;
    private List<TvShowRenamerPreviewContainer> selectedResults;

    ResultSelectionModel() {
      emptyResult = new TvShowRenamerPreviewContainer(new TvShow());
    }

    void updateSelectedResult() {
      lblTitle.setText(selectedResult.getTvShow().getTitleSortable());
      lblDatasource.setText(selectedResult.getTvShow().getDataSource());

      // the empty result does not have any valid Path
      if (selectedResult != emptyResult) {
        lblFolderOld.setText(selectedResult.getOldPathRelative().toString());
        lblFolderNew.setText(selectedResult.getNewPathRelative().toString());
      }
      else {
        lblFolderOld.setText("");
        lblFolderNew.setText("");
      }

      // set Mfs
      try {
        oldMediaFileEventList.getReadWriteLock().writeLock().lock();
        oldMediaFileEventList.clear();
        for (MediaFile mf : selectedResult.getOldMediaFiles()) {
          boolean found = false;
          MediaFileContainer container = new MediaFileContainer();
          // newPath here, since we already faked the files in the renamer
          container.filename = selectedResult.getNewPath().relativize(mf.getFileAsPath()).toString();

          if (selectedResult.getNewMediaFiles().contains(mf)) {
            found = true;
          }

          if (!found) {
            container.icon = IconManager.REMOVE;
          }

          if (found && cbFilter.isSelected()) {
            // same - do not add
          }
          else {
            oldMediaFileEventList.add(container);
          }
        }
      }
      catch (Exception ignored) {
        // ignored
      }
      finally {
        oldMediaFileEventList.getReadWriteLock().writeLock().unlock();
      }

      try {
        newMediaFileEventList.getReadWriteLock().writeLock().lock();
        newMediaFileEventList.clear();
        for (MediaFile mf : selectedResult.getNewMediaFiles()) {
          boolean found = false;
          MediaFileContainer container = new MediaFileContainer();
          container.filename = selectedResult.getNewPath().relativize(mf.getFileAsPath()).toString();

          if (selectedResult.getOldMediaFiles().contains(mf)) {
            found = true;
          }

          if (!found) {
            container.icon = IconManager.ADD;

          }
          if (found && cbFilter.isSelected()) {
            // same - do not add
          }
          else {
            newMediaFileEventList.add(container);
          }
        }
      }
      catch (Exception ignored) {
        // ignored
      }
      finally {
        newMediaFileEventList.getReadWriteLock().writeLock().unlock();
      }
    }

    synchronized void setSelectedResult(TvShowRenamerPreviewContainer newValue) {
      if (newValue == null) {
        selectedResult = emptyResult;
      }
      else {
        selectedResult = newValue;
      }

      updateSelectedResult();
    }

    @Override
    public void valueChanged(ListSelectionEvent arg0) {
      if (arg0.getValueIsAdjusting()) {
        return;
      }

      // display first selected result
      if (!selectedResults.isEmpty() && selectedResult != selectedResults.get(0)) {
        setSelectedResult(selectedResults.get(0));
      }

      // display empty result
      if (selectedResults.isEmpty()) {
        setSelectedResult(emptyResult);
      }
    }
  }

  private static class MediaFileContainer {
    ImageIcon icon = null;
    String    filename;
  }
}
