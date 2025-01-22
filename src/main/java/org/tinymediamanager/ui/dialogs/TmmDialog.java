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
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.BindingGroup;
import org.tinymediamanager.ui.EqualsLayout;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class TmmDialog. The abstract super class to handle all dialogs in tMM
 * 
 * @author Manuel Laggner
 */
public abstract class TmmDialog extends JDialog implements IModalPopupPanelProvider {

  protected BindingGroup bindingGroup = null;

  protected JPanel       topPanel     = null;
  protected JPanel       bottomPanel  = null;
  protected JPanel       buttonPanel  = null;

  private int            popupIndex   = JLayeredPane.MODAL_LAYER;

  /**
   * @wbp.parser.constructor
   */
  public TmmDialog(String title, String id) {
    this(MainWindow.getInstance(), title, id);
  }

  public TmmDialog(JFrame owner, String title, String id) {
    super(owner);
    init(title, id);
  }

  public TmmDialog(JDialog owner, String title, String id) {
    super(owner);
    init(title, id);
  }

  public TmmDialog(Window owner, String title, String id) {
    super(owner);
    init(title, id);
  }

  /**
   * set all desired parameters for that dialog
   *
   * @param title
   *          the dialog title
   * @param id
   *          the dialog id
   */
  protected void init(String title, String id) {
    setTitle(title);
    if (StringUtils.isNotBlank(id)) {
      setName(id);
    }
    setModal(true);
    setModalityType(ModalityType.APPLICATION_MODAL);

    if (getOwner() != null) {
      try {
        setIconImage(getOwner().getIconImages().get(0));
      }
      catch (Exception ignored) {
        setIconImage(MainWindow.LOGOS);
      }
    }
    else {
      setIconImage(MainWindow.LOGOS);
    }

    initBottomPanel();

    // always resize all popups too
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        for (Component component : getLayeredPane().getComponents()) {
          int layer = getLayeredPane().getLayer(component);
          if (layer >= JLayeredPane.MODAL_LAYER) {
            component.setBounds(getContentPane().getBounds());
          }
        }
      }
    });
  }

  /**
   * init the bottomPanel (south) for button and information usage
   */
  protected void initBottomPanel() {
    {
      bottomPanel = new JPanel();
      getContentPane().add(bottomPanel, BorderLayout.SOUTH);
      bottomPanel.setLayout(new MigLayout("insets n 0 0 0, gap rel 0", "[grow][]", "[shrink 0][]"));

      JSeparator separator = new JSeparator();
      bottomPanel.add(separator, "cell 0 0 2 1,growx");

      buttonPanel = new JPanel();
      EqualsLayout layout = new EqualsLayout(5);
      layout.setMinWidth(100);
      buttonPanel.setLayout(layout);
      buttonPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
      bottomPanel.add(buttonPanel, "cell 1 1");
    }
  }

  /**
   * set the given panel on the bottom (as a replacement to the default bottom pane)
   *
   * @param panel
   *          the panel to be set
   */
  protected void setBottomPanel(JPanel panel) {
    getContentPane().add(panel, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(null);
  }

  /**
   * set the given panel on the bottom left (left of the buttons)
   *
   * @param panel
   *          the panel to be set
   */
  protected void setBottomInformationPanel(JPanel panel) {
    bottomPanel.add(panel, "cell 0 1,growx, wmin 0");
  }

  /**
   * set the given panel on the top
   *
   * @param panel
   *          the panel to be set
   */
  protected void setTopPanel(JPanel panel) {
    getContentPane().add(panel, BorderLayout.NORTH);
  }

  /**
   * set the given panel on the top including the JSeparator to create a visual border to the content
   *
   * @param panel
   *          the panel to be set
   */
  protected void setTopInformationPanel(JPanel panel) {
    if (topPanel == null) {
      topPanel = new JPanel();
      getContentPane().add(topPanel, BorderLayout.NORTH);
      topPanel.setLayout(new MigLayout("insets 0 0 n 0, gap rel 0", "[grow]", "[][shrink 0]"));

      JSeparator separator = new JSeparator();
      topPanel.add(separator, "cell 0 1,growx, wmin 0");
    }

    topPanel.add(panel, "cell 0 0,growx");
  }

  /**
   * add a button to the buttonPanel
   *
   * @param button
   *          the button to be added
   * @param defaultButton
   *          should that button be the default button for that dialog?
   */
  private void addButton(JButton button, boolean defaultButton) {
    if (button != null) {
      buttonPanel.add(button);
      if (defaultButton) {
        getRootPane().setDefaultButton(button);
      }
    }
  }

  /**
   * add a button to the buttonPanel
   *
   * @param button
   *          the button to be added
   */
  protected void addButton(JButton button) {
    addButton(button, false);
  }

  /**
   * add a default button to the buttonPanel
   *
   * @param button
   *          the button to be added
   */
  protected void addDefaultButton(JButton button) {
    addButton(button, true);
  }

  @Override
  protected JRootPane createRootPane() {
    JRootPane rootPane = super.createRootPane();
    KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
    Action actionListener = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        // hide topmost modal panel or hide the dialog at all
        if (popupIndex > JLayeredPane.MODAL_LAYER) {
          Component[] components = getLayeredPane().getComponentsInLayer(popupIndex - 1);
          for (Component component : components) {
            if (component instanceof ModalPopupPanel popupPanel) {
              hideModalPopupPanel(popupPanel);
            }
          }
        }
        else {
          // no modal panel - hide the dialog
          setVisible(false);
        }
      }
    };

    InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(stroke, "ESCAPE");
    rootPane.getActionMap().put("ESCAPE", actionListener);

    return rootPane;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      pack();
      TmmUILayoutStore.getInstance().loadSettings(this);
      super.setVisible(true);
      toFront();
    }
    else {
      TmmUILayoutStore.getInstance().saveSettings(this);
      super.setVisible(false);
      dispose();
    }
  }

  @Override
  public void dispose() {
    unbind();
    super.dispose();
  }

  /**
   * unbind bound bindings (reduce memory consumption)
   */
  protected void unbind() {
    if (bindingGroup != null) {
      try {
        bindingGroup.unbind();
      }
      catch (Exception ignored) {
        // just not crash
      }
    }
  }

  @Override
  public void showModalPopupPanel(ModalPopupPanel popupPanel) {
    popupPanel.setBounds(getContentPane().getBounds());
    getLayeredPane().add(popupPanel, popupIndex++, 0);
  }

  @Override
  public void hideModalPopupPanel(ModalPopupPanel popupPanel) {
    getLayeredPane().remove(popupPanel);
    popupIndex--;
    validate();
    repaint();
  }
}
