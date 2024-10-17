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
package org.tinymediamanager.ui.dialogs;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.EqualsLayout;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.tree.TmmTree;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.tree.TmmTreeTextFilter;
import org.tinymediamanager.ui.settings.TmmSettingsDataProvider;
import org.tinymediamanager.ui.settings.TmmSettingsNode;

import net.miginfocom.swing.MigLayout;

/**
 * The class SettingsDialog. For displaying all settings in a dialog
 * 
 * @author Manuel Laggner
 */
public class SettingsDialog extends TmmDialog {
  private static JDialog                instance;

  private final TmmSettingsDataProvider dataProvider;
  private TmmTree<TmmTreeNode>          tree;
  private JPanel                        rightPanel;
  private TmmSettingsTreeFilter         tfFilter;

  /**
   * Get the single instance of the settings dialog
   * 
   * @return the settings dialog
   */
  public static JDialog getInstance() {
    if (instance == null) {
      instance = new SettingsDialog();
    }
    return instance;
  }

  private SettingsDialog() {
    super(TmmResourceBundle.getString("tmm.settings"), "settings");

    dataProvider = new TmmSettingsDataProvider();

    initComponents();
    initPanels();

    tree.addFilter(tfFilter);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);

    tree.addTreeSelectionListener(e -> {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      if (node != null) {
        // click on a settings node
        if (node.getUserObject() instanceof TmmSettingsNode settingsNode) {
          CardLayout cl = (CardLayout) rightPanel.getLayout();
          cl.show(rightPanel, settingsNode.getId());
          if (settingsNode.getComponent() != null) {
            settingsNode.getComponent().invalidate();
          }
        }
      }
    });

    // expand tree nodes
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }

    // select first node on creation
    SwingUtilities.invokeLater(() -> {
      DefaultMutableTreeNode firstLeaf = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) tree.getModel().getRoot()).getFirstChild();
      tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) firstLeaf.getParent()).getPath()));
      tree.setSelectionPath(new TreePath(firstLeaf.getPath()));
      tree.requestFocus();
    });
  }

  private void initPanels() {
    for (TmmTreeNode node : tree.getDataProvider().getChildren(tree.getDataProvider().getRoot()))
      addSettingsPanel(node);
  }

  private void addSettingsPanel(TmmTreeNode node) {
    if (!(node.getUserObject() instanceof TmmSettingsNode)) {
      return;
    }
    JComponent component = ((TmmSettingsNode) node.getUserObject()).getComponent();
    if (component != null) {
      JScrollPane scrollPane = new NoBorderScrollPane(component);
      scrollPane.getVerticalScrollBar().setUnitIncrement(16);
      rightPanel.add(scrollPane, ((TmmSettingsNode) node.getUserObject()).getId());
    }
    for (TmmTreeNode child : tree.getDataProvider().getChildren(node)) {
      addSettingsPanel(child);
    }
  }

  private void initComponents() {
    {
      JPanel contentPanel = new JPanel();
      contentPanel.setLayout(new MigLayout("", "[600lp:1000lp,grow]", "[600lp,grow]"));
      getContentPane().add(contentPanel, BorderLayout.CENTER);

      JSplitPane splitPane = new JSplitPane();
      splitPane.setName(getName() + ".splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      contentPanel.add(splitPane, "cell 0 0, grow");

      rightPanel = new JPanel();
      rightPanel.setLayout(new CardLayout(0, 0));
      splitPane.setRightComponent(rightPanel);

      JPanel panelLeft = new JPanel();
      splitPane.setLeftComponent(panelLeft);
      panelLeft.setLayout(new MigLayout("", "[200lp:200lp,grow]", "[][400lp,grow]"));
      {
        tfFilter = new TmmSettingsTreeFilter();
        panelLeft.add(tfFilter, "cell 0 0,grow");
        tfFilter.setColumns(10);
      }

      JScrollPane scrollPaneLeft = new NoBorderScrollPane();
      panelLeft.add(scrollPaneLeft, "cell 0 1,grow");

      tree = new TmmTree<>(dataProvider);
      scrollPaneLeft.setViewportView(tree);
      scrollPaneLeft.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    }
    {
      JPanel southPanel = new JPanel();
      getContentPane().add(southPanel, BorderLayout.SOUTH);
      southPanel.setLayout(new MigLayout("insets n 0 0 0, gap rel 0", "[grow][]", "[shrink 0][]"));

      JSeparator separator = new JSeparator();
      southPanel.add(separator, "cell 0 0 2 1,growx");

      JPanel panelButtons = new JPanel();
      EqualsLayout layout = new EqualsLayout(5);
      layout.setMinWidth(100);
      panelButtons.setLayout(layout);
      panelButtons.setBorder(new EmptyBorder(4, 4, 4, 4));
      southPanel.add(panelButtons, "cell 1 1,alignx left,aligny top");

      JButton okButton = new JButton(TmmResourceBundle.getString("Button.close"));
      panelButtons.add(okButton);
      okButton.setAction(new CloseAction());
      getRootPane().setDefaultButton(okButton);
    }
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if (!visible) {
      TmmModuleManager.getInstance().saveSettings();
    }
  }

  private class CloseAction extends AbstractAction {
    CloseAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.close"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }

  private static class TmmSettingsTreeFilter extends TmmTreeTextFilter<TmmTreeNode> {
    @Override
    public boolean accept(TmmTreeNode node) {
      if (StringUtils.isBlank(filterText)) {
        return true;
      }

      // first: filter on the node text
      Matcher matcher = filterPattern.matcher(node.toString());
      if (matcher.find()) {
        return true;
      }

      // second: parse all children too
      for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements();) {
        if (accept((TmmTreeNode) e.nextElement())) {
          return true;
        }
      }

      // third: check the parent(s)
      if (checkParent(node.getDataProvider().getParent(node), filterPattern)) {
        return true;
      }

      return false;
    }
  }
}
