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

package org.tinymediamanager.core;

import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.thirdparty.ImageLoader;

public class ImageUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

  private ImageUtils() {
    throw new IllegalAccessError();
  }

  /**
   * Scale image to fit in the given width.
   *
   * @param imageBytes
   *          the image bytes
   * @param width
   *          the width
   * @return the input stream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static InputStream scaleImage(byte[] imageBytes, int width) throws IOException {
    BufferedImage originalImage;
    try {
      originalImage = createImage(imageBytes);
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    }

    Point size = new Point();
    size.x = width;
    size.y = size.x * originalImage.getHeight() / originalImage.getWidth();

    // BufferedImage scaledImage = Scaling.scale(originalImage, size.x, size.y);
    BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, size.x, size.y, Scalr.OP_ANTIALIAS);
    originalImage.flush();

    ImageWriter imgWrtr = null;
    ImageWriteParam imgWrtrPrm = null;

    // here we have two different ways to create our thumb
    // a) a scaled down jpg/png (without transparency) which we have to modify since OpenJDK cannot call native jpg encoders
    // b) a scaled down png (with transparency) which we can store without any more modifying as png
    if (hasTransparentPixels(scaledImage)) {
      // transparent image -> png
      imgWrtr = ImageIO.getImageWritersByFormatName("png").next();
      imgWrtrPrm = imgWrtr.getDefaultWriteParam();

    }
    else {
      // non transparent image -> jpg
      // convert to rgb
      BufferedImage rgb = new BufferedImage(scaledImage.getWidth(), scaledImage.getHeight(), BufferedImage.TYPE_INT_RGB);
      ColorConvertOp xformOp = new ColorConvertOp(null);
      xformOp.filter(scaledImage, rgb);
      imgWrtr = ImageIO.getImageWritersByFormatName("jpg").next();
      imgWrtrPrm = imgWrtr.getDefaultWriteParam();
      imgWrtrPrm.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
      imgWrtrPrm.setCompressionQuality(0.80f);

      scaledImage = rgb;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageOutputStream output = ImageIO.createImageOutputStream(baos);
    imgWrtr.setOutput(output);
    IIOImage outputImage = new IIOImage(scaledImage, null, null);
    imgWrtr.write(null, outputImage, imgWrtrPrm);
    imgWrtr.dispose();
    scaledImage.flush();

    byte[] bytes = baos.toByteArray();

    output.flush();
    output.close();
    baos.close();

    return new ByteArrayInputStream(bytes);
  }

  /**
   * Scale image to fit in the given width.
   *
   * @param file
   *          the original image file
   * @param width
   *          the width
   * @return the input stream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static InputStream scaleImage(Path file, int width) throws IOException {
    BufferedImage originalImage;
    try {
      originalImage = createImage(file);
    }
    catch (Exception e) {
      throw new IOException(e.getMessage());
    }

    Point size = new Point();
    size.x = width;
    size.y = size.x * originalImage.getHeight() / originalImage.getWidth();

    // BufferedImage scaledImage = Scaling.scale(originalImage, size.x, size.y);
    BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, size.x, size.y, Scalr.OP_ANTIALIAS);
    originalImage.flush();

    ImageWriter imgWrtr = null;
    ImageWriteParam imgWrtrPrm = null;

    // here we have two different ways to create our thumb
    // a) a scaled down jpg/png (without transparency) which we have to modify since OpenJDK cannot call native jpg encoders
    // b) a scaled down png (with transparency) which we can store without any more modifying as png
    if (hasTransparentPixels(scaledImage)) {
      // transparent image -> png
      imgWrtr = ImageIO.getImageWritersByFormatName("png").next();
      imgWrtrPrm = imgWrtr.getDefaultWriteParam();

    }
    else {
      // non transparent image -> jpg
      // convert to rgb
      BufferedImage rgb = new BufferedImage(scaledImage.getWidth(), scaledImage.getHeight(), BufferedImage.TYPE_INT_RGB);
      ColorConvertOp xformOp = new ColorConvertOp(null);
      xformOp.filter(scaledImage, rgb);
      imgWrtr = ImageIO.getImageWritersByFormatName("jpg").next();
      imgWrtrPrm = imgWrtr.getDefaultWriteParam();
      imgWrtrPrm.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
      imgWrtrPrm.setCompressionQuality(0.80f);

      scaledImage = rgb;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageOutputStream output = ImageIO.createImageOutputStream(baos);
    imgWrtr.setOutput(output);
    IIOImage outputImage = new IIOImage(scaledImage, null, null);
    imgWrtr.write(null, outputImage, imgWrtrPrm);
    imgWrtr.dispose();
    scaledImage.flush();

    byte[] bytes = baos.toByteArray();

    output.flush();
    output.close();
    baos.close();

    return new ByteArrayInputStream(bytes);
  }

  static boolean hasTransparentPixels(BufferedImage image) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        int pixel = image.getRGB(x, y);
        if ((pixel >> 24) == 0x00) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * calculate a new size which fits into maxWidth and maxHeight
   *
   * @param maxWidth
   *          the maximum width of the result
   * @param maxHeight
   *          the maximum height of the result
   * @param originalWidth
   *          the width of the source
   * @param originalHeight
   *          the height of the source
   * @param respectFactor
   *          should we respect the aspect ratio?
   * @return the calculated new size
   */
  public static Point calculateSize(int maxWidth, int maxHeight, int originalWidth, int originalHeight, boolean respectFactor) {
    Point size = new Point();
    if (respectFactor) {
      // calculate on available height
      size.y = maxHeight;
      size.x = (int) (size.y * (double) originalWidth / (double) originalHeight);

      if (size.x > maxWidth) {
        // calculate on available height
        size.x = maxWidth;
        size.y = (int) (size.x * (double) originalHeight / (double) originalWidth);
      }
    }
    else {
      size.x = maxWidth;
      size.y = maxHeight;
    }
    return size;
  }

  public static BufferedImage createImage(byte[] imageData) throws IOException {
    try {
      // try to read with the fast implementation
      return createImage(Toolkit.getDefaultToolkit().createImage(imageData));
    }
    catch (Exception e) {
      // did not work? try with ImageIO
      try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
        return ImageIO.read(bis);
      }
    }
  }

  public static BufferedImage createImage(Path file) throws IOException {
    try {
      // try to read with the fast implementation
      return createImage(Toolkit.getDefaultToolkit().createImage(file.toFile().getAbsolutePath()));
    }
    catch (Exception e) {
      // did not work? try with ImageIO
      return ImageIO.read(file.toFile());
    }
  }

  public static BufferedImage createImage(Image img) {
    return ImageLoader.createImage(img);
  }

  public static Path downloadImage(String urlAsString, Path destinationFolder, String filename) throws Exception {
    return downloadImage(urlAsString, destinationFolder, filename, false, 0);
  }

  public static Path downloadImage(String urlAsString, Path destinationFolder, String filename, boolean rescale, int newWidth) throws Exception {

    // don't write jpeg -> write jpg
    if (FilenameUtils.getExtension(filename).equalsIgnoreCase("JPEG")) {
      filename = FilenameUtils.getBaseName(filename) + ".jpg";
    }

    Path destFile = destinationFolder.resolve(filename);

    // check if old and new file are the same (possible if you select it in the imagechooser)
    if (urlAsString.startsWith("file:")) {
      String newUrl = urlAsString.replace("file:/", "");
      Path file = Paths.get(newUrl);
      if (file.equals(destFile)) {
        return destFile;
      }
    }

    LOGGER.debug("downloading {} to {}", urlAsString, destFile);

    Path tempFile = null;
    try {
      long timestamp = System.currentTimeMillis();

      try {
        // create a temp file/folder inside the temp folder or tmm folder
        Path tempFolder = Paths.get(Utils.getTempFolder());
        if (!Files.exists(tempFolder)) {
          Files.createDirectory(tempFolder);
        }
        tempFile = tempFolder.resolve(filename + "." + timestamp + ".part"); // multi episode same file
      }
      catch (Exception e) {
        LOGGER.debug("could not write to temp folder: {}", e.getMessage());

        // could not create the temp folder somehow - put the files into the entity dir
        tempFile = destFile.resolveSibling(filename + "." + timestamp + ".part"); // multi episode same file
      }

      // fetch and store images
      Url url;
      try {
        url = new Url(urlAsString);
      }
      catch (Exception e) {
        LOGGER.error("downloading {} - {}", urlAsString, e.getMessage());
        throw e;
      }

      if (!rescale || newWidth == 0) {
        try (InputStream is = url.getInputStreamWithRetry(5); FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
          IOUtils.copy(is, outputStream);
          Utils.flushFileOutputStreamToDisk(outputStream);
        }
      }
      else {
        try (InputStream is = url.getInputStreamWithRetry(5);
            InputStream scaledIs = ImageUtils.scaleImage(IOUtils.toByteArray(is), newWidth);
            FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {

          IOUtils.copy(scaledIs, outputStream);
          Utils.flushFileOutputStreamToDisk(outputStream);
        }
      }

      // check if the file has been downloaded
      if (!Files.exists(tempFile) || Files.size(tempFile) == 0) {
        // cleanup the file
        FileUtils.deleteQuietly(tempFile.toFile());
        throw new IOException("0byte file downloaded: " + filename);
      }

      // delete new destination if existing
      Utils.deleteFileSafely(destFile);

      // move the temp file to the expected filename
      if (!Utils.moveFileSafe(tempFile, destFile)) {
        throw new IOException("renaming temp file failed: " + filename);
      }

      // give it a few milliseconds to finish writing on the file system
      Thread.sleep(150);
    }
    catch (AccessDeniedException e) {
      // propagate to UI by logging with error
      LOGGER.error("ACCESS DENIED (download artwork) - '{}'", e.getMessage());
      // re-throw
      throw e;
    }
    finally {
      // remove temp file
      if (tempFile != null && Files.exists(tempFile)) {
        Utils.deleteFileSafely(tempFile);
      }
    }

    return destFile;
  }

  /**
   * create a {@link java.awt.image.MultiResolutionImage} off the given {@link ImageIcon} with the @param baseHeight as @1<br/>
   * the image sizes will be
   * <ul>
   * <li>100%</li>
   * <li>125%</li>
   * <li>150%</li>
   * <li>175%</li>
   * <li>200%</li>
   * </ul>
   * 
   * @param original
   *          the original image to create the {@link java.awt.image.MultiResolutionImage} off
   * @param baseHeight
   *          the height for @1
   * @return the created {@link java.awt.image.MultiResolutionImage}
   */
  public static ImageIcon createMultiResolutionImage(ImageIcon original, int baseHeight) {
    Image[] images = new Image[5];

    images[0] = getScaledIcon(original, baseHeight).getImage();
    images[1] = getScaledIcon(original, (int) (baseHeight * 1.25f)).getImage();
    images[2] = getScaledIcon(original, (int) (baseHeight * 1.5f)).getImage();
    images[3] = getScaledIcon(original, (int) (baseHeight * 1.75f)).getImage();
    images[4] = getScaledIcon(original, baseHeight * 2).getImage();

    return new ImageIcon(new BaseMultiResolutionImage(images));
  }

  static ImageIcon getScaledIcon(ImageIcon original, int height) {
    int width = original.getIconWidth() / original.getIconHeight() * height;

    BufferedImage scaledImage = Scalr.resize(ImageUtils.createImage(original.getImage()), Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, width, height,
        Scalr.OP_ANTIALIAS);
    return new ImageIcon(scaledImage);
  }
}
