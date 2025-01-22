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

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskListener;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.TaskListComponent;

import net.miginfocom.swing.MigLayout;

public class TaskListDialog extends TmmDialog implements TmmTaskListener {
  private static TaskListDialog                       instance;

  // a map of all active tasks
  private final Map<TmmTaskHandle, TaskListComponent> taskMap = new ConcurrentHashMap<>();
  private final TaskListComponent                     noActiveTask;

  private final JPanel                                panelContent;

  private TaskListDialog() {
    super(TmmResourceBundle.getString("tasklist.title"), "taskList");
    setModalityType(ModalityType.MODELESS);

    {
      panelContent = new JPanel();
      panelContent.setOpaque(false);

      noActiveTask = new TaskListComponent(TmmResourceBundle.getString("task.nonerunning"));
      noActiveTask.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
      panelContent.add(noActiveTask);

      panelContent.setLayout(new MigLayout("", "[200lp:300lp,grow]", "[]"));
      panelContent.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

      JScrollPane scrollPane = new NoBorderScrollPane();
      scrollPane.setOpaque(false);
      scrollPane.getViewport().setOpaque(false);
      scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
      scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setViewportView(panelContent);

      JPanel rootPanel = new JPanel();
      rootPanel.setBackground(UIManager.getColor("Menu.background"));
      rootPanel.setLayout(new MigLayout("insets 0", "[200lp:300lp,grow]", "[100lp:300lp,grow]"));
      rootPanel.add(scrollPane, "cell 0 0, top, grow");

      getContentPane().add(rootPanel, BorderLayout.CENTER);

      JButton btnAbortAll = new JButton(TmmResourceBundle.getString("Button.abortqueue"));
      btnAbortAll.addActionListener(e -> {
        TmmTaskChain.abortAllOpenTasks();
        taskMap.forEach((task, component) -> {
          task.cancel();
          // removeListItem(task);// hmm... nah, not yet.
        });
      });
      addButton(btnAbortAll);
    }
    TmmTaskManager.getInstance().addTaskListener(this);
    TmmUILayoutStore.getInstance().install(panelContent);
  }

  public static TaskListDialog getInstance() {
    if (instance == null) {
      instance = new TaskListDialog();
    }
    return instance;
  }

  @Override
  public void processTaskEvent(final TmmTaskHandle task) {
    SwingUtilities.invokeLater(() -> {
      TmmTaskHandle.TaskState state = task.getState();
      if (state == null) {
        return;
      }

      switch (state) {
        case CREATED:
        case QUEUED:
          addListItem(task);
          break;

        case STARTED:
        case CANCELLED:
          TaskListComponent comp = taskMap.get(task);
          if (comp == null) {
            addListItem(task);
            comp = taskMap.get(task);
          }
          comp.updateTaskInformation();
          break;

        case FINISHED:
        case FAILED:
        default:
          removeListItem(task);
          break;
      }
    });
  }

  /**
   * add a new task to the task list
   *
   * @param task
   *          the task to be added
   */
  private void addListItem(TmmTaskHandle task) {
    TaskListComponent comp;
    if (taskMap.containsKey(task)) {
      // happens when we click to display on popup and there is a
      // new handle waiting in the queue.
      comp = taskMap.get(task);
    }
    else {
      comp = new TaskListComponent(task);
      taskMap.put(task, comp);
    }

    // remove the no active task component (if available)
    panelContent.remove(noActiveTask);

    comp.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    panelContent.add(comp, "wrap, growx");
    bottomPanel.setVisible(true);

    if (isShowing()) {
      revalidate();
      repaint();
    }
  }

  /**
   * remove the given task from the task list
   *
   * @param task
   *          the task to be removed
   */
  private void removeListItem(TmmTaskHandle task) {
    TaskListComponent comp = taskMap.remove(task);
    if (comp != null) {
      panelContent.remove(comp);
    }

    if (taskMap.isEmpty()) {
      panelContent.add(noActiveTask, "wrap, growx");
      bottomPanel.setVisible(false);
    }

    if (isShowing()) {
      revalidate();
      repaint();
    }
  }
}
