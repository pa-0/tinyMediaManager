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
package org.tinymediamanager.ui.components.toolbar;

import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.license.License;
import org.tinymediamanager.ui.HintManager;
import org.tinymediamanager.ui.ITmmUIModule;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUIMessageCollector;
import org.tinymediamanager.ui.actions.CheckForUpdateAction;
import org.tinymediamanager.ui.actions.UnlockAction;
import org.tinymediamanager.ui.dialogs.MessageHistoryDialog;
import org.tinymediamanager.updater.UpdateCheck;

import net.miginfocom.swing.MigLayout;

/**
 * The Class ToolbarPanel.
 *
 * @author Manuel Laggner
 */
public class ToolbarPanel extends JPanel {
  private static final Logger LOGGER = LoggerFactory.getLogger(ToolbarPanel.class); // $NON-NLS-1$

  private final ToolbarButton btnSearch;
  private final ToolbarButton btnEdit;
  private final ToolbarButton btnUpdate;
  private final ToolbarButton btnRename;
  private final ToolbarButton btnUnlock;
  private final ToolbarButton btnRenewLicense;
  private final ToolbarButton btnUpdateFound;

  private final ToolbarMenu   menuUpdate;
  private final ToolbarMenu   menuSearch;
  private final ToolbarMenu   menuEdit;
  private final ToolbarMenu   menuRename;

  public ToolbarPanel() {
    setLayout(new BorderLayout());

    JPanel panelCenter = new JPanel();
    add(panelCenter, BorderLayout.CENTER);
    panelCenter.setOpaque(false);
    panelCenter.setLayout(
        new MigLayout("insets 0, hidemode 3", "[][]20lp[]20lp[]20lp[]20lp[]20lp[][grow]15lp[]15lp[]15lp[][][][][][15lp:n]", "[45lp]1lp[]5lp"));

    panelCenter.add(new JLabel(IconManager.TOOLBAR_LOGO), "cell 1 0, alignx left, aligny bottom");
    JLabel lblVersion = new ToolbarLabel(ReleaseInfo.getRealVersion());
    TmmFontHelper.changeFont(lblVersion, TmmFontHelper.L1, Font.BOLD);
    panelCenter.add(lblVersion, "cell 1 1, alignx center");

    btnUpdate = new ToolbarButton(IconManager.TOOLBAR_REFRESH, IconManager.TOOLBAR_REFRESH_HOVER);
    panelCenter.add(btnUpdate, "cell 2 0,grow, alignx center, aligny bottom");

    btnSearch = new ToolbarButton(IconManager.TOOLBAR_SEARCH, IconManager.TOOLBAR_SEARCH_HOVER);
    panelCenter.add(btnSearch, "cell 3 0,grow, alignx center, aligny bottom");

    btnEdit = new ToolbarButton(IconManager.TOOLBAR_EDIT, IconManager.TOOLBAR_EDIT_HOVER);
    panelCenter.add(btnEdit, "cell 4 0,grow, alignx center, aligny bottom");

    btnRename = new ToolbarButton(IconManager.TOOLBAR_RENAME, IconManager.TOOLBAR_RENAME_HOVER);
    panelCenter.add(btnRename, "cell 5 0,grow, alignx center, aligny bottom");

    btnUnlock = new ToolbarButton(IconManager.TOOLBAR_UPGRADE, IconManager.TOOLBAR_UPGRADE);
    Action unlockAction = new UnlockAction();
    btnUnlock.setAction(unlockAction);
    btnUnlock.setToolTipText(TmmResourceBundle.getString("Toolbar.upgrade.desc"));
    panelCenter.add(btnUnlock, "cell 11 0 1 2, center");

    btnRenewLicense = new ToolbarButton(IconManager.TOOLBAR_RENEW, IconManager.TOOLBAR_RENEW);
    btnRenewLicense.setAction(unlockAction);
    btnRenewLicense.setToolTipText(TmmResourceBundle.getString("Toolbar.renewlicense.desc"));
    panelCenter.add(btnRenewLicense, "cell 12 0 1 2, center, gap 10lp");

    btnUpdateFound = new ToolbarButton(IconManager.TOOLBAR_DOWNLOAD, IconManager.TOOLBAR_DOWNLOAD);
    btnUpdateFound.setAction(new CheckForUpdateAction());
    btnUpdateFound.setToolTipText(TmmResourceBundle.getString("tmm.update.message.toolbar"));
    panelCenter.add(btnUpdateFound, "cell 13 0 1 2, center, gap 10lp");

    JButton btnNotifications = new ToolbarButton(IconManager.TOOLBAR_ALERT);
    panelCenter.add(btnNotifications, "cell 14 0 1 2, center, gap 10lp");

    btnNotifications.addActionListener(e -> {
      MessageHistoryDialog dialog = MessageHistoryDialog.getInstance();
      dialog.setVisible(true);
    });

    menuUpdate = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.update"));
    panelCenter.add(menuUpdate, "cell 2 1,alignx center, wmin 0");

    menuSearch = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.search"));
    panelCenter.add(menuSearch, "cell 3 1,alignx center, wmin 0");

    menuEdit = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.edit"));
    panelCenter.add(menuEdit, "cell 4 1,alignx center, wmin 0");

    menuRename = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.rename"));
    panelCenter.add(menuRename, "cell 5 1,alignx center, wmin 0");

    // listener for messages change
    TmmUIMessageCollector.instance.addPropertyChangeListener(evt -> {
      if (Constants.MESSAGES.equals(evt.getPropertyName())) {
        if (TmmUIMessageCollector.instance.getNewMessagesCount() > 0) {
          btnNotifications.setIcon(IconManager.TOOLBAR_ALERT_RED);
          btnNotifications.setToolTipText(TmmResourceBundle.getString("notifications.new"));
        }
        else {
          btnNotifications.setIcon(IconManager.TOOLBAR_ALERT);
          btnNotifications.setToolTipText(null);
        }
        btnNotifications.repaint();
      }
    });

    License.getInstance().addEventListener(this::showHideUnlock);

    showHideUnlock();

    initUpgradeCheck();

    // add the hint for the buttons
    HintManager.getInstance().addHint(TmmResourceBundle.getString("hintmanager.update"), btnUpdate, SwingConstants.BOTTOM);
    HintManager.getInstance().addHint(TmmResourceBundle.getString("hintmanager.scrape"), btnSearch, SwingConstants.BOTTOM);
    HintManager.getInstance().addHint(TmmResourceBundle.getString("hintmanager.edit"), btnEdit, SwingConstants.BOTTOM);
    HintManager.getInstance().addHint(TmmResourceBundle.getString("hintmanager.rename"), btnRename, SwingConstants.BOTTOM);
  }

