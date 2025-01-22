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
package org.tinymediamanager.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.scraper.http.InMemoryCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.images.TmmSvgIcon;
import org.tinymediamanager.ui.thirdparty.ShadowRenderer;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.madgag.gif.fmsware.GifDecoder;

/**
 * The Class ImageLabel.
 * 
 * @author Manuel Laggner
 */
public class ImageLabel extends JComponent {
  public enum Position {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
  }

  private static final Color        EMPTY_BACKGROUND_COLOR = new Color(141, 165, 179);

  private static final TmmSvgIcon   NO_IMAGE               = createNoImageIcon();
  private static final Dimension    EMPTY_SIZE             = new Dimension(0, 0);
  private static final int          SHADOW_SIZE            = 8;
  protected byte[]                  originalImageBytes;
  protected Dimension               originalImageSize      = EMPTY_SIZE;
  protected Image                   scaledImage;
  protected ImageIcon               animatedGif;

  protected String                  imageUrl;
  protected String                  imagePath;

  protected Position                position               = Position.TOP_LEFT;
  protected boolean                 drawBorder;
  protected boolean                 drawFullWidth;
  protected boolean                 drawShadow;

  protected boolean                 enabledLightbox        = false;
  protected boolean                 preferCache            = true;
  protected boolean                 isLightBox             = false;
  protected float                   desiredAspectRatio     = 0f;
  protected boolean                 cacheUrl               = false;
  protected boolean                 scaleUpIfTooSmall      = true;

  protected ShadowRenderer          shadowRenderer;
  protected SwingWorker<Void, Void> worker                 = null;
  protected MouseListener           lightboxListener       = null;

  protected List<MouseListener>     boundedMouseListeners  = new ArrayList<>();
  protected double                  imageScaleX            = 1;
  protected double                  imageScaleY            = 1;

  private static TmmSvgIcon createNoImageIcon() {

    try {
      // create the icon
      URI uri = IconManager.class.getResource("images/svg/image.svg").toURI();
      return new TmmSvgIcon(uri);
    }
    catch (Exception e) {
      return null;
    }
  }

  public ImageLabel() {
    this(true, false);
  }

  public ImageLabel(boolean drawBorder) {
    this(drawBorder, false);
  }

  public ImageLabel(boolean drawBorder, boolean drawFullWidth) {
    this(drawBorder, drawFullWidth, false);
  }

  public ImageLabel(boolean drawBorder, boolean drawFullWidth, boolean drawShadow) {
    super();
    this.drawBorder = drawBorder;
    this.drawFullWidth = drawFullWidth;
    this.drawShadow = drawShadow;

    if (drawShadow) {
      this.shadowRenderer = new ShadowRenderer(8, 0.3f, Color.BLACK);
    }

    addMouseListener(new BoundedMouseListener());
  }

  /**
   * Register a {@link MouseListener} that will only be invoked when the mouse event occurs inside the bounds of the rendered image.
   */
  public void addBoundedMouseListener(MouseListener listener) {
    boundedMouseListeners.add(listener);
  }

  /**
   * Unregister a {@link MouseListener} that was previously registered with {@link #addBoundedMouseListener}.
   */
  public void removeBoundedMouseListener(MouseListener listener) {
    boundedMouseListeners.remove(listener);
  }

  public void setOriginalImage(byte[] originalImageBytes) {
    setImageBytes(originalImageBytes);
    recreateScaledImageIfNeeded(0, 0, this.getSize().width, this.getSize().height);
    repaint();
  }

  protected void setImageBytes(byte[] bytes) {
    originalImageBytes = bytes;
  }

