/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.thirdparty.ImageLoader;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.thirdparty.imageviewer.ImageViewer;

import net.miginfocom.swing.MigLayout;

/**
 * The class ImagePreviewDialog. To display a preview of the image in the image chooser
 * 
 * @author Manuel Laggner
 */
public class ImagePreviewDialog extends TmmDialog {
  private static final Logger LOGGER     = LoggerFactory.getLogger(ImagePreviewDialog.class);

  private final JPanel        imagePanel = new JPanel();
  private final ImageViewer   imgViewer  = new ImageViewer();

  private String              imageUrl;
  private String              imagePath;

  private byte[]              originalImageBytes;

  public ImagePreviewDialog(String urlToImage) {
    super(TmmResourceBundle.getString("image.show"), "imagePreview");
    init();

    this.imageUrl = urlToImage;
  }

  public ImagePreviewDialog(Path pathToImage) {
    super(TmmResourceBundle.getString("image.show"), "imagePreview");
    init();

    this.imagePath = pathToImage.toString();
  }

  private void init() {
    imgViewer.getPopupMenu().addSeparator();
    imgViewer.getPopupMenu().add(new SaveToDiskAction());

    {
      imagePanel.setLayout(new MigLayout("", "[300lp,grow]", "[300lp,grow]"));
      getContentPane().add(imagePanel);
    }
    {
      JButton closeButton = new JButton(TmmResourceBundle.getString("Button.close"));
      closeButton.addActionListener(e -> setVisible(false));
      addDefaultButton(closeButton);
    }
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      // get max screen size on multi screen setups
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      int width = gd.getDisplayMode().getWidth();
      int height = gd.getDisplayMode().getHeight();
      setMaximumSize(new Dimension(width, height));

      // run async to avoid strange loading artefacts
      SwingWorker<Void, Void> worker = new SwingWorker<>() {
        @Override
        protected Void doInBackground() throws Exception {
          if (StringUtils.isNotBlank(imagePath)) {
            Path file = Paths.get(imagePath);
            originalImageBytes = Files.readAllBytes(file);
          }
          else if (StringUtils.isNotBlank(imageUrl)) {
            try {
              Url url = new Url(imageUrl);
              originalImageBytes = url.getBytesWithRetry(5);
            }
            catch (Exception e) {
              LOGGER.error("could not load image - {}", e.getMessage());
            }
          }

          if (originalImageBytes.length > 0) {
            try {
              imgViewer.setImage(ImageLoader.createImage(originalImageBytes));
              imagePanel.removeAll();
              imagePanel.add(imgViewer.getComponent(), "cell 0 0, center, grow");
              imagePanel.invalidate();
              imagePanel.repaint();

              if (imgViewer.getImage().getWidth() > 0 && imgViewer.getImage().getHeight() > 0) {
                setTitle(
                    TmmResourceBundle.getString("image.show") + " - " + imgViewer.getImage().getWidth() + "x" + imgViewer.getImage().getHeight());
              }
            }
            catch (Exception e) {
              LOGGER.error("could not load image - '{}'", e.getMessage());
            }
          }

          return null;
        }
      };
      worker.execute();

      pack();
      setLocationRelativeTo(MainWindow.getInstance());
      super.setVisible(true);
    }
    else {
      super.setVisible(false);
      dispose();
    }
  }

  private class SaveToDiskAction extends TmmAction {

    private SaveToDiskAction() {
      putValue(LARGE_ICON_KEY, IconManager.EXPORT);
      putValue(SMALL_ICON, IconManager.EXPORT);
      putValue(NAME, TmmResourceBundle.getString("image.savetodisk"));
    }

    @Override
    protected void processAction(ActionEvent e) {
      SwingUtilities.invokeLater(() -> {
        try {
          // open save to dialog
          String filename = "";
          if (StringUtils.isNotBlank(imagePath)) {
            filename = FilenameUtils.getBaseName(imagePath);
          }
          else if (StringUtils.isNotBlank(imageUrl)) {
            filename = FilenameUtils.getBaseName(imageUrl);
          }

          Path file = TmmUIHelper.saveFile(TmmResourceBundle.getString("image.savetodisk"), "", filename,
              new FileNameExtensionFilter("Image files", ".jpg", ".png", ".webp"));
          if (file != null) {
            try (FileOutputStream os = new FileOutputStream(file.toFile())) {
              IOUtils.write(originalImageBytes, os);
            }
          }
        }
        catch (Exception ex) {
          LOGGER.error("Could not save image file: {}", ex.getMessage());
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, "", "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
        }
      });
    }
  }
}
