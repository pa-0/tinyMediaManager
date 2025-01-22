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

package org.tinymediamanager.ui.panels;

import static org.tinymediamanager.ui.plaf.UIUtils.fitToScreen;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskListener;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmTaskbar;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.dialogs.TaskListDialog;

import net.miginfocom.swing.MigLayout;

/**
 * a status taskProgressBar indicating the memory amount, some information and the messages
 *
 * @author Manuel Laggner
 */
public class StatusBarPanel extends JPanel implements TmmTaskListener {
  private final Set<TmmTaskHandle> taskSet;

  private TmmTaskHandle            activeTask;

  private JPanel                   memoryUsagePanel;

  private JLabel                   taskLabel;
  private JProgressBar             taskProgressBar;
  private JButton                  taskStopButton;

  public StatusBarPanel() {
    initComponents();

    // further initializations
    taskLabel.setVisible(false);
    taskStopButton.setVisible(false);
    taskProgressBar.setVisible(false);

    // task management
    taskSet = new HashSet<>();
    taskLabel.setText("");
    TmmTaskManager.getInstance().addTaskListener(this);

    // memory indication
    final Settings settings = Settings.getInstance();

    memoryUsagePanel.setVisible(settings.isShowMemory());
    memoryUsagePanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        System.gc();
      }
    });
    // listener for settings change

    settings.addPropertyChangeListener(evt -> {
      memoryUsagePanel.setVisible(settings.isShowMemory());
    });

    // pre-load the dialog (to fetch all events)
    TaskListDialog.getInstance();
  }

  private void initComponents() {
    setLayout(new MigLayout("insets 0 n 0 0, hidemode 3", "[][50lp:n][grow][100lp][15lp:n][][]", "[22lp:n]"));

    {
      taskLabel = new JLabel("XYZ");
      add(taskLabel, "cell 2 0,alignx right, wmin 0");
    }
    {
      taskProgressBar = new JProgressBar();
      taskProgressBar.addMouseListener(new MListener());
      add(taskProgressBar, "cell 3 0");
    }
    {
      taskStopButton = new FlatButton(IconManager.CANCEL);
      taskStopButton.addActionListener(e -> {
        if (activeTask instanceof TmmTask) {
          activeTask.cancel();
        }
      });
      add(taskStopButton, "cell 4 0");
    }
    {
      memoryUsagePanel = new MemoryUsagePanel();
      add(memoryUsagePanel, "cell 6 0");
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // draw top border
    Graphics2D graphics2D = (Graphics2D) g.create();
    graphics2D.setColor(UIManager.getColor("Panel.tmmAlternateBackground"));
    graphics2D.drawLine(0, 0, getWidth(), 0);
    graphics2D.dispose();
  }

  @Override
  public synchronized void processTaskEvent(final TmmTaskHandle task) {
    SwingUtilities.invokeLater(() -> {

      switch (task.getState()) {
        case CREATED:
        case QUEUED:
        case STARTED:
        case CANCELLED:
          taskSet.add(task);
          break;

        case FINISHED:
        case FAILED:
          taskSet.remove(task);
          break;

      }

      // search for a new activetask to be displayed in the statusbar
      if (activeTask == null || activeTask.getState() == TmmTaskHandle.TaskState.FINISHED
          || activeTask.getState() == TmmTaskHandle.TaskState.FAILED) {
        activeTask = null;
        for (TmmTaskHandle handle : taskSet) {
          if (handle.getType() == TmmTaskHandle.TaskType.MAIN_TASK && handle.getState() == TmmTaskHandle.TaskState.STARTED) {
            activeTask = handle;
            break;
          }
        }

        // no active main task found; if there are any BG tasks, display a dummy char to indicate something is working
        if (activeTask == null) {
          for (TmmTaskHandle handle : taskSet) {
            if (handle.getState() == TmmTaskHandle.TaskState.STARTED) {
              activeTask = handle;
              break;
            }
          }
        }
      }

      // hide components if there is nothing to be displayed
      if (activeTask == null) {
        taskLabel.setVisible(false);
        taskStopButton.setVisible(false);
        taskProgressBar.setVisible(false);

        TmmTaskbar.setProgressState(Taskbar.State.OFF);
        TmmTaskbar.setProgressValue(-1);
      }
      else {
        // ensure everything is visible
        taskLabel.setVisible(true);
        taskProgressBar.setVisible(true);
        if (activeTask.getType() == TmmTaskHandle.TaskType.MAIN_TASK) {
          taskStopButton.setVisible(true);
        }
        else {
          taskStopButton.setVisible(false);
        }

        // and update content
        taskLabel.setText(activeTask.getTaskName());
        if (activeTask.getWorkUnits() > 0) {
          try {
            // try/catch here; in a very occasional situation the last task might finish while we are inside the IF
            int workUnits = activeTask.getWorkUnits();

            if (workUnits == 1) {
              taskProgressBar.setIndeterminate(true);
            }
            else {
              taskProgressBar.setMaximum(workUnits);
              taskProgressBar.setValue(activeTask.getProgressDone());
              taskProgressBar.setIndeterminate(false);
            }

            TmmTaskbar.setProgressState(Taskbar.State.NORMAL);
            TmmTaskbar.setProgressValue(100 * activeTask.getProgressDone() / activeTask.getWorkUnits());
          }
          catch (Exception e) {
            // just ignore
          }
        }
        else {
          taskProgressBar.setIndeterminate(true);

          TmmTaskbar.setProgressState(Taskbar.State.INDETERMINATE);
        }
      }
    });
  }

  /**
   * calculate the tooltip {@link Point} for the given tooltip {@link Dimension} and the actual mouse location
   *
   * @param owner
   *          the {@link Component} showing the tooltip
   * @param popupSize
   *          the {@link Dimension} of the tooltip
   * @return the origin point where the tooltip should be shown
   */
  private Point getTooltipPointFor(Component owner, Dimension popupSize) {
    Point location = new Point(0, owner.getHeight());
    location.y += 5; // 5 ... tooltip offset
    SwingUtilities.convertPointToScreen(location, owner);

    Rectangle r = new Rectangle(location, popupSize);
    fitToScreen(r);
    location = r.getLocation();
    SwingUtilities.convertPointFromScreen(location, owner);
    r.setLocation(location);

    if (r.intersects(new Rectangle(0, 0, owner.getWidth(), owner.getHeight()))) {
      location.y = -r.height - 5;
    }

    return location;
  }

  /****************************************************************************************
   * helper classes
   ****************************************************************************************/
  private class MListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      TaskListDialog.getInstance().setVisible(true);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
  }
}
