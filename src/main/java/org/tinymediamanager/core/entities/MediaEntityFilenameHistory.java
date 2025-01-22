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

package org.tinymediamanager.core.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * the class {@link MediaEntityFilenameHistory} is used to track the changed of the last renamer action
 * 
 * @author Manuel Laggner
 */
public class MediaEntityFilenameHistory {
  @JsonProperty
  private String                oldPath;
  @JsonProperty
  private String                newPath;
  @JsonProperty
  private List<FilenameHistory> filenameHistory;

  public void setOldPath(String oldPath) {
    this.oldPath = oldPath;
  }

  /**
   * This gets the old path of the entity. THIS IS ONLY FILLED FOR TOP LEVEL ENTITIES (movies, TV shows, ...)
   * 
   * @return the old path of this entity
   */
  public String getOldPath() {
    return oldPath;
  }

  public void setNewPath(String newPath) {
    this.newPath = newPath;
  }

  /**
   * This gets the new path of the entity. THIS IS ONLY FILLED FOR TOP LEVEL ENTITIES (movies, TV shows, ...)
   * 
   * @return the new path of this entity
   */
  public String getNewPath() {
    return newPath;
  }

  public void addFilenameHistory(FilenameHistory filenameHistory) {
    if (this.filenameHistory == null) {
      this.filenameHistory = new ArrayList<>();
    }
    this.filenameHistory.add(filenameHistory);
  }

  public void setFilenameHistory(List<FilenameHistory> newFilenames) {
    if (filenameHistory == null) {
      filenameHistory = new ArrayList<>();
    }
    else {
      filenameHistory.clear();
    }
    filenameHistory.addAll(newFilenames);
  }

  /**
   * This gets a {@link List} of all file paths with their old and new values. ALL VALUES ARE RELATIVE TO THE PATH OF THE TOP LEVEL ENTITY
   * 
   * @return a {@link List} of all file paths
   */
  public List<FilenameHistory> getFilenameHistory() {
    if (filenameHistory == null) {
      return Collections.emptyList();
    }
    return filenameHistory;
  }

  /**
   * The record FileNameHistory is used to track the filename changes after renaming
   *
   * @param oldFilename
   *          the old file name
   * @param newFilename
   *          the new file name
   */
  public record FilenameHistory(@JsonProperty String oldFilename, @JsonProperty String newFilename) {
  }
}
