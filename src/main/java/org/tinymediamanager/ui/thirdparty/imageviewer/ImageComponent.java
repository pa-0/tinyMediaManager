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

package org.tinymediamanager.ui.thirdparty.imageviewer;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.ScrollPaneConstants;

/**
 * The component that displays the image itself.
 * 
 * @author Kazó Csaba, Manuel Laggner
 */
class ImageComponent extends JComponent {
  private final PaintManager          paintManager      = new PaintManager();
  private final Scroller              scroller          = new Scroller();
  private final PropertyChangeSupport propertyChangeSupport;
  private final ImageViewer           viewer;

  private BufferedImage               image;

  private ResizeStrategy              resizeStrategy    = ResizeStrategy.SHRINK_TO_FIT;
  private Object                      interpolationType = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
  private boolean                     pixelatedZoom     = false;
  private double                      zoomFactor        = 1;

  public ImageComponent(ImageViewer viewer, PropertyChangeSupport propertyChangeSupport) {
    this.viewer = viewer;
    this.propertyChangeSupport = propertyChangeSupport;
    setOpaque(true);
  }

  private boolean hasSize() {
    return getWidth() > 0 && getHeight() > 0;
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  public void setImage(BufferedImage newImage) {
    BufferedImage oldImage = image;
    image = newImage;

    // need to resize the panel area
    resizePanel();

    paintManager.notifyChanged();
    if (oldImage != newImage
        && (oldImage == null || newImage == null || oldImage.getWidth() != newImage.getWidth() || oldImage.getHeight() != newImage.getHeight())) {
      revalidate();
    }

    repaint();
    propertyChangeSupport.firePropertyChange("image", oldImage, newImage);
  }

  public BufferedImage getImage() {
    return image;
  }

  /**
   * Preforms all necessary actions to ensure that the viewer is resized to its proper size. It does that by invoking {@code validate()} on the
   * viewer's validateRoot. It also issues a {@code repaint()}.
   */
  private void resizeNow() {
    invalidate();

    // find the validate root; adapted from the package-private SwingUtilities.getValidateRoot
    Container root = null;
    Container c = this;
    for (; c != null; c = c.getParent()) {
      if (!c.isDisplayable() || c instanceof CellRendererPane) {
        return;
      }
      if (c.isValidateRoot()) {
        root = c;
        break;
      }
    }

    if (root == null)
      return;

    for (; c != null; c = c.getParent()) {
      if (!c.isDisplayable() || !c.isVisible()) {
        return;
      }
      if (c instanceof Window) {
        break;
      }
    }

    if (c == null) {
      return;
    }

    root.validate();
    repaint();
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    if (resizeStrategy == this.resizeStrategy) {
      return;
    }

    scroller.prepare();
    ResizeStrategy oldResizeStrategy = this.resizeStrategy;
    this.resizeStrategy = resizeStrategy;

    resizeNow();
    scroller.rescroll();

    propertyChangeSupport.firePropertyChange("resizeStrategy", oldResizeStrategy, resizeStrategy);
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setInterpolationType(Object type) {
    if (interpolationType == type) {
      return;
    }

    if (type != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR && type != RenderingHints.VALUE_INTERPOLATION_BILINEAR
        && type != RenderingHints.VALUE_INTERPOLATION_BICUBIC) {
      throw new IllegalArgumentException("Invalid interpolation type; use one of the RenderingHints constants");
    }

    Object old = this.interpolationType;
    this.interpolationType = type;

    paintManager.notifyChanged();
    repaint();
    propertyChangeSupport.firePropertyChange("interpolationType", old, type);
  }

  public Object getInterpolationType() {
    return interpolationType;
  }

  public void setPixelatedZoom(boolean pixelatedZoom) {
    if (pixelatedZoom == this.pixelatedZoom) {
      return;
    }

    this.pixelatedZoom = pixelatedZoom;

    paintManager.notifyChanged();
    repaint();
    propertyChangeSupport.firePropertyChange("pixelatedZoom", !pixelatedZoom, pixelatedZoom);
  }

  public boolean isPixelatedZoom() {
    return pixelatedZoom;
  }

  /** Returns the zoom factor used when resize strategy is CUSTOM_ZOOM. */
  public double getZoomFactor() {
    return zoomFactor;
  }

  protected void resizePanel() {
    if (zoomFactor == 1.0) {
      this.viewer.getComponent().setPreferredSize(getPreferredSize());
      this.viewer.getScrollPane().setPreferredSize(getPreferredSize());
    }
    else {
      // back to image original size.
      Dimension original = new Dimension(image.getWidth(), image.getHeight());
      this.viewer.getComponent().setPreferredSize(original);
      this.viewer.getScrollPane().setPreferredSize(original);
    }

    this.viewer.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    this.viewer.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
  }

  /**
   * Sets the zoom factor to use when the resize strategy is CUSTOM_ZOOM.
   * <p>
   * Note that calling this function does not change the current resize strategy.
   * 
   * @throws IllegalArgumentException
   *           if {@code newZoomFactor} is not a positive number
   */
  public void setZoomFactor(double newZoomFactor) {
    if (zoomFactor == newZoomFactor) {
      return;
    }

    if (newZoomFactor <= 0 || Double.isInfinite(newZoomFactor) || Double.isNaN(newZoomFactor)) {
      throw new IllegalArgumentException("Invalid zoom factor: " + newZoomFactor);
    }

    if (getResizeStrategy() == ResizeStrategy.CUSTOM_ZOOM) {
      scroller.prepare();
    }

    double oldZoomFactor = zoomFactor;
    zoomFactor = newZoomFactor;
    if (getResizeStrategy() == ResizeStrategy.CUSTOM_ZOOM) {

      // resize the panel shrink if needed
      resizePanel();

      resizeNow();
      scroller.rescroll();
    }

    propertyChangeSupport.firePropertyChange("zoomFactor", oldZoomFactor, newZoomFactor);
  }

  @Override
  public Dimension getPreferredSize() {
    if (image == null) {
      return new Dimension();
    }
    else if (resizeStrategy == ResizeStrategy.CUSTOM_ZOOM) {
      return new Dimension((int) Math.ceil(image.getWidth() * zoomFactor), (int) Math.ceil(image.getHeight() * zoomFactor));
    }
    else
      return new Dimension(image.getWidth(), image.getHeight());
  }

  /**
   * Returns the image pixel that is under the given point.
   * 
   * @param p
   *          a point in component coordinate system
   * @return the corresponding image pixel, or <code>null</code> if the point is outside the image
   */
  public Point pointToPixel(Point p) {
    return pointToPixel(p, true);
  }

  /**
   * Returns the image pixel corresponding to the given point. If the <code>clipToImage</code> parameter is <code>false</code>, then the function will
   * return an appropriately positioned pixel on an infinite plane, even if the point is outside the image bounds. If <code>clipToImage</code> is
   * <code>true</code> then the function will return <code>null</code> for such positions, and any non-null return value will be a valid image pixel.
   * 
   * @param p
   *          a point in component coordinate system
   * @param clipToImage
   *          whether the function should return <code>null</code> for positions outside the image bounds
   * @return the corresponding image pixel
   * @throws IllegalStateException
   *           if there is no image set or if the size of the viewer is 0 (for example because it is not in a visible component)
   */
  public Point pointToPixel(Point p, boolean clipToImage) {
    Point2D.Double fp = new Point2D.Double(p.x + .5, p.y + .5);
    try {
      getImageTransform().inverseTransform(fp, fp);
    }
    catch (NoninvertibleTransformException ex) {
      throw new IllegalStateException("Image transformation not invertible");
    }

    p.x = (int) Math.floor(fp.x);
    p.y = (int) Math.floor(fp.y);
    if (clipToImage && (p.x < 0 || p.y < 0 || p.x >= image.getWidth() || p.y >= image.getHeight())) {
      return null;
    }
    return p;
  }

  @Override
  protected void paintComponent(Graphics g) {
    paintManager.paintComponent(g);
  }

  /**
   * Returns the transformation that is applied to the image. Most commonly the transformation is the concatenation of a uniform scale and a
   * translation.
   * <p>
   * The <code>AffineTransform</code> instance returned by this method should not be modified.
   * 
   * @return the transformation applied to the image before painting
   * @throws IllegalStateException
   *           if there is no image set or if the size of the viewer is 0 (for example because it is not in a visible component)
   */
  public AffineTransform getImageTransform() {
    if (getImage() == null) {
      throw new IllegalStateException("No image");
    }
    if (!hasSize()) {
      throw new IllegalStateException("Viewer size is zero");
    }

    double currentZoom;
    switch (resizeStrategy) {
      case NO_RESIZE:
        currentZoom = 1;
        break;

      case SHRINK_TO_FIT:
        currentZoom = Math.min(getSizeRatio(), 1);
        break;

      case RESIZE_TO_FIT:
        currentZoom = getSizeRatio();
        break;

      case CUSTOM_ZOOM:
        currentZoom = zoomFactor;
        break;

      default:
        throw new IllegalStateException("Unhandled resize strategy");
    }

    AffineTransform tr = new AffineTransform();
    tr.setToTranslation((getWidth() - image.getWidth() * currentZoom) / 2.0, (getHeight() - image.getHeight() * currentZoom) / 2.0);
    tr.scale(currentZoom, currentZoom);
    return tr;
  }

  private double getSizeRatio() {
    return Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
  }

  /**
   * Helper class that manages the actual painting.
   */
  private class PaintManager {
    BufferedImage   cachedImage        = null;
    boolean         cachedImageChanged = false;
    AffineTransform cachedTransform;

    private void doPaint(Graphics2D gg, AffineTransform imageTransform) {
      gg.setColor(getBackground());
      gg.fillRect(0, 0, getWidth(), getHeight());

      gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      if (pixelatedZoom && imageTransform.getScaleX() >= 1) {
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      }
      else {
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationType);
      }

      gg.drawImage(image, imageTransform, ImageComponent.this);
    }

    private void ensureCachedValid(AffineTransform imageTransform) {
      boolean cacheValid;

      // create the image if necessary; if the existing one is sufficiently large, use it
      if (cachedImage == null || cachedImage.getWidth() < getWidth() || cachedImage.getHeight() < getHeight()) {
        cachedImage = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());
        cacheValid = false;
      }
      else {
        cacheValid = cachedTransform.equals(imageTransform) && !cachedImageChanged;
      }

      if (!cacheValid) {
        Graphics2D gg = cachedImage.createGraphics();
        doPaint(gg, imageTransform);
        gg.dispose();
        cachedImageChanged = false;
        cachedTransform = new AffineTransform(imageTransform);
      }
    }

    /**
     * Called when a property which affects how the component is painted changes. This invalidates the cache and causes it to be redrawn upon the next
     * paint request.
     */
    public void notifyChanged() {
      cachedImageChanged = true;
    }

    public void paintComponent(Graphics g) {
      if (image == null) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(getBackground());
        gg.fillRect(0, 0, getWidth(), getHeight());
        gg.dispose();
        return;
      }

      AffineTransform imageTransform = getImageTransform();

      if (imageTransform.getScaleX() < 1 && interpolationType != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
        /*
         * We're shrinking the image; instead of letting the Graphics object do it every time, we do it and cache the result.
         */
        ensureCachedValid(imageTransform);
        g.drawImage(cachedImage, 0, 0, ImageComponent.this);
      }
      else {
        // draw the image directly
        Graphics2D gg = (Graphics2D) g.create();
        doPaint(gg, imageTransform);
        gg.dispose();
      }

    }
  }

  /* Handles repositioning the scroll pane when the image is resized so that the same area remains visible. */
  class Scroller {
    private Point2D preparedCenter = null;

    void prepare() {
      if (image != null && hasSize()) {
        Rectangle viewRect = viewer.getScrollPane().getViewport().getViewRect();
        preparedCenter = new Point2D.Double(viewRect.getCenterX(), viewRect.getCenterY());
        try {
          getImageTransform().inverseTransform(preparedCenter, preparedCenter);
        }
        catch (NoninvertibleTransformException e) {
          throw new Error(e);
        }
      }
    }

    void rescroll() {
      if (preparedCenter != null) {
        Dimension viewSize = viewer.getScrollPane().getViewport().getExtentSize();
        getImageTransform().transform(preparedCenter, preparedCenter);
        Rectangle view = new Rectangle((int) Math.round(preparedCenter.getX() - viewSize.width / 2.0),
            (int) Math.round(preparedCenter.getY() - viewSize.height / 2.0), viewSize.width, viewSize.height);
        preparedCenter = null;
        scrollRectToVisible(view);
      }
    }
  }
}
