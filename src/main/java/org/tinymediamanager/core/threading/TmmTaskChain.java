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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaEntity;

/**
 * chain several {@link TmmTask}s in a controlled way
 *
 * @author Alex Bruns, Kai Werner, Manuel Laggner
 */
public class TmmTaskChain implements TmmTaskListener {
  private static final Logger                         LOGGER             = LoggerFactory.getLogger(TmmTaskChain.class);

  private static final Map<MediaEntity, TmmTaskChain> ACTIVE_TASK_CHAINS = new HashMap<>();

  private final MediaEntity                           mediaEntity;
  private final Queue<TmmTask>                        tasks              = new LinkedList<>();
  private final ReentrantReadWriteLock                lock               = new ReentrantReadWriteLock();

  private boolean                                     running            = false;

  /**
   * get the {@link TmmTaskChain} for the given {@link MediaEntity}
   *
   * @param mediaEntity
   *          the {@link MediaEntity} to get the {@link TmmTaskChain} for
   * @return the {@link TmmTaskChain}
   */
  public static TmmTaskChain getInstance(MediaEntity mediaEntity) {
    synchronized (ACTIVE_TASK_CHAINS) {
      return ACTIVE_TASK_CHAINS.computeIfAbsent(mediaEntity, TmmTaskChain::new);
    }
  }

  /**
   * abort ALL open Tasks
   */
  public static void abortAllOpenTasks() {
    synchronized (ACTIVE_TASK_CHAINS) {
      ACTIVE_TASK_CHAINS.forEach((mediaEntity1, tmmTaskChain) -> tmmTaskChain.abort());
      ACTIVE_TASK_CHAINS.clear();
    }
  }

  private TmmTaskChain(MediaEntity mediaEntity) {
    this.mediaEntity = mediaEntity;
  }

  /**
   * add a new {@link TmmTask} to the task queue
   *
   * @param task
   *          the {@link TmmTask} to add
   * @return the {@link TmmTaskChain} instance
   */
  public TmmTaskChain add(TmmTask task) {
    lock.writeLock().lock();
    task.addListener(this);
    tasks.add(task);
    lock.writeLock().unlock();

    // start with first task
    if (!running) {
      running = true;
      startNextTask();
    }

    return this;
  }

  /**
   * abort all open tasks
   */
  public void abort() {
    lock.writeLock().lock();
    tasks.clear();
    lock.writeLock().unlock();
  }

  @Override
  public void processTaskEvent(TmmTaskHandle task) {
    switch (task.getState()) {
      case FAILED:
        LOGGER.debug("Task '{}' {} - continue with next", task.getClass().getName(), task.getState());
        startNextTask();
        break;

      case FINISHED:
        startNextTask();
        break;

      default:
        break;
    }
  }

  private void startNextTask() {
    lock.writeLock().lock();
    TmmTask nextTask = tasks.poll();
    lock.writeLock().unlock();

    if (nextTask != null) {
      if (nextTask instanceof TmmThreadPool threadPool) {
        // add the main task to the named queue
        TmmTaskManager.getInstance().addMainTask(threadPool);
      }
      else {
        // add to the unnamed queue
        TmmTaskManager.getInstance().addUnnamedTask(nextTask);
      }
    }
    else {
      // finished
      synchronized (ACTIVE_TASK_CHAINS) {
        ACTIVE_TASK_CHAINS.remove(mediaEntity);
      }
    }
  }
}
