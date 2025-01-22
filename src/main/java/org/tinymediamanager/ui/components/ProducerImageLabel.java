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
package org.tinymediamanager.ui.components;

import java.awt.Graphics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.Person;

/**
 * The Class ActorImageLabel.
 * 
 * @author Manuel Laggner
 */
public class ProducerImageLabel extends ImageLabel {
  private SwingWorker<Void, Void> producerWorker = null;
  private Person                  producer       = null;

  public void setProducer(MediaEntity mediaEntity, Person producer) {
    clearImage();

    if (mediaEntity != null && producer != null && producer != this.producer) {
      if (producerWorker != null && !producerWorker.isDone()) {
        producerWorker.cancel(true);
      }
      this.producer = producer;
      producerWorker = new ProducerImageLoader(producer, mediaEntity);
      producerWorker.execute();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    // refetch the image if its visible now
    if (isShowing() && !isLoading() && scaledImage == null) {
      if (StringUtils.isNotBlank(imagePath)) {
        if (worker != null && !worker.isDone()) {
          worker.cancel(true);
        }
        worker = new ImageLoader(this.imagePath, this.getSize());
        worker.execute();
        return;
      }
      else if (StringUtils.isNotBlank(imageUrl)) {
        worker = new ImageFetcher(imageUrl, this.getSize());
        worker.execute();
        return;
      }
    }

    super.paintComponent(g);
  }

  @Override
  protected boolean isLoading() {
    return (worker != null && !worker.isDone()) || (producerWorker != null && !producerWorker.isDone());
  }

  /*
   * inner class for loading the producer images
   */
  protected class ProducerImageLoader extends SwingWorker<Void, Void> {
    private final Person      producer;
    private final MediaEntity mediaEntity;
    private Path              imagePath = null;

    private ProducerImageLoader(Person producer, MediaEntity mediaEntity) {
      this.producer = producer;
      this.mediaEntity = mediaEntity;
    }

    @Override
    protected Void doInBackground() {
      // set file (or cached one) if existent
      String producerImageFilename = producer.getNameForStorage();
      if (StringUtils.isNotBlank(producerImageFilename)) {
        Path file = null;

        // we prefer reading it from the cache
        if (preferCache) {
          file = ImageCache.getCachedFile(Paths.get(mediaEntity.getPath(), Person.PRODUCER_DIR, producerImageFilename));
        }

        // not in the cache - read it from the path
        if (file == null) {
          file = Paths.get(mediaEntity.getPath(), Person.PRODUCER_DIR, producerImageFilename);
        }

        // not available in the path and not preferred from the cache..
        // well just try to read it from the cache
        if ((file == null || !Files.exists(file)) && !preferCache) {
          file = ImageCache.getCachedFile(Paths.get(mediaEntity.getPath(), Person.PRODUCER_DIR, producerImageFilename));
        }

        if (file != null && Files.exists(file)) {
          imagePath = file;
          return null;
        }
      }

      // no file found, try to cache url (if visible, otherwise load on demand in paintComponent)
      if (isShowing()) {
        Path p = ImageCache.getCachedFile(producer.getThumbUrl());
        if (p != null) {
          imagePath = p;
        }
      }

      return null;
    }

    @Override
    protected void done() {
      if (isCancelled()) {
        return;
      }

      if (imagePath != null) {
        setImagePath(imagePath.toString());
      }
      else {
        clearImage();
      }
    }
  }
}
