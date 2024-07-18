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
package org.tinymediamanager.ui;

import static org.tinymediamanager.TinyMediaManager.shutdownLogger;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.TinyMediaManager;
import org.tinymediamanager.core.ITmmModule;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.MediaInfo;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.TextFieldPopupMenu;
import org.tinymediamanager.ui.components.toolbar.ToolbarPanel;
import org.tinymediamanager.ui.dialogs.SettingsDialog;
import org.tinymediamanager.ui.images.LogoCircle;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.panels.IModalPopupPanelProvider;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.panels.StatusBarPanel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import com.sun.jna.Platform;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MainWindow.
 * 
 * @author Manuel Laggner
 */
public class MainWindow extends JFrame implements IModalPopupPanelProvider {
  private static final Logger LOGGER     = LoggerFactory.getLogger(MainWindow.class);
  public static final Image   LOGOS      = createLogos();

  private static MainWindow   instance;

  private JMenuBar            topMenuBar;
  private ToolbarPanel        toolbarPanel;

  private JPanel              masterPanel;
  private MainMenuPanel       menuPanel;
  private JPanel              detailPanel;
  private int                 popupIndex = JLayeredPane.MODAL_LAYER;

  /**
   * Gets the active instance.
   *
   * @return the active instance
   */
  public static synchronized MainWindow getInstance() {
    if (instance == null) {
      new MainWindow("tinyMediaManager");
    }
    return instance;
  }

  /**
   * Create the application.
   * 
   * @param name
   *          the name
   */
  private MainWindow(String name) {
    super(name);
    setName("mainWindow");
    setMinimumSize(new Dimension(1050, 700));

    // needed here, otherwise we get a stack overflow
    instance = this; // NOSONAR

    initialize();
  }

  /**
   * load all predefined logo sizes
   * 
   * @return a list of all predefined logos
   */
  private static Image createLogos() {
    List<Image> images = new ArrayList<>();
    images.add(new LogoCircle(48).getImage());
    images.add(new LogoCircle(64).getImage());
    images.add(new LogoCircle(96).getImage());
    images.add(new LogoCircle(128).getImage());
    images.add(new LogoCircle(256).getImage());
    images.add(new LogoCircle(512).getImage());
    images.add(new LogoCircle(1024).getImage());

    return new BaseMultiResolutionImage(5, images.toArray(new Image[0]));
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    // set the logo
    setIconImage(LOGOS);
    setBounds(5, 5, 1100, 720);
    // do nothing, we have our own windowClosing() listener
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    // on macOS use the system menu bar
    if (SystemUtils.IS_OS_MAC) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");

      topMenuBar = new JMenuBar();
      setJMenuBar(topMenuBar);
    }

    toolbarPanel = new ToolbarPanel();
    getContentPane().add(toolbarPanel, BorderLayout.NORTH);

    JPanel rootPanel = new JPanel();
    Color color = UIManager.getColor("Panel.tmmAlternateBackground");
    if (color != null) {
      rootPanel.setBackground(color);
    }
    rootPanel.setLayout(new MigLayout("insets 0", "[]0lp[900lp:n,grow]", "[300lp:400lp,grow,shrink 0]"));

    // to draw the shadow beneath the toolbar, encapsulate the panel
    JLayer<JComponent> rootLayer = new JLayer<>(rootPanel, new ShadowLayerUI()); // $hide$ - do not parse this in wbpro
    getContentPane().add(rootLayer, BorderLayout.CENTER);

    menuPanel = new MainMenuPanel();
    rootPanel.add(menuPanel, "cell 0 0, growy");

    JSplitPane splitPane = new JSplitPane();
    splitPane.setContinuousLayout(true);
    splitPane.setResizeWeight(0.5);
    splitPane.setOneTouchExpandable(true);
    splitPane.setName("mainWindow.splitPane");
    rootPanel.add(splitPane, "cell 1 0, grow");

    masterPanel = new JPanel();
    masterPanel.setLayout(new CardLayout(0, 0));
    splitPane.setLeftComponent(masterPanel);

    detailPanel = new JPanel();
    detailPanel.setLayout(new CardLayout(0, 0));
    splitPane.setRightComponent(detailPanel);

    // add the hint for this part
    HintManager.getInstance().addHint(TmmResourceBundle.getString("hintmanager.tabs"), detailPanel, SwingConstants.LEFT);

    JPanel panelStatusBar = new StatusBarPanel();
    getContentPane().add(panelStatusBar, BorderLayout.SOUTH);

    LOGGER.debug("Initializing MovieUIModule...");
    addModule(MovieUIModule.getInstance());
    LOGGER.debug("Initializing MovieSetUIModule...");
    addModule(MovieSetUIModule.getInstance());
    LOGGER.debug("Initializing TvShowUIModule...");
    addModule(TvShowUIModule.getInstance());
    LOGGER.debug("Done initializing UI modules :)");

