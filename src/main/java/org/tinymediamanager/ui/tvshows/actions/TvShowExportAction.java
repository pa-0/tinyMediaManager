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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tasks.ExportTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowExporter;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.panels.ExporterPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The TvShowExportAction - to export all selected TV shows via a template
 * 
 * @author Manuel Laggner
 */
public class TvShowExportAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowExportAction.class);

  public TvShowExportAction() {
    putValue(LARGE_ICON_KEY, IconManager.EXPORT);
    putValue(SMALL_ICON, IconManager.EXPORT);
    putValue(NAME, TmmResourceBundle.getString("tvshow.export"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive(true);

    if (selectedTvShows.isEmpty()) {
      return;
    }

    ModalPopupPanel popupPanel = MainWindow.getInstance().createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("tvshow.export"));

    ExporterPanel exporterPanel = new ExporterPanel(MediaEntityExporter.TemplateType.TV_SHOW) {
      @Override
      protected void onClose() {
        if (StringUtils.isBlank(tfExportDir.getText())) {
          return;
        }
        // check selected template
        ExportTemplate template = list.getSelectedValue();
        if (template == null) {
          return;
        }

        Path exportPath;
        try {
          exportPath = getExportPath();
        }
        catch (Exception e) {
          LOGGER.debug("Aborted export - '{}'", e.getMessage());
          return;
        }

        try {
          TmmProperties.getInstance().putProperty(panelId + ".template", template.getName());
          TvShowExporter exporter = new TvShowExporter(Paths.get(template.getPath()));
          TmmTaskManager.getInstance()
              .addMainTask(new ExportTask(TmmResourceBundle.getString("tvshow.export"), exporter, selectedTvShows, exportPath));
        }
        catch (Exception e) {
          LOGGER.error("Error exporting TV shows: ", e);
        }
        setVisible(false);
      }
    };

    popupPanel.setContent(exporterPanel);
    MainWindow.getInstance().showModalPopupPanel(popupPanel);
  }
}
