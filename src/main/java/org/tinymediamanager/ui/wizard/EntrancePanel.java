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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.jsoup.Jsoup;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.actions.ImportV4DataAction;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.images.Logo;

import net.miginfocom.swing.MigLayout;

/**
 * The class EntrancePanel is the first panel which is displayed in the wizard
 *
 * @author Manuel Laggner
 */
class EntrancePanel extends JPanel {
  public EntrancePanel() {
    initComponents();
  }

  /*
   * init UI components
   */
  private void initComponents() {
    setLayout(new MigLayout("", "[50lp:50lp,grow][][10lp][][50lp:50lp,grow]", "[grow][25lp!][][20lp:20lp][][40lp][50lp:50lp,grow]"));

    JLabel lblLogo = new JLabel("");
    lblLogo.setIcon(new Logo(256));
    add(lblLogo, "cell 3 0,alignx center,aligny bottom");

    String greetingText = Jsoup.parse(TmmResourceBundle.getString("wizard.greeting.header")).text();
    final JTextPane tpGreetingHeader = new ReadOnlyTextPane(greetingText);
    TmmFontHelper.changeFont(tpGreetingHeader, TmmFontHelper.H1);
    add(tpGreetingHeader, "cell 0 2 5 1,alignx center");

    JTextPane tpGreetingText = new ReadOnlyTextPane(TmmResourceBundle.getString("wizard.greeting.text"));
    add(tpGreetingText, "cell 3 4,grow");

    JButton btnImportData = new JButton(TmmResourceBundle.getString("wizard.import"));
    add(btnImportData, "cell 3 6,alignx center,aligny center");

    btnImportData.addActionListener(new ImportV4DataAction());
  }
}
