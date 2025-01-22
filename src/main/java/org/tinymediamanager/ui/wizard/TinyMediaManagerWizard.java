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
package org.tinymediamanager.ui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.tinymediamanager.Globals;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The class TinyMediaManagerWizard provides a wizard for easy first time setup of tinyMediaManager
 * 
 * @author Manuel Laggner
 */
public class TinyMediaManagerWizard extends TmmDialog {
  private final List<JPanel> panels;
  private int                activePanelIndex = 0;

  private BackAction         actBack;
  private NextAction         actNext;
  private JButton            btnBack;
  private JButton            btnNext;
  private JPanel             panelContent;

  public TinyMediaManagerWizard() {
    super((JFrame) null, "tinyMediaManager Setup Wizard", "");
    setMinimumSize(new Dimension(800, 600));

    initComponents();

    // data init
    panels = new ArrayList<>();
    panels.add(new EntrancePanel());
    panels.add(new DisclaimerPanel(this));
    panels.add(new UiSettingsPanelLite());
    panels.add(new MovieSourcePanel());
    panels.add(new MovieScraperPanel());
    panels.add(new TvShowSourcePanel());
    panels.add(new TvShowScraperPanel());

    for (int i = 0; i < panels.size(); i++) {
      JPanel panel = panels.get(i);
      panelContent.add(panel, "" + i);
    }

    btnBack.setEnabled(false);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        Utils.deleteFileSafely(Paths.get(Globals.DATA_FOLDER, "tmm.json"));
        System.exit(0);
      }
    });
  }

  private void initComponents() {
    {
      JPanel panelSizing = new JPanel();
      getContentPane().add(panelSizing, BorderLayout.CENTER);
      panelSizing.setLayout(new MigLayout("", "[650lp:850lp,grow]", "[450lp:550lp,grow]"));

      panelContent = new JPanel();
      panelContent.setLayout(new CardLayout());
      panelSizing.add(panelContent, "cell 0 0,grow");
    }
    {
      actBack = new BackAction();
      btnBack = new JButton(actBack);
      addButton(btnBack);

      actNext = new NextAction();
      btnNext = new JButton(actNext);
      addDefaultButton(btnNext);
    }
  }

  JButton getBtnBack() {
    return btnBack;
  }

  JButton getBtnNext() {
    return btnNext;
  }

  @Override
  public void pack() {
    // do not pack - it would look weird
  }

  private void setNextButtonLabel() {
    actNext.setButtonLabel();
  }

  private boolean isLastPanel() {
    return panels != null && activePanelIndex >= panels.size() - 1;
  }

  private class BackAction extends AbstractAction {
    public BackAction() {
      putValue(NAME, TmmResourceBundle.getString("wizard.back"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      activePanelIndex--;
      if (activePanelIndex > 0) {
        btnBack.setEnabled(true);
      }
      else {
        btnBack.setEnabled(false);
      }
      setNextButtonLabel();

      CardLayout cl = (CardLayout) (panelContent.getLayout());
      cl.show(panelContent, "" + activePanelIndex);
      panelContent.revalidate();
    }
  }

  private class NextAction extends AbstractAction {
    public NextAction() {
      setButtonLabel();
    }

    void setButtonLabel() {
      if (isLastPanel()) {
        putValue(NAME, TmmResourceBundle.getString("wizard.finish"));
      }
      else {
        putValue(NAME, TmmResourceBundle.getString("wizard.next"));
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      activePanelIndex++;

      if (activePanelIndex == panels.size()) {
        TmmModuleManager.getInstance().saveSettings();

        // fire events, that the wizard finished
        MovieModuleManager.getInstance().getSettings().firePropertyChange("wizard", false, true);
        TvShowModuleManager.getInstance().getSettings().firePropertyChange("wizard", false, true);

        // close the wizard
        TinyMediaManagerWizard.this.setVisible(false);
      }
      else {
        btnBack.setEnabled(true);
        setNextButtonLabel();

        CardLayout cl = (CardLayout) (panelContent.getLayout());
        cl.show(panelContent, "" + activePanelIndex);
        panelContent.revalidate();
      }
    }
  }

}
