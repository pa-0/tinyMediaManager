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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

import org.tinymediamanager.ui.components.NoBorderScrollPane;

import net.miginfocom.swing.MigLayout;

public class ImageViewer {
  private final ImageComponent        theImage;
  private final JScrollPane           scroller;
  private final JPanel                panel;
  private final PropertyChangeSupport propertyChangeSupport;

  private final JPopupMenu            popup;

  /**
   * Creates a new image viewer. Initially it will be empty, and it will have a default popup menu.
   */
  public ImageViewer() {
    this(null);
  }

  /**
   * Creates a new image viewer displaying the specified image. TThe viewer will have a default popup menu.
   * 
   * @param image
   *          the image to display; if {@code null} then no image is displayed
   */
  public ImageViewer(BufferedImage image) {
    propertyChangeSupport = new PropertyChangeSupport(this);
    panel = new JPanel(new MigLayout("", "[300lp,grow]", "[300lp,grow]"));
    scroller = new NoBorderScrollPane() {

      @Override
      protected JViewport createViewport() {
        return new JViewport() {

          @Override
          protected LayoutManager createLayoutManager() {
            return new CustomViewportLayout(ImageViewer.this);
          }

          @Override
          public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
          }

        };
      }

      @Override
      public boolean isValidateRoot() {
        return false;
      }

    };

    theImage = new ImageComponent(this, propertyChangeSupport);
    scroller.setViewportView(new ScrollablePanel(theImage));
    theImage.setImage(image);
    panel.add(scroller, "cell 0 0, center, grow");

