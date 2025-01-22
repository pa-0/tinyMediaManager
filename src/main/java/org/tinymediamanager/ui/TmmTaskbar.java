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
package org.tinymediamanager.ui;

import java.awt.Image;
import java.awt.Taskbar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the class TmmTaskbar manages updates to the taskbar/dock on supported systems
 * 
 * @author Manuel Laggner
 */
public class TmmTaskbar {
  private static final Logger  LOGGER  = LoggerFactory.getLogger(TmmTaskbar.class);
  private static final Taskbar TASKBAR = initTaskbar();

  private TmmTaskbar() {
    // private constructor for utility classes
  }

  /**
   * init the Taskbar
   * 
   * @return an instance of the Taskbar or null if not supported
   */
  private static Taskbar initTaskbar() {
    if (Taskbar.isTaskbarSupported()) {
      return Taskbar.getTaskbar();
    }
    return null;
  }

  /**
   * set the given image as the Taskbar image (if supported)
   * 
   * @param image
   *          the image to set
   */
  public static void setImage(final Image image) {
    try {
      if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        TASKBAR.setIconImage(image);
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not set taskbar image - {}", e.getMessage());
    }
  }

  /**
   * set the given text as the Taskbar badge (if supported)
   * 
   * @param text
   *          the text to set
   */
  public static void setBadge(final String text) {
    try {
      if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)) {
        TASKBAR.setIconBadge(text);
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not set taskbar badge - {}", e.getMessage());
    }
  }

  /**
   * set the given image as the Taskbar badge (if supported)
   *
   * @param image
   *          the image to set
   */
  public static void setBadge(final Image image) {
    try {
      if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.ICON_BADGE_IMAGE_WINDOW)) {
        TASKBAR.setWindowIconBadge(MainWindow.getInstance(), image);
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not set taskbar badge - {}", e.getMessage());
    }
  }

  /**
   * request user attention for in the taskbar
   */
  public static void requestUserAttention() {
    try {
      if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.USER_ATTENTION)) {
        TASKBAR.requestUserAttention(true, true);
      }
      else if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.USER_ATTENTION_WINDOW)) {
        TASKBAR.requestWindowUserAttention(MainWindow.getInstance());
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not request user attention - {}", e.getMessage());
    }
  }

  /**
   * set the progress value
   * 
   * @param progress
   */
  public static void setProgressValue(int progress) {
    try {
      if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
        TASKBAR.setProgressValue(progress);
      }
      else if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW)) {
        TASKBAR.setWindowProgressValue(MainWindow.getInstance(), progress);
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not set progress value - {}", e.getMessage());
    }
  }

  /**
   * set the progress state
   *
   * @param state
   *          the progress state (OFF, NORMAL, INDETERMINATE, ..)
   */
  public static void setProgressState(Taskbar.State state) {
    try {
      if (TASKBAR != null && TASKBAR.isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
        TASKBAR.setWindowProgressState(MainWindow.getInstance(), state);
      }
    }
    catch (Exception e) {
      LOGGER.warn("could not set progress state - {}", e.getMessage());
    }
  }
}
