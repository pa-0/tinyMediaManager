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
package org.tinymediamanager.scraper.spi;

import java.util.List;

import org.tinymediamanager.scraper.interfaces.IMediaProvider;

/**
 * the interface {@link IAddonProvider} is used to provide loading of external scrapers
 * 
 * @author Manuel Laggner
 */
public interface IAddonProvider {
  List<Class<? extends IMediaProvider>> getAddonClasses();
  // /**
  // * get an instance of the {@link IMovieMetadataProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link IMovieMetadataProvider} or null
  // */
  // IMovieMetadataProvider getMovieMetadataProvider();
  //
  // /**
  // * get an instance of the {@link IMovieArtworkProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link IMovieArtworkProvider} or null
  // */
  // IMovieArtworkProvider getMovieArtworkProvider();
  //
  // /**
  // * get an instance of the {@link IMovieTrailerProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link IMovieTrailerProvider} or null
  // */
  // IMovieTrailerProvider getMovieTrailerProvider();
  //
  // /**
  // * get an instance of the {@link IMovieSubtitleProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link IMovieSubtitleProvider} or null
  // */
  // IMovieSubtitleProvider getMovieSubtitleProvider();
  //
  // /**
  // * get an instance of the {@link ITvShowMetadataProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link ITvShowMetadataProvider} or null
  // */
  // ITvShowMetadataProvider getTvShowMetadataProvider();
  //
  // /**
  // * get an instance of the {@link ITvShowArtworkProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link ITvShowArtworkProvider} or null
  // */
  // ITvShowArtworkProvider getTvShowArtworkProvider();
  //
  // /**
  // * get an instance of the {@link ITvShowTrailerProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link ITvShowTrailerProvider} or null
  // */
  // ITvShowTrailerProvider getTvShowTrailerProvider();
  //
  // /**
  // * get an instance of the {@link ITvShowSubtitleProvider} or null if the plugin does not provide an implementation of that interface
  // *
  // * @return an instance of {@link ITvShowSubtitleProvider} or null
  // */
  // ITvShowSubtitleProvider getTvShowSubtitleProvider();
}
