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
package org.tinymediamanager.core.threading;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.license.TmmFeature;

/**
 * The class TmmTask. The main class representing tasks in tmm
 * 
 * @author Manuel Laggner
 */
public abstract class TmmTask implements Runnable, TmmTaskHandle, TmmFeature {
  private final Set<TmmTaskListener> listeners = new CopyOnWriteArraySet<>();
  private final TaskType             type;
  private final long                 uniqueId;

  protected TaskState                state     = TaskState.CREATED;
  protected String                   taskName;
  protected String                   taskDescription;
  protected int                      workUnits;
  protected int                      progressDone;
  protected boolean                  cancel;
  protected Thread                   thread;

  protected TmmTask(String taskName, int workUnits, TaskType type) {
    this.taskName = taskName;
    this.workUnits = workUnits;
    this.taskDescription = "";
    this.progressDone = 0;
    this.type = type;
    uniqueId = TmmTaskManager.getInstance().GLOB_THRD_CNT.incrementAndGet();
    this.thread = null;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public final String getTaskName() {
    return taskName;
  }

  @Override
  public final int getWorkUnits() {
    return workUnits;
  }

  @Override
  public final int getProgressDone() {
    return progressDone;
  }

  @Override
  public final String getTaskDescription() {
    return taskDescription;
  }

  @Override
  public final TaskState getState() {
    return state;
  }

  protected final void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  protected final void setWorkUnits(int workUnits) {
    this.workUnits = workUnits;
  }

  protected final void setProgressDone(int progressDone) {
    this.progressDone = progressDone;
  }

  protected final void setTaskDescription(String taskDescription) {
    this.taskDescription = taskDescription;
  }

  public final void addListener(final TmmTaskListener listener) {
    listeners.add(listener);
  }

  protected final void setState(TaskState newState) {
    this.state = newState;
    // System.out.println("Task '" + getTaskName() + "' set to " + newState);
    informListeners();
  }

  public final void removeListener(final TmmTaskListener listener) {
    listeners.remove(listener);
  }

  protected final void informListeners() {
    // inform the statistics timer that tmm is active
    TmmModuleManager.getInstance().setActive();

    for (TmmTaskListener listener : listeners) {
      listener.processTaskEvent(this);
    }
  }

  @Override
  public final void run() {
    // is this task active at all?
    if (!isFeatureEnabled()) {
      return;
    }

    // the task has been cancelled before it is being executed
    if (cancel) {
      finish();
      return;
    }

    String name = Thread.currentThread().getName();
    if (!name.contains("-G")) {
      name = name + "-G0";
    }
    name = name.replaceAll("\\-G\\d+", "-G" + uniqueId);
    Thread.currentThread().setName(name);

    start();
    try {
      doInBackground();
    }
    finally {
      finish();
    }
  }

  @Override
  public void cancel() {
    this.cancel = true;
    setState(TaskState.CANCELLED);
    // thread = null; // nah, cancelled tasks can run till finished, and must not emty-ed immediately
  }

  protected void start() {
    thread = Thread.currentThread();
    setState(TaskState.STARTED);
  }

  protected void publishState(String taskDescription, int progress) {
    this.taskDescription = taskDescription;
    this.progressDone = progress;
    informListeners();
  }

  protected void publishState(int progress) {
    this.progressDone = progress;
    informListeners();
  }

  protected void publishState(String taskDescription) {
    this.taskDescription = taskDescription;
    publishState();
  }

  protected void publishState() {
    informListeners();
  }

  protected void finish() {
    if (state != TaskState.FAILED) {
      setState(TaskState.FINISHED);
    }
    thread = null;
    listeners.clear(); // avoid leak
  }

  @Override
  public final TaskType getType() {
    return type;
  }

  protected abstract void doInBackground();
}
