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

package org.tinymediamanager.ui.thirdparty;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmLazyMenuAdapter;
import org.tinymediamanager.ui.movies.actions.MovieKodiGetWatchedAction;
import org.tinymediamanager.ui.movies.actions.MovieKodiRefreshNfoAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetKodiGetWatchedMovieAction;
import org.tinymediamanager.ui.moviesets.actions.MovieSetKodiRefreshMovieNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowKodiGetWatchedAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowKodiRefreshNfoAction;

/**
 * the class {@link KodiRPCMenu} is used to build the menu for the Kodi RPC
 * 
 * @author Myron Boyle, Manuel Laggner
 */
public class KodiRPCMenu {
  protected static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

  private KodiRPCMenu() {
    throw new IllegalAccessError();
  }

  /**
   * Adds Kodi RPC menu structure in right-click popup
   * 
   * @return the {@link JMenu} for the movie popup menu
   */
  public static JMenu createMenuKodiMenuRightClickMovies() {
    String version = KodiRPC.getInstance().getVersion();
    JMenu m = new JMenu(version);
    m.setIcon(IconManager.KODI);

    m.add(new MovieKodiRefreshNfoAction());
    m.add(new MovieKodiGetWatchedAction());

    m.addSeparator();

    JMenuItem connectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.connect"), IconManager.CONNECT);
    connectMenuItem.addActionListener(e -> KodiRPC.getInstance().connect());
    m.add(connectMenuItem);

    JMenuItem disconnectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.disconnect"), IconManager.DISCONNECT);
    disconnectMenuItem.addActionListener(e -> KodiRPC.getInstance().disconnect());
    m.add(disconnectMenuItem);

    m.addSeparator();

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.updatemappings"), IconManager.SYNC);
    i.addActionListener(e -> KodiRPC.getInstance().updateMovieMappings());
    m.add(i);

    m.addSeparator();

    m.add(createMenuApplication());
    m.add(createMenuSystem());
    m.add(createMenuVideoDatasources());
    m.add(createMenuAudioDatasources());

    m.addMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {

        boolean connected = KodiRPC.getInstance().isConnected();

        for (Component component : menu.getMenuComponents()) {
          if (component instanceof JSeparator) {
            continue;
          }

          if (component instanceof JMenuItem menuItem) {
            if (connected) {
              // connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(false);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(true);
              }
              else {// all others
                component.setEnabled(true);
              }
            }
            else {
              // not connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(true);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(false);
              }
              else {// all others
                component.setEnabled(false);
              }
            }
          }
        }
      }
    });

    return m;
  }

  /**
   * Adds Kodi RPC menu structure in right-click popup
   *
   * @return the {@link JMenu} for the movie set popup menu
   */
  public static JMenu createMenuKodiMenuRightClickMovieSets() {
    String version = KodiRPC.getInstance().getVersion();
    JMenu m = new JMenu(version);
    m.setIcon(IconManager.KODI);

    m.add(new MovieSetKodiRefreshMovieNfoAction());
    m.add(new MovieSetKodiGetWatchedMovieAction());

    m.addSeparator();

    JMenuItem connectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.connect"), IconManager.CONNECT);
    connectMenuItem.addActionListener(e -> KodiRPC.getInstance().connect());
    m.add(connectMenuItem);

    JMenuItem disconnectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.disconnect"), IconManager.DISCONNECT);
    disconnectMenuItem.addActionListener(e -> KodiRPC.getInstance().disconnect());
    m.add(disconnectMenuItem);

    m.addSeparator();

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.updatemappings"), IconManager.SYNC);
    i.addActionListener(e -> KodiRPC.getInstance().updateMovieMappings());
    m.add(i);

    m.addSeparator();

    m.add(createMenuApplication());
    m.add(createMenuSystem());
    m.add(createMenuVideoDatasources());
    m.add(createMenuAudioDatasources());

    m.addMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        boolean connected = KodiRPC.getInstance().isConnected();

        for (Component component : menu.getMenuComponents()) {
          if (component instanceof JSeparator) {
            continue;
          }

          if (component instanceof JMenuItem menuItem) {
            if (connected) {
              // connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(false);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(true);
              }
              else {// all others
                component.setEnabled(true);
              }
            }
            else {
              // not connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(true);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(false);
              }
              else {// all others
                component.setEnabled(false);
              }
            }
          }
        }
      }
    });

    return m;
  }

  /**
   * Adds Kodi RPC menu structure in right-click popup
   * 
   * @return the {@link JMenu} for the TV show popup menu
   */
  public static JMenu createMenuKodiMenuRightClickTvShows() {
    String version = KodiRPC.getInstance().getVersion();
    JMenu m = new JMenu(version);
    m.setIcon(IconManager.KODI);

    m.add(new TvShowKodiRefreshNfoAction());
    m.add(new TvShowKodiGetWatchedAction());

    m.addSeparator();

    JMenuItem connectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.connect"), IconManager.CONNECT);
    connectMenuItem.addActionListener(e -> KodiRPC.getInstance().connect());
    m.add(connectMenuItem);

    JMenuItem disconnectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.disconnect"), IconManager.DISCONNECT);
    disconnectMenuItem.addActionListener(e -> KodiRPC.getInstance().disconnect());
    m.add(disconnectMenuItem);

    m.addSeparator();

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.updatemappings"), IconManager.SYNC);
    i.addActionListener(e -> KodiRPC.getInstance().updateTvShowMappings());
    m.add(i);

    m.addSeparator();

    m.add(createMenuApplication());
    m.add(createMenuSystem());
    m.add(createMenuVideoDatasources());
    m.add(createMenuAudioDatasources());

    m.addMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        boolean connected = KodiRPC.getInstance().isConnected();

        for (Component component : menu.getMenuComponents()) {
          if (component instanceof JSeparator) {
            continue;
          }

          if (component instanceof JMenuItem menuItem) {
            if (connected) {
              // connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(false);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(true);
              }
              else {// all others
                component.setEnabled(true);
              }
            }
            else {
              // not connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(true);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(false);
              }
              else {// all others
                component.setEnabled(false);
              }
            }
          }
        }
      }
    });

    return m;
  }

  /**
   * Adds Kodi RPC menu structure in top bar
   * 
   * @return the {@link JMenu} for the tools popup menu
   */
  public static JMenu createKodiMenuTop() {
    String version = KodiRPC.getInstance().getVersion();
    JMenu m = new JMenu(version);
    m.setIcon(IconManager.KODI);

    JMenuItem connectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.connect"), IconManager.CONNECT);
    connectMenuItem.addActionListener(e -> KodiRPC.getInstance().connect());
    m.add(connectMenuItem);

    JMenuItem disconnectMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.disconnect"), IconManager.DISCONNECT);
    disconnectMenuItem.addActionListener(e -> KodiRPC.getInstance().disconnect());
    m.add(disconnectMenuItem);

    m.addSeparator();

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.updatemappings"), IconManager.SYNC);
    i.addActionListener(e -> {
      KodiRPC.getInstance().updateMovieMappings();
      KodiRPC.getInstance().updateTvShowMappings();
    });
    m.add(i);

    m.addSeparator();

    m.add(createMenuApplication());
    m.add(createMenuSystem());
    m.add(createMenuVideoDatasources());
    m.add(createMenuAudioDatasources());

    m.addMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        boolean connected = KodiRPC.getInstance().isConnected();

        for (Component component : m.getMenuComponents()) {
          if (component instanceof JSeparator) {
            continue;
          }

          if (component instanceof JMenuItem menuItem) {
            if (connected) {
              // connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(false);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(true);
              }
              else {// all others
                component.setEnabled(true);
              }
            }
            else {
              // not connected
              if (menuItem.getIcon() == IconManager.CONNECT) {
                component.setEnabled(true);
              }
              else if (menuItem.getIcon() == IconManager.DISCONNECT) {
                component.setEnabled(false);
              }
              else {// all others
                component.setEnabled(false);
              }
            }
          }
        }
      }
    });

    return m;
  }

  private static JMenu createMenuApplication() {
    JMenu m = new JMenu(TmmResourceBundle.getString("kodi.rpc.application"));
    m.setIcon(IconManager.MENU);

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.quit"));
    i.addActionListener(e -> KodiRPC.getInstance().quitApplication());
    m.add(i);

    i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.mute"));
    i.addActionListener(e -> KodiRPC.getInstance().muteApplication());
    m.add(i);

    m.add(createMenuVolume());

    return m;
  }

  private static JMenu createMenuVideoDatasources() {
    JMenu m = new JMenu(TmmResourceBundle.getString("kodi.rpc.videolibrary"));
    m.setIcon(IconManager.MOVIE);

    JMenuItem cleanLibraryMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.cleanvideo"), IconManager.CLEAN);
    cleanLibraryMenuItem.setToolTipText(TmmResourceBundle.getString("kodi.rpc.cleanvideo.desc"));
    cleanLibraryMenuItem.addActionListener(event -> KodiRPC.getInstance().cleanVideoLibrary());
    m.add(cleanLibraryMenuItem);

    m.addSeparator();

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.scan.all"), IconManager.REFRESH);
    i.addActionListener(event -> KodiRPC.getInstance().scanVideoLibrary());
    i.setEnabled(true);
    m.add(i);

    if (!KodiRPC.getInstance().getVideoDataSources().isEmpty()) {
      i.setEnabled(true);
      for (Map.Entry<String, String> ds : KodiRPC.getInstance().getVideoDataSources().entrySet()) {
        if (ds.getKey().startsWith("upnp") || ds.getKey().startsWith("addons")) {
          continue;
        }
        i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.scan.item") + " (" + ds.getValue() + ")", IconManager.REFRESH);
        i.addActionListener(event -> KodiRPC.getInstance().scanVideoLibrary(ds.getKey()));
        m.add(i);
      }
    }

    return m;
  }

  private static JMenu createMenuAudioDatasources() {
    JMenu m = new JMenu(TmmResourceBundle.getString("kodi.rpc.audiolibrary"));
    m.setIcon(IconManager.MUSIC);

    JMenuItem cleanLibraryMenuItem = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.cleanaudio"), IconManager.CLEAN);
    cleanLibraryMenuItem.setToolTipText(TmmResourceBundle.getString("kodi.rpc.cleanaudio.desc"));
    cleanLibraryMenuItem.addActionListener(event -> KodiRPC.getInstance().cleanAudioLibrary());
    m.add(cleanLibraryMenuItem);

    m.addSeparator();

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.scan.all"), IconManager.REFRESH);
    i.addActionListener(event -> KodiRPC.getInstance().scanAudioLibrary());
    i.setEnabled(false);
    m.add(i);

    if (!KodiRPC.getInstance().getAudioDataSources().isEmpty()) {
      i.setEnabled(true);
      for (String ds : KodiRPC.getInstance().getAudioDataSources()) {
        if (ds.startsWith("upnp") || ds.startsWith("addons")) {
          continue;
        }
        i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.scan.item") + " " + ds, IconManager.REFRESH);
        i.addActionListener(event -> KodiRPC.getInstance().scanAudioLibrary(ds));
        m.add(i);
      }
    }

    return m;
  }

  private static JMenu createMenuVolume() {
    JMenu m = new JMenu(TmmResourceBundle.getString("kodi.rpc.volume"));
    m.setIcon(IconManager.VOLUME);

    JMenuItem i = new JMenuItem("100%");
    i.addActionListener(new ApplicationVolumeListener(100));
    m.add(i);
    i = new JMenuItem(" 90%");
    i.addActionListener(new ApplicationVolumeListener(90));
    m.add(i);
    i = new JMenuItem(" 80%");
    i.addActionListener(new ApplicationVolumeListener(80));
    m.add(i);
    i = new JMenuItem(" 70%");
    i.addActionListener(new ApplicationVolumeListener(70));
    m.add(i);
    i = new JMenuItem(" 60%");
    i.addActionListener(new ApplicationVolumeListener(60));
    m.add(i);
    i = new JMenuItem(" 50%");
    i.addActionListener(new ApplicationVolumeListener(50));
    m.add(i);
    i = new JMenuItem(" 40%");
    i.addActionListener(new ApplicationVolumeListener(40));
    m.add(i);
    i = new JMenuItem(" 30%");
    i.addActionListener(new ApplicationVolumeListener(30));
    m.add(i);
    i = new JMenuItem(" 20%");
    i.addActionListener(new ApplicationVolumeListener(20));
    m.add(i);
    i = new JMenuItem(" 10%");
    i.addActionListener(new ApplicationVolumeListener(10));
    m.add(i);

    return m;
  }

  private static class ApplicationVolumeListener implements ActionListener {
    private final int vol;

    public ApplicationVolumeListener(int vol) {
      this.vol = vol;
    }

    public void actionPerformed(ActionEvent e) {
      KodiRPC.getInstance().setVolume(vol);
    }
  }

  private static JMenu createMenuSystem() {
    JMenu m = new JMenu(TmmResourceBundle.getString("kodi.rpc.system"));
    m.setIcon(IconManager.MENU);

    JMenuItem i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.hibernate"));
    i.addActionListener(e -> KodiRPC.getInstance().SystemHibernate());
    m.add(i);

    i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.reboot"));
    i.addActionListener(e -> KodiRPC.getInstance().SystemReboot());
    m.add(i);

    i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.shutdown"));
    i.addActionListener(e -> KodiRPC.getInstance().SystemShutdown());
    m.add(i);

    i = new JMenuItem(TmmResourceBundle.getString("kodi.rpc.suspend"));
    i.addActionListener(e -> KodiRPC.getInstance().SystemSuspend());
    m.add(i);

    return m;
  }
}