  private void showHideUnlock() {
    if (License.getInstance().isValidLicense()) {
      btnUnlock.setVisible(false);

      LocalDate validUntil = License.getInstance().validUntil();
      if (validUntil != null && validUntil.minus(14, ChronoUnit.DAYS).isBefore(LocalDate.now())) {
        btnRenewLicense.setVisible(true);
      }
      else {
        btnRenewLicense.setVisible(false);
      }
    }
    else {
      btnUnlock.setVisible(true);
      btnRenewLicense.setVisible(false);
    }
  }

  private void initUpgradeCheck() {
    btnUpdateFound.setVisible(false);

    Runnable runnable = () -> {
      // only update if the last update check is more than the specified interval ago
      if (TmmUIHelper.shouldCheckForUpdate()) {
        try {
          UpdateCheck updateCheck = new UpdateCheck();
          if (updateCheck.isUpdateAvailable()) {
            btnUpdateFound.setVisible(true);
            LOGGER.info("update available");
          }
        }
        catch (Exception e) {
          LOGGER.warn("Update check failed - {}", e.getMessage());
        }
      }
    };

    if (!Settings.getInstance().isNewConfig()) {
      // if the wizard is not run, check for an update
      // this has a simple reason: the wizard lets you do some settings only once: if you accept the update WHILE the wizard is
      // showing, the wizard will no more appear
      // the same goes for the scraping AFTER the wizard has been started.. in this way the update check is only being done at the
      // next startup

      // update task start a few secs after GUI...
      Timer timer = new Timer(10 * 1000, e -> TmmTaskManager.getInstance().addUnnamedTask(runnable));
      timer.setRepeats(false);
      timer.start();
    }

    // for long-running instances (like docker) trigger an update check every day
    Timer timer = new Timer(24 * 60 * 60 * 1000, e -> TmmTaskManager.getInstance().addUnnamedTask(runnable));
    timer.setRepeats(true);
    timer.start();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    setBackground(UIManager.getColor("Tmm.toolbar.background"));
  }

  public void setUIModule(ITmmUIModule module) {
    btnUpdate.setAction(module.getUpdateAction());
    btnUpdate.setIcons(module.getSearchButtonIcon(), module.getSearchButtonHoverIcon());
    btnSearch.setAction(module.getSearchAction());
    btnEdit.setAction(module.getEditAction());
    btnRename.setAction(module.getRenameAction());

    menuUpdate.setPopupMenu(module.getUpdateMenu());
    menuSearch.setPopupMenu(module.getSearchMenu());
    menuEdit.setPopupMenu(module.getEditMenu());
    menuRename.setPopupMenu(module.getRenameMenu());
  }
}
