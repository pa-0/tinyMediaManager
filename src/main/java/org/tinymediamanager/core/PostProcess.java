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
package org.tinymediamanager.core;

/**
 * The class {@link PostProcess} - to represent a Postprocess
 *
 * @Author Wolfgang Janes
 */
public class PostProcess extends AbstractModelObject {
  private String name;
  private String path;
  private String command;

  public String getName() {
    return name;
  }

  public void setName(String newValue) {
    String oldValue = this.name;
    this.name = newValue;
    firePropertyChange("name", oldValue, newValue);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String newValue) {
    String oldValue = this.path;
    this.path = newValue;
    firePropertyChange("path", oldValue, newValue);
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String newValue) {
    String oldValue = this.command;
    this.command = newValue;
    firePropertyChange("command", oldValue, newValue);
  }

  @Override
  public String toString() {
    return "PostProcess [path=" + path + ", command=" + command + "]";
  }
}
