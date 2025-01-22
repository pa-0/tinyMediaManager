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

package org.tinymediamanager.ui.thirdparty.imageviewer;

/**
 * Strategy for resizing an image inside a component.
 * 
 * @author Kazó Csaba
 */
public enum ResizeStrategy {
  /** The image is displayed in its original size. */
  NO_RESIZE,
  /** If the image doesn't fit in the component, it is shrunk to the best fit. */
  SHRINK_TO_FIT,
  /** Shrink or enlarge the image to optimally fit the component (keeping aspect ratio). */
  RESIZE_TO_FIT,
  /** Custom fixed zoom */
  CUSTOM_ZOOM
}