    // shutdown listener - to clean database connections safely
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        closeTmm();
      }
    });

    MessageManager.instance.addListener(TmmUIMessageCollector.instance);

    // mouse event listener for context menu
    Toolkit.getDefaultToolkit().addAWTEventListener(arg0 -> {
      if (arg0 instanceof MouseEvent mouseEvent && ((MouseEvent) arg0).isPopupTrigger() && arg0.getSource() instanceof JTextComponent textComponent) {
        if (mouseEvent.isPopupTrigger() && textComponent.getComponentPopupMenu() == null) {
          TextFieldPopupMenu.buildCutCopyPaste().show(textComponent, mouseEvent.getX(), mouseEvent.getY());
        }
      }
    }, AWTEvent.MOUSE_EVENT_MASK);

    // inform user that MI could not be loaded
    if (Platform.isLinux() && StringUtils.isBlank(MediaInfo.version())) {
      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this, TmmResourceBundle.getString("mediainfo.failed.linux")));
    }

    // inform user that something happened while loading the modules
    for (ITmmModule module : TmmModuleManager.getInstance().getModules()) {
      if (!module.getStartupMessages().isEmpty()) {
        for (String message : module.getStartupMessages()) {
          SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this, message));
        }
      }
    }

    // init the settings window
    SwingUtilities.invokeLater(SettingsDialog::getInstance);

    if (Settings.getInstance().isNewConfig()) {
      SwingUtilities.invokeLater(() -> HintManager.getInstance().showHints());
    }

    // set the splitter position after everything has been initialized
    TmmUILayoutStore.getInstance().install(splitPane);
  }

  private void addModule(ITmmUIModule module) {
    JTabbedPane tabbedPane = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("rightBorder", "half");
        putClientProperty("roundEdge", Boolean.FALSE);
        super.updateUI();
      }
    };
    tabbedPane.addTab(module.getTabTitle(), module.getTabPanel());
    masterPanel.add(tabbedPane, module.getModuleId());
    detailPanel.add(module.getDetailPanel(), module.getModuleId());

    menuPanel.addModule(module);
  }

  public void closeTmm() {
    closeTmmAndStart(null);
  }

  public void closeTmmAndStart(ProcessBuilder pb) {
    int confirm = JOptionPane.YES_OPTION;
    // if there are some threads running, display exit confirmation
    if (TmmTaskManager.getInstance().isPoolRunning()) {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      confirm = JOptionPane.showOptionDialog(MainWindow.this, TmmResourceBundle.getString("tmm.exit.runningtasks"),
          TmmResourceBundle.getString("tmm.exit.confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null); // $NON-NLS-1$
    }
    if (confirm == JOptionPane.YES_OPTION) {
      LOGGER.info("bye bye");

      saveWindowLayout();

      TinyMediaManager.shutdown();
      dispose();

      // spawn our process
      if (pb != null) {
        try {
          LOGGER.info("Going to execute: {}", pb.command());
          pb.start();
        }
        catch (Exception e) {
          LOGGER.error("Cannot spawn process:", e);
        }
      }
      shutdownLogger();
      System.exit(0); // calling the method is a must
    }
  }

  /**
   * Gets the frame.
   * 
   * @return the frame
   */
  public static JFrame getFrame() {
    return instance;
  }

  public void createLightbox(String pathToFile, String urlToFile) {
    LightBox.showLightBox(instance, pathToFile, urlToFile);
  }

  public void saveWindowLayout() {
    for (Window window : Window.getWindows()) {
      if (window instanceof JDialog dialog) {
        if (dialog.isVisible()) {
          TmmUILayoutStore.getInstance().saveSettings(dialog);
        }
      }
      else if (window instanceof JFrame frame) {
        if (frame.isVisible()) {
          TmmUILayoutStore.getInstance().saveSettings(frame);
        }
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

  void setActiveModule(ITmmUIModule module) {
    toolbarPanel.setUIModule(module);

    CardLayout cl = (CardLayout) masterPanel.getLayout();
    cl.show(masterPanel, module.getModuleId());

    cl = (CardLayout) detailPanel.getLayout();
    cl.show(detailPanel, module.getModuleId());

    updateMenuBar(module);
  }

  private void updateMenuBar(ITmmUIModule uiModule) {
    if (topMenuBar != null) {
      topMenuBar.removeAll();

      // update data sources
      if (uiModule.getUpdateMenu() != null) {
        topMenuBar.add(TmmUIMenuHelper.morphJPopupMenuToJMenu(uiModule.getUpdateMenu(), TmmResourceBundle.getString("Toolbar.update")));
      }

      // search & scrape
      if (uiModule.getSearchMenu() != null) {
        topMenuBar.add(TmmUIMenuHelper.morphJPopupMenuToJMenu(uiModule.getSearchMenu(), TmmResourceBundle.getString("Toolbar.search")));
      }

      // edit
      if (uiModule.getEditMenu() != null) {
        topMenuBar.add(TmmUIMenuHelper.morphJPopupMenuToJMenu(uiModule.getEditMenu(), TmmResourceBundle.getString("Toolbar.edit")));
      }

      // rename
      if (uiModule.getRenameMenu() != null) {
        topMenuBar.add(TmmUIMenuHelper.morphJPopupMenuToJMenu(uiModule.getRenameMenu(), TmmResourceBundle.getString("Toolbar.rename")));
      }

      // tools
      topMenuBar.add(TmmUIMenuHelper.morphJPopupMenuToJMenu(menuPanel.getToolsMenu(), TmmResourceBundle.getString("Toolbar.tools")));
      topMenuBar.add(TmmUIMenuHelper.morphJPopupMenuToJMenu(menuPanel.getInfoMenu(), TmmResourceBundle.getString("Toolbar.help")));

      topMenuBar.revalidate();
    }
  }
}
