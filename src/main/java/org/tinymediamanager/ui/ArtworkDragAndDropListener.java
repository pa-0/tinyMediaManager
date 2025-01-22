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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.components.ImageLabel;

/**
 * The class {@link ArtworkDragAndDropListener} is used to offer file (artwork) drag and drop support for some UI components
 *
 * @author acdvorak, Manuel Laggner
 */
public class ArtworkDragAndDropListener implements DropTargetListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ArtworkDragAndDropListener.class);

  private final ImageLabel    imageLabel;

  public ArtworkDragAndDropListener(ImageLabel imageLabel) {
    this.imageLabel = imageLabel;
  }

  private File[] getImageFiles(DropTargetDragEvent dtde) {
    try {
      return getImageFilesFromTransferData(dtde.getTransferable());
    }
    catch (Throwable t) {
      LOGGER.error("Failed to retrieve image files from drag-and-drop event: ", t);
    }
    return new File[0];
  }

  private File[] getImageFiles(DropTargetDropEvent dtde) {
    try {
      return getImageFilesFromTransferData(dtde.getTransferable());
    }
    catch (Throwable t) {
      LOGGER.error("Failed to retrieve image files from drag-and-drop event: ", t);
    }
    return new File[0];
  }

  private File[] getImageFilesFromTransferData(Transferable transferable) {
    try {
      Object transferData = transferable.getTransferData(DataFlavor.javaFileListFlavor);
      if (!(transferData instanceof java.util.List)) {
        return new File[0];
      }

      var filter = new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "bmp", "gif", "tbn", "webp");

      @SuppressWarnings("unchecked")
      File[] imageFiles = ((java.util.List<File>) transferData).stream().filter(file -> {
        return filter.accept(file) && Utils.isRegularFile(file.getAbsoluteFile().toPath());
      }).toArray(File[]::new);

      return imageFiles;
    }
    catch (Throwable t) {
      LOGGER.error("Failed to retrieve image files from drag-and-drop event: ", t);
    }

    return new File[0];
  }

  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
    // This method MUST be called BEFORE extracting any files from the DND event, or an error will be thrown.
    dtde.acceptDrag(DnDConstants.ACTION_COPY);

    File[] imageFiles = getImageFiles(dtde);
    if (imageFiles.length > 0) {
      // TODO(acdvorak): Display green drop target icon or similar
    }
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
    // This method MUST be called BEFORE extracting any files from the DND event, or an error will be thrown.
    dtde.acceptDrag(DnDConstants.ACTION_COPY);

    File[] imageFiles = getImageFiles(dtde);
    if (imageFiles.length > 0) {
      // TODO(acdvorak): Display green drop target icon or similar
    }
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
    // TODO(acdvorak): Hide green drop target icon or similar
  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
    try {
      // This method MUST be called BEFORE extracting any files from the DND event, or an error will be thrown.
      dtde.acceptDrop(DnDConstants.ACTION_COPY);

      File[] imageFiles = getImageFiles(dtde);
      if (imageFiles.length == 0) {
        return;
      }

      File file = imageFiles[0];
      String fileName = file.getAbsolutePath();
      imageLabel.clearImage();
      imageLabel.setImageUrl("file:/" + fileName);
    }
    catch (Throwable t) {
      LOGGER.error("An error occurred while processing the dropped file: ", t);
    }
  }
}