    popup = new ImageViewerPopup(ImageViewer.this);
    theImage.addMouseListener(new MyMouseAdapter(popup, panel));
  }

  public JPopupMenu getPopupMenu() {
    return popup;
  }

  /**
   * Returns the image viewer component that can be displayed.
   * 
   * @return the image viewer component
   */
  public JComponent getComponent() {
    return panel;
  }

  /**
   * Sets the image displayed by the viewer. If the argument is the same object as the image currently being displayed, then this method will trigger
   * a refresh. If you modify the image shown by the viewer, use this function to notify the component and cause it to update.
   * 
   * @param image
   *          the new image to display; if <code>null</code> then no image is displayed
   */
  public void setImage(BufferedImage image) {
    theImage.setImage(image);
  }

  /**
   * Returns the currently displayed image.
   * 
   * @return the current image, or <code>null</code> if no image is displayed
   */
  public BufferedImage getImage() {
    return theImage.getImage();
  }

  /**
   * Sets the resize strategy this viewer should use. The default is {@link ResizeStrategy#SHRINK_TO_FIT}.
   * 
   * @param resizeStrategy
   *          the new resize strategy
   */
  void setResizeStrategy(ResizeStrategy resizeStrategy) {
    theImage.setResizeStrategy(resizeStrategy);
  }

  /**
   * Returns the current resize strategy. The default is {@link ResizeStrategy#SHRINK_TO_FIT}.
   * 
   * @return the current resize strategy
   */
  ResizeStrategy getResizeStrategy() {
    return theImage.getResizeStrategy();
  }

  /**
   * Sets whether the image should be resized with nearest neighbor interpolation when it is expanded. The default is {@code false}.
   * 
   * @param pixelatedZoom
   *          the new value of the pixelatedZoom property
   */
  void setPixelatedZoom(boolean pixelatedZoom) {
    theImage.setPixelatedZoom(pixelatedZoom);
  }

  /**
   * Returns the current pixelated zoom setting. The default is {@code false}.
   * 
   * @return the current pixelated zoom setting
   */
  boolean isPixelatedZoom() {
    return theImage.isPixelatedZoom();
  }

  /**
   * Returns the current interpolation type. The default is {@link java.awt.RenderingHints#VALUE_INTERPOLATION_BICUBIC}.
   * 
   * @return the interpolation type
   * @see #setInterpolationType(Object)
   */
  Object getInterpolationType() {
    return theImage.getInterpolationType();
  }

  /**
   * Sets the interpolation type to use when resizing images. See {@link java.awt.RenderingHints#KEY_INTERPOLATION} for details. The default value is
   * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC}.
   * <p>
   * The allowed values are:
   * <ul>
   * <li>{@link java.awt.RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}
   * <li>{@link java.awt.RenderingHints#VALUE_INTERPOLATION_BILINEAR}
   * <li>{@link java.awt.RenderingHints#VALUE_INTERPOLATION_BICUBIC} (default)
   * </ul>
   * Changing the interpolation type to bilinear or nearest neighbor improves painting performance when the image needs to be resized.
   * <p>
   * Note: when the {@code pixelatedZoom} property is set to true and the image is enlarged, then the nearest neighbor method is used regardless of
   * this setting.
   * 
   * @param type
   *          the interpolation type to use when resizing images
   * @throws IllegalArgumentException
   *           if the parameter is not one of the allowed values
   */
  void setInterpolationType(Object type) {
    theImage.setInterpolationType(type);
  }

  /**
   * Returns the zoom factor used when resize strategy is CUSTOM_ZOOM. The default value is 1.
   * 
   * @return the custom zoom factor
   */
  double getZoomFactor() {
    return theImage.getZoomFactor();
  }

  /**
   * Sets the zoom factor to use when the resize strategy is CUSTOM_ZOOM. The default value is 1.
   * <p>
   * Note that calling this function does not change the current resize strategy.
   * 
   * @param newZoomFactor
   *          the new zoom factor for the CUSTOM_ZOOM strategy
   * @throws IllegalArgumentException
   *           if {@code newZoomFactor} is not a positive number
   */
  void setZoomFactor(double newZoomFactor) {
    theImage.setZoomFactor(newZoomFactor);
  }

  /**
   * Adds the specified mouse listener to receive mouse events from the image component of this image viewer. If listener <code>l</code> is
   * <code>null</code>, no exception is thrown and no action is performed.
   * 
   * @param l
   *          the mouse listener
   */
  public void addMouseListener(MouseListener l) {
    theImage.addMouseListener(l);
  }

  /**
   * Removes the specified mouse listener so that it no longer receives mouse motion events from the image component of this image viewer. This method
   * performs no function, nor does it throw an exception, if the listener specified by the argument was not previously added to this component. If
   * listener <code>l</code> is <code>null</code>, no exception is thrown and no action is performed.
   * 
   * @param l
   *          the mouse motion listener
   */
  public void removeMouseListener(MouseListener l) {
    theImage.removeMouseListener(l);
  }

  /**
   * Adds a {@code PropertyChangeListener} to the listener list. The same listener object may be added more than once, and will be called as many
   * times as it is added. If the listener is {@code null}, no exception is thrown and no action is taken.
   * 
   * @param l
   *          the listener to be added
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    propertyChangeSupport.addPropertyChangeListener(l);
  }

  /**
   * Remove a {@code PropertyChangeListener} from the listener list. This removes a listener that was registered for all properties. If the listener
   * was added more than once, it will be notified one less time after being removed. If the listener is {@code null}, or was never added, no
   * exception is thrown and no action is taken.
   * 
   * @param l
   *          the listener to remove
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    propertyChangeSupport.removePropertyChangeListener(l);
  }

  /**
   * Adds a {@code PropertyChangeListener} for a specific property. The listener will be invoked only when a call on firePropertyChange names that
   * specific property. The same listener object may be added more than once. For each property, the listener will be invoked the number of times it
   * was added for that property. If the property name or the listener is null, no exception is thrown and no action is taken.
   * 
   * @param name
   *          the name of the property to listen on
   * @param l
   *          the listener to add
   */
  public void addPropertyChangeListener(String name, PropertyChangeListener l) {
    propertyChangeSupport.addPropertyChangeListener(name, l);
  }

  /**
   * Remove a {@code PropertyChangeListener} from the listener list. This removes a PropertyChangeListener that was registered for all properties. If
   * the listener was added more than once, it will be notified one less time after being removed. If the listener is {@code null}, or was never
   * added, no exception is thrown and no action is taken.
   * 
   * @param name
   *          the name of the property that was listened on
   * @param l
   *          the listener to remove
   */
  public void removePropertyChangeListener(String name, PropertyChangeListener l) {
    propertyChangeSupport.removePropertyChangeListener(name, l);
  }

  /**
   * Returns the scroll pane of the image viewer.
   * 
   * @return the scroll pane
   */
  JScrollPane getScrollPane() {
    return scroller;
  }

  /**
   * helper classes
   */
  private static class MyMouseAdapter extends MouseAdapter {
    private final JPopupMenu popup;
    private final JPanel     panel;

    public MyMouseAdapter(JPopupMenu popup, JPanel panel) {
      this.popup = popup;
      this.panel = panel;
    }

    private void showPopup(MouseEvent e) {
      e.consume();

      Point point = panel.getPopupLocation(e);
      if (point == null) {
        point = e.getPoint();
      }

      popup.show(e.getComponent(), point.x, point.y);
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (e.isPopupTrigger()) {
        showPopup(e);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.isPopupTrigger()) {
        showPopup(e);
      }
    }
  }

  private static class CustomViewportLayout implements LayoutManager {

    private final ImageViewer viewer;

    public CustomViewportLayout(ImageViewer viewer) {
      this.viewer = viewer;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
      // nothing to do
    }

    @Override
    public void removeLayoutComponent(Component comp) {
      // nothing to do
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      BufferedImage image = viewer.getImage();
      if (image == null) {
        return new Dimension();
      }
      else {
        return new Dimension(image.getWidth(), image.getHeight());
      }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      return new Dimension(4, 4);
    }

    @Override
    public void layoutContainer(Container parent) {
      JViewport vp = (JViewport) parent;
      Component view = vp.getView();

      if (view == null) {
        return;
      }

      Dimension vpSize = vp.getSize();
      Dimension viewSize = new Dimension(view.getPreferredSize());

      if (viewer.getResizeStrategy() == ResizeStrategy.SHRINK_TO_FIT || viewer.getResizeStrategy() == ResizeStrategy.RESIZE_TO_FIT) {
        viewSize.width = vpSize.width;
        viewSize.height = vpSize.height;
      }
      else {
        viewSize.width = Math.max(viewSize.width, vpSize.width);
        viewSize.height = Math.max(viewSize.height, vpSize.height);
      }

      vp.setViewSize(viewSize);
    }
  }

  private static class ScrollablePanel extends JPanel implements Scrollable {

    private final ImageComponent theImage;

    public ScrollablePanel(ImageComponent imageComponent) {
      theImage = imageComponent;
      setLayout(new ScrollableLayout(theImage));
      add(theImage);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 50;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return theImage.getResizeStrategy() == ResizeStrategy.SHRINK_TO_FIT || theImage.getResizeStrategy() == ResizeStrategy.RESIZE_TO_FIT;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return theImage.getResizeStrategy() == ResizeStrategy.SHRINK_TO_FIT || theImage.getResizeStrategy() == ResizeStrategy.RESIZE_TO_FIT;
    }

    /*
     * The getPreferredScrollableViewportSize does not seem to be used.
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
      if (theImage.getResizeStrategy() == ResizeStrategy.NO_RESIZE) {
        return getPreferredSize();
      }
      else {
        return javax.swing.SwingUtilities.getAncestorOfClass(JViewport.class, this).getSize();
      }
    }
  }

  private static class ScrollableLayout implements LayoutManager {
    private final ImageComponent theImage;

    public ScrollableLayout(ImageComponent theImage) {
      this.theImage = theImage;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
      // nothing to do
    }

    @Override
    public void removeLayoutComponent(Component comp) {
      // nothing to do
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return theImage.getPreferredSize();
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      return theImage.getMinimumSize();
    }

    @Override
    public void layoutContainer(Container parent) {
      for (int i = 0; i < parent.getComponentCount(); i++) {
        parent.getComponent(i).setBounds(0, 0, parent.getWidth(), parent.getHeight());
      }
    }
  }
}
