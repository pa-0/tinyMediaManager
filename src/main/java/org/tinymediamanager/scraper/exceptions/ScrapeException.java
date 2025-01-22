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

package org.tinymediamanager.scraper.exceptions;

/**
 * the class {@link ScrapeException} wraps the source {@link Exception} which occurred while scraping or searching
 *
 * @author Manuel Laggner
 * @since 3.0
 */
public class ScrapeException extends Exception {
  /**
   * the package private constructor -> to be used in child classes
   */
  ScrapeException() {
  }

  /**
   * the main constructor - just to wrap the source {@link Exception}
   *
   * @param cause
   *          the source {@link Exception}
   */
  public ScrapeException(Throwable cause) {
    super(cause);
  }

  /**
   * a handy constructor to pass just a message to the exception
   *
   * @param message
   *          the message for the exception
   */
  public ScrapeException(String message) {
    super(message);
  }
}