  protected void createScaledImage(byte[] originalImageBytes, int width, int height) throws Exception {
    // check if this file is a gif
    GifDecoder decoder = new GifDecoder();
    int status = decoder.read(new ByteArrayInputStream(originalImageBytes));

    if (status == GifDecoder.STATUS_OK && decoder.getFrameCount() > 1) {
      // this is an animated gif
      animatedGif = new ImageIcon(originalImageBytes);
      originalImageSize = new Dimension(decoder.getFrameSize().width, decoder.getFrameSize().height);
      scaledImage = animatedGif.getImage();
    }
    else {
      // this is just a normal pic
      BufferedImage originalImage = ImageUtils.createImage(originalImageBytes);
      originalImageSize = new Dimension(originalImage.getWidth(), originalImage.getHeight());

      if (width < 1000 || height < 1000) {
        // scale fast
        scaledImage = Scalr.resize(originalImage, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, width, height, Scalr.OP_ANTIALIAS);
      }
      else {
        // scale good
        scaledImage = Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.AUTOMATIC, width, height, Scalr.OP_ANTIALIAS);
      }
      originalImage.flush();
      animatedGif = null;
    }
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String newValue) {
    String oldValue = this.imagePath;

    if (StringUtils.isNotEmpty(oldValue) && oldValue.equals(newValue)) {
      return;
    }

    this.imagePath = newValue;
    firePropertyChange("imagePath", oldValue, newValue);

    // stop previous worker
    if (worker != null && !worker.isDone()) {
      worker.cancel(true);
    }

    clearImageData();

    if (StringUtils.isBlank(newValue)) {
      this.repaint();
      return;
    }

    // load image in separate worker -> performance
    worker = new ImageLoader(this.imagePath, this.getSize());
    worker.execute();
    this.repaint();
  }

  public void clearImage() {
    imagePath = "";
    imageUrl = "";

    if (worker != null && !worker.isDone()) {
      worker.cancel(true);
    }
    clearImageData();
    this.repaint();
  }

  protected void clearImageData() {
    animatedGif = null;
    scaledImage = null;
    originalImageBytes = null;
    originalImageSize = EMPTY_SIZE;
    firePropertyChange("originalImageSize", null, 0);
    firePropertyChange("originalImageBytes", null, new byte[] {});
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String newValue) {
    String oldValue = this.imageUrl;
    this.imageUrl = newValue;
    firePropertyChange("imageUrl", oldValue, newValue);

    // stop previous worker
    if (worker != null && !worker.isDone()) {
      worker.cancel(true);
    }

    clearImageData();

    if (StringUtils.isEmpty(newValue)) {
      this.repaint();
      return;
    }

    // fetch image in separate worker -> performance
    worker = new ImageFetcher(imageUrl, this.getSize());
    worker.execute();
    this.repaint();
  }

  public void setDesiredAspectRatio(float desiredAspectRatio) {
    this.desiredAspectRatio = desiredAspectRatio;
  }

  public float getDesiredAspectRatio() {
    return desiredAspectRatio;
  }

  public void setScaleUpIfTooSmall(boolean scaleUpIfTooSmall) {
    this.scaleUpIfTooSmall = scaleUpIfTooSmall;
  }

  /**
   * get a byte array of the original image.<br/>
   * WARNING: this array is only filled _after_ the image has been loaded!
   *
   * @return a byte array of the original (not rescaled) image
   */
  public byte[] getOriginalImageBytes() {
    return originalImageBytes;
  }

  /**
   * get the {@link Dimension} of the original image.<br/>
   * WARNING: the {@link Dimension} is only filled _after_ the image has been loaded!
   *
   * @return the {@link Dimension} original (not rescaled) image
   */
  public Dimension getOriginalImageSize() {
    return originalImageSize;
  }

  @Override
  public Dimension getPreferredSize() {

    if (originalImageSize != EMPTY_SIZE) {
      int parentWidth = getParent().getWidth();
      int parentHeight = getParent().getHeight();

      if (scaleUpIfTooSmall || parentWidth < originalImageSize.width || parentHeight < originalImageSize.height) {
        // we maximize the image regardless of its size or if it is bigger than the parent
        return new Dimension(getParent().getWidth(),
            (int) ((getParent().getWidth() - getShadowSize()) / (float) originalImageSize.width * originalImageSize.height) + getShadowSize());
      }
      else {
        // we wont scale up
        return new Dimension(originalImageSize.width, originalImageSize.height + getShadowSize());
      }

    }

    if (desiredAspectRatio == 0) {
      // no desired aspect ratio; get the JLabel's preferred size
      return super.getPreferredSize();
    }
    else {
      return new Dimension(getParent().getWidth(), (int) ((getParent().getWidth() - getShadowSize()) / desiredAspectRatio) + 1 + getShadowSize());
    }
  }

  private int getShadowSize() {

    if (drawShadow) {
      return SHADOW_SIZE;
    }
    return 0;
  }

  /**
   * This is overridden to return false if the current image is not equal to the passed in Image <code>img</code>.
   *
   * @see java.awt.image.ImageObserver
   * @see java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int, int)
   */
  @Override
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {

    if (!isShowing() || scaledImage != img) {
      return false;
    }

    return super.imageUpdate(img, infoflags, x, y, w, h);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2d = (Graphics2D) g.create();

    try {
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      imageScaleX = g2d.getTransform().getScaleX();
      imageScaleY = g2d.getTransform().getScaleY();

      if (scaledImage != null) {
        int scaledImageWidth = scaledImage.getWidth(null);
        int scaledImageHeight = scaledImage.getHeight(null);

        // calculate new height/width
        Rectangle rectangle = new Rectangle();

        if (drawBorder && !drawFullWidth && !drawShadow) {
          Point size = ImageUtils.calculateSize(getMaxWidth() - 8, getMaxHeight() - 8, originalImageSize.width, originalImageSize.height, true);

          // calculate offsets
          if (position == Position.TOP_RIGHT || position == Position.BOTTOM_RIGHT) {
            rectangle.x = this.getWidth() - size.x - 8;
          }

          if (position == Position.BOTTOM_LEFT || position == Position.BOTTOM_RIGHT) {
            rectangle.y = this.getHeight() - size.y - 8;
          }

          if (position == Position.CENTER) {
            rectangle.x = (this.getWidth() - size.x - 8) / 2;
            rectangle.y = (this.getHeight() - size.y - 8) / 2;
          }

          rectangle.width = size.x;
          rectangle.height = size.y;

          g2d.setColor(Color.BLACK);
          g2d.drawRect(rectangle.x, rectangle.y, size.x + 7, size.y + 7);
          g2d.setColor(Color.WHITE);
          g2d.fillRect(rectangle.x + 1, rectangle.y + 1, size.x + 6, size.y + 6);

          Rectangle hiDpi = scaleHiDpi(g2d.getTransform(), rectangle);

          // when the image size differs too much - reload and rescale the original image
          recreateScaledImageIfNeeded(scaledImageWidth, scaledImageHeight, hiDpi.width, hiDpi.height);

          Rectangle drawRectangle = new Rectangle(rectangle);
          drawRectangle.x += 4;
          drawRectangle.y += 4;

          drawImageAtScale1x(scaledImage, g2d, scaleHiDpi(g2d.getTransform(), drawRectangle));
        }
        else if (drawShadow && !drawFullWidth) {
          Point size = ImageUtils.calculateSize(this.getMaxWidth() - SHADOW_SIZE, this.getMaxHeight() - SHADOW_SIZE, originalImageSize.width,
              originalImageSize.height, true);

          rectangle.width = size.x;
          rectangle.height = size.y;

          Rectangle hiDpi = scaleHiDpi(g2d.getTransform(), rectangle);

          // when the image size differs too much - reload and rescale the original image
          recreateScaledImageIfNeeded(scaledImageWidth, scaledImageHeight, hiDpi.width, hiDpi.height);

          // did the image reset to null?
          if (scaledImage instanceof BufferedImage) {
            // create shadow
            BufferedImage shadowImage = shadowRenderer.createShadow((BufferedImage) scaledImage);

            Rectangle shadowRectangle = new Rectangle(rectangle);
            shadowRectangle.x = SHADOW_SIZE;
            shadowRectangle.y = SHADOW_SIZE;

            // draw shadow
            drawImageAtScale1x(shadowImage, g2d, scaleHiDpi(g2d.getTransform(), shadowRectangle));
            shadowImage.flush();
          }

          // draw image
          drawImageAtScale1x(scaledImage, g2d, hiDpi);
        }
        else {
          Point size;

          if (drawFullWidth) {
            size = new Point(this.getMaxWidth(), this.getMaxWidth() * originalImageSize.height / originalImageSize.width);
          }
          else {
            size = ImageUtils.calculateSize(this.getMaxWidth(), this.getMaxHeight(), originalImageSize.width, originalImageSize.height, true);
          }

          // calculate offsets
          if (position == Position.TOP_RIGHT || position == Position.BOTTOM_RIGHT) {
            rectangle.x = this.getWidth() - size.x;
          }

          if (position == Position.BOTTOM_LEFT || position == Position.BOTTOM_RIGHT) {
            rectangle.y = this.getHeight() - size.y;
          }

          if (position == Position.CENTER) {
            rectangle.x = (this.getWidth() - size.x) / 2;
            rectangle.y = (this.getHeight() - size.y) / 2;
          }

          rectangle.width = size.x;
          rectangle.height = size.y;

          Rectangle hiDpi = scaleHiDpi(g2d.getTransform(), rectangle);

          // when the image size differs too much - reload and rescale the original image
          recreateScaledImageIfNeeded(scaledImageWidth, scaledImageHeight, hiDpi.width, hiDpi.height);

          drawImageAtScale1x(scaledImage, g2d, hiDpi);
        }
      }
      // do not draw the "no image found" icon if the worker is loading or in lightbox usage
      else if (!isLoading() && !isLightBox) {
        // nothing to draw; draw the _no image found_ indicator
        Rectangle rectangle = new Rectangle();

        if (drawShadow) {
          rectangle.width = this.getMaxWidth() - 8;
          rectangle.height = this.getMaxHeight() - 8;
        }
        else {
          rectangle.width = this.getMaxWidth();
          rectangle.height = this.getMaxHeight();
        }

        Rectangle hiDpi = scaleHiDpi(g2d.getTransform(), rectangle);

        // calculate the optimal font size; the pt is about 0.75 * the needed px
        // we draw that icon at max 50% of the available space
        int imageSize = (int) (Math.min(hiDpi.width, hiDpi.height) * 0.5 / 0.75);

        // draw the _no image found_ icon
        if (NO_IMAGE != null) {
          Graphics2D g2 = null;
          try {
            BufferedImage tmp = new BufferedImage(hiDpi.width, hiDpi.height, BufferedImage.TYPE_INT_ARGB);
            g2 = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(tmp);

            FlatUIUtils.setRenderingHints(g2);

            g2.setColor(EMPTY_BACKGROUND_COLOR);
            g2.fillRect(0, 0, hiDpi.width, hiDpi.height);

            NO_IMAGE.setPreferredSize(new Dimension(imageSize, imageSize));
            NO_IMAGE.setColor(UIManager.getColor("Panel.background"));

            if (drawShadow) {
              BufferedImage shadowImage = shadowRenderer.createShadow(tmp);

              Rectangle shadowRectangle = new Rectangle(rectangle);
              shadowRectangle.x = 8;
              shadowRectangle.y = 8;

              // draw shadow
              drawImageAtScale1x(shadowImage, g2d, scaleHiDpi(g2d.getTransform(), shadowRectangle));
              shadowImage.flush();
            }

            g2.drawImage(NO_IMAGE.getImage(), (hiDpi.width - NO_IMAGE.getIconWidth()) / 2, (hiDpi.height - NO_IMAGE.getIconHeight()) / 2, null);

            // draw image
            drawImageAtScale1x(tmp, g2d, hiDpi);
            tmp.flush();
          }
          finally {
            if (g2 != null) {
              g2.dispose();
            }
          }
        }
      }
    }
    catch (Exception e) {
      // just catch to do not crash here
      Rectangle rectangle = new Rectangle();
      rectangle.width = this.getMaxWidth();
      rectangle.height = this.getMaxHeight();

      Rectangle hiDpi = scaleHiDpi(g2d.getTransform(), rectangle);
      g2d.setColor(EMPTY_BACKGROUND_COLOR);
      g2d.fillRect(0, 0, hiDpi.width, hiDpi.height);
    }
    finally {
      g2d.dispose();
    }
  }

  private int getMaxWidth() {

    if (!scaleUpIfTooSmall && originalImageSize != null) {
      return Math.min(getWidth(), originalImageSize.width);
    }
    return getWidth();
  }

  private int getMaxHeight() {

    if (!scaleUpIfTooSmall && originalImageSize != null) {
      return Math.min(originalImageSize.height, getHeight());
    }
    return getHeight();
  }

  /**
   * Scales a rectangle in the same way as the JRE does in sun.java2d.pipe.PixelToParallelogramConverter.fillRectangle(), which is used by
   * Graphics.fillRect().
   */
  private Rectangle scaleHiDpi(AffineTransform transform, Rectangle rectangle) {

    // no need to scale
    if (transform.getScaleX() == 1 && transform.getScaleY() == 1) {
      return new Rectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    double dx1 = transform.getScaleX();
    double dy2 = transform.getScaleY();
    double px = rectangle.x * dx1 + transform.getTranslateX();
    double py = rectangle.y * dy2 + transform.getTranslateY();
    dx1 *= rectangle.width;
    dy2 *= rectangle.height;

    int newx = (int) Math.floor(normalize(px));
    int newy = (int) Math.floor(normalize(py));
    dx1 = normalize(px + dx1) - newx;
    dy2 = normalize(py + dy2) - newy;

    return new Rectangle(newx, newy, (int) dx1, (int) dy2);
  }

  private void drawImageAtScale1x(Image image, Graphics2D g2D, Rectangle rectangle) {
    // save original transform
    AffineTransform transform = g2D.getTransform();

    try {
      // unscale to factor 1.0 and move origin (to whole numbers)
      if (transform.getScaleX() != 1 || transform.getScaleY() != 1) {
        g2D.setTransform(new AffineTransform(1, 0, 0, 1, 0, 0));
      }

      // paint
      g2D.drawImage(image, rectangle.x, rectangle.y, rectangle.width, rectangle.height, this);
    }
    finally {
      // restore original transform
      g2D.setTransform(transform);
    }
  }

  private static double normalize(double value) {
    return Math.floor(value + 0.25) + 0.25;
  }

  protected boolean isLoading() {
    return worker != null && !worker.isDone();
  }

  private void recreateScaledImageIfNeeded(int originalWidth, int originalHeight, int newWidth, int newHeight) {

    if (animatedGif != null) {
      scaledImage = animatedGif.getImage();
    }
    else if (originalWidth < 20 || originalHeight < 20 || (newWidth * 0.8f > originalWidth) || (originalWidth > newWidth * 1.2f)
        || (newHeight * 0.8f > originalHeight) || (originalHeight > newHeight * 1.2f)) {

      try {
        createScaledImage(originalImageBytes, newWidth, newHeight);
      }
      catch (Exception e) {
        scaledImage = null;
      }
    }
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public void enableLightbox() {
    this.enabledLightbox = true;

    if (lightboxListener == null) {
      lightboxListener = new ImageLabelClickListener();
      addMouseListener(lightboxListener);
    }
  }

  public void disableLightbox() {
    this.enabledLightbox = false;

    if (lightboxListener != null) {
      removeMouseListener(lightboxListener);
      lightboxListener = null;
    }
  }

  public void setPreferCache(boolean preferCache) {
    this.preferCache = preferCache;
  }

  public void setIsLightbox(boolean value) {
    this.isLightBox = value;
  }

  /**
   * should the url get cached for this session
   * 
   * @param cacheUrl
   *          true if the image behind this url should be cache in this session
   */
  public void setCacheUrl(boolean cacheUrl) {
    this.cacheUrl = cacheUrl;
  }

  /*
   * inner class for downloading online images
   */
  protected class ImageFetcher extends SwingWorker<Void, Void> {
    private final String    imageUrl;
    private final Dimension newSize;

    public ImageFetcher(String imageUrl, Dimension newSize) {
      this.imageUrl = imageUrl;
      this.newSize = newSize;
    }

    @Override
    protected Void doInBackground() {
      try {
        // if we want to use the cache, fetch this url via the image cache
        if (preferCache) {
          Path cachedFile = ImageCache.getCachedFile(imageUrl);

          if (cachedFile != null && Files.exists(cachedFile)) {

            try {
              byte[] bytes = Files.readAllBytes(cachedFile);
              clearImageData();
              setImageBytes(bytes);
              recreateScaledImageIfNeeded(0, 0, newSize.width, newSize.height);
              return null;
            }
            catch (Exception e) {
              // okay, we got an exception here - set the image path to empty to avoid an endless try-to-reload
              ImageLabel.this.imagePath = "";
              clearImageData();
            }
          }
        }

        // no image cache? just fetch it directly
        Url url;

        if (cacheUrl) {
          url = new InMemoryCachedUrl(imageUrl);
        }
        else {
          url = new Url(imageUrl);
        }
        byte[] bytes = url.getBytesWithRetry(2);
        clearImageData();
        setImageBytes(bytes);
        recreateScaledImageIfNeeded(0, 0, newSize.width, newSize.height);
      }
      catch (Exception e) {
        ImageLabel.this.imageUrl = "";
        clearImageData();
      }

      return null;
    }

    @Override
    protected void done() {
      if (isCancelled() || !ImageLabel.this.imageUrl.equals(imageUrl)) {
        ImageLabel.this.imageUrl = "";
        clearImageData();
      }
      else {
        // fire events
        ImageLabel.this.firePropertyChange("originalImageBytes", null, originalImageBytes);
        ImageLabel.this.firePropertyChange("originalImageSize", null, originalImageSize);
      }

      revalidate();
      repaint();
    }
  }

  /*
   * inner class for loading local images
   */
  protected class ImageLoader extends SwingWorker<Void, Void> {
    private final String    imagePath;
    private final Dimension newSize;

    public ImageLoader(String imagePath, Dimension newSize) {
      this.imagePath = imagePath;
      this.newSize = newSize;
    }

    @Override
    protected Void doInBackground() {
      Path file = null;

      // we prefer reading it from the cache
      if (preferCache) {
        file = ImageCache.getCachedFile(Paths.get(imagePath));
      }

      // not in the cache - read it from the path
      if (file == null) {
        file = Paths.get(imagePath);
      }

      // not available in the path and not preferred from the cache..
      // well just try to read it from the cache
      if ((!Files.exists(file)) && !preferCache) {
        file = ImageCache.getCachedFile(Paths.get(imagePath));
      }

      if (file != null && Files.exists(file)) {

        try {
          byte[] bytes = Files.readAllBytes(file);
          clearImageData();
          setImageBytes(bytes);
          recreateScaledImageIfNeeded(0, 0, newSize.width, newSize.height);
        }
        catch (Exception e) {
          // okay, we got an exception here - set the image path to empty to avoid an endless try-to-reload
          ImageLabel.this.imagePath = "";
          clearImageData();
        }
      }

      return null;
    }

    @Override
    protected void done() {

      if (isCancelled() || !ImageLabel.this.imagePath.equals(imagePath)) {
        ImageLabel.this.imagePath = "";
        clearImageData();
      }
      else {
        // fire events
        ImageLabel.this.firePropertyChange("originalImageBytes", null, originalImageBytes);
        ImageLabel.this.firePropertyChange("originalImageSize", null, originalImageSize);
      }

      revalidate();
      repaint();
    }
  }

  /*
   * click listener for creating a lightbox effect
   */
  private class ImageLabelClickListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent arg0) {

      if (arg0.getClickCount() == 1 && scaledImage != null) {
        MainWindow.getInstance().createLightbox(getImagePath(), getImageUrl());
      }
    }
  }

  /**
   * A {@link MouseAdapter} implementation that proxies events to child listeners **IFF** the event occurs inside the bounds of the rendered image.
   */
  private class BoundedMouseListener extends MouseAdapter {
    private int scale(int dimension, double scale) {
      return (int) (dimension / (scale == 0 ? 1 : scale));
    }

    private boolean contains(MouseEvent e) {
      if (isLoading() || !isShowing() || scaledImage == null || e.getClickCount() != 1) {
        return false;
      }

      try {
        int width = scale(scaledImage.getWidth(null), imageScaleX);
        int height = scale(scaledImage.getHeight(null), imageScaleY);
        Rectangle rect = new Rectangle(0, 0, width, height);
        Point mouseLocationRelativeToLabel = e.getPoint();
        return rect.contains(mouseLocationRelativeToLabel);
      }
      catch (Throwable t) {
        // Ignore
      }

      return false;
    }

    private void ifContains(MouseEvent e, Consumer<MouseListener> action) {
      if (contains(e)) {
        boundedMouseListeners.forEach(action);
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      ifContains(e, (listener) -> listener.mouseEntered(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
      ifContains(e, (listener) -> listener.mouseExited(e));
    }

    @Override
    public void mousePressed(MouseEvent e) {
      ifContains(e, (listener) -> listener.mousePressed(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      ifContains(e, (listener) -> listener.mouseReleased(e));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      ifContains(e, (listener) -> listener.mouseClicked(e));
    }
  }
}
