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

package org.tinymediamanager.ui.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.license.ZipArchiveHelper;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * the class {@link ExportAnalysisDataAction} is used offer enhanced analysis data
 *
 * @author Manuel Laggner
 */
public class ExportAnalysisDataAction extends TmmAction {
  private static final long   serialVersionUID = 1L;
  private static final Logger LOGGER           = LoggerFactory.getLogger(ExportAnalysisDataAction.class);

  public ExportAnalysisDataAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.exportanalysisdata"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.exportanalysisdata.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    // open the log download window
    Path file = null;
    try {
      String path = TmmProperties.getInstance().getProperty("exportlogs.path", "");
      file = TmmUIHelper.saveFile(TmmResourceBundle.getString("BugReport.savelogs"), path, "tmm_data.zip",
          new FileNameExtensionFilter("Zip files", ".zip"));
      if (file != null) {
        MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        writeLogsFile(file.toFile());
        TmmProperties.getInstance().putProperty("exportlogs.path", file.toAbsolutePath().toString());
        MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

      }
    }
    catch (Exception ex) {
      LOGGER.error("Could not write logs.zip: {}", ex.getMessage());
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, file != null ? file.toString() : "", "message.erroropenfile",
          new String[] { ":", ex.getLocalizedMessage() }));
    }
  }

  private void writeLogsFile(File file) throws Exception {
    List<Path> extraFiles = new ArrayList<>();
    // create data exports
    extraFiles.add(exportMovieDatasources());
    extraFiles.add(exportTvShowDatasources());

    // create zip
    ZipParameters zipParameters = createZipParameters();

    try (FileOutputStream os = new FileOutputStream(file); ZipOutputStream zos = ZipArchiveHelper.getInstance().createEncryptedZipOutputStream(os)) {
      // attach logs
      List<Path> logs = Utils.listFiles(Paths.get(Globals.LOG_FOLDER));
      for (Path logFile : logs) {
        try (InputStream in = Files.newInputStream(logFile)) {
          zipParameters.setFileNameInZip("logs/" + logFile.getFileName());

          zos.putNextEntry(zipParameters);
          IOUtils.copy(in, zos);
          zos.closeEntry();
        }
        catch (Exception e) {
          LOGGER.warn("unable to attach {} - {}", logFile, e.getMessage());
        }
      }

      // attach config files, but not DB
      File[] data = new File(Globals.DATA_FOLDER).listFiles((directory, filename) -> {
        return !filename.matches(".*\\.db$") && !filename.matches(".*\\.lic$"); // not DB and license
      });
      if (data != null) {
        for (File dataFile : data) {
          try (InputStream in = Files.newInputStream(dataFile.toPath())) {
            zipParameters.setFileNameInZip("data/" + dataFile.getName());

            zos.putNextEntry(zipParameters);
            IOUtils.copy(in, zos);
            zos.closeEntry();
          }
          catch (Exception e) {
            LOGGER.warn("unable to attach {} - {}", dataFile.getName(), e.getMessage());
          }
        }
      }

      // attach extra files
      for (Path extraFile : extraFiles) {
        try (InputStream in = Files.newInputStream(extraFile)) {
          zipParameters.setFileNameInZip(extraFile.getFileName().toString());

          zos.putNextEntry(zipParameters);
          IOUtils.copy(in, zos);
          zos.closeEntry();
        }
        catch (Exception e) {
          LOGGER.warn("unable to attach {} - {}", extraFile.getFileName(), e.getMessage());
        }

        Utils.deleteFileSafely(extraFile);
      }
    }
  }

  private ZipParameters createZipParameters() {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setEncryptFiles(true);

    return zipParameters;
  }

  private Path exportMovieDatasources() {
    Path exportFile = Paths.get(Utils.getTempFolder(), "tmm_moviefiles.zip");
    try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(exportFile.toFile())) {
      for (Movie movie : MovieModuleManager.getInstance().getMovieList().getMovies()) {
        Path datasource = Paths.get(movie.getDataSource());
        for (MediaFile mf : movie.getMediaFiles()) {
          String rel = Utils.relPath(datasource, mf.getFileAsPath());
          ZipArchiveEntry entry = new ZipArchiveEntry(datasource.getFileName().toString() + File.separatorChar + rel);
          archive.putArchiveEntry(entry);
          archive.closeArchiveEntry();
        }
      }
      archive.finish();
    }
    catch (Exception e) {
      LOGGER.error("Failed to create zip file: {}", e.getMessage()); // NOSONAR
    }
    return exportFile;
  }

  private Path exportTvShowDatasources() {
    Path exportFile = Paths.get(Utils.getTempFolder(), "tmm_tvshowfiles.zip");
    try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(exportFile.toFile())) {
      for (TvShow show : TvShowModuleManager.getInstance().getTvShowList().getTvShows()) {
        Path datasource = Paths.get(show.getDataSource());
        List<MediaFile> mfs = show.getMediaFiles();
        mfs.addAll(show.getSeasonMediaFiles());
        mfs.addAll(show.getEpisodesMediaFiles());
        for (MediaFile mf : mfs) {
          String rel = Utils.relPath(datasource, mf.getFileAsPath());
          ZipArchiveEntry entry = new ZipArchiveEntry(datasource.getFileName().toString() + File.separatorChar + rel);
          archive.putArchiveEntry(entry);
          archive.closeArchiveEntry();
        }
      }
      archive.finish();
    }
    catch (Exception e) {
      LOGGER.error("Failed to create zip file: {}", e.getMessage()); // NOSONAR
    }
    return exportFile;
  }
}
