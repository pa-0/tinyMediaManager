/*
 * Copyright 2012 - 2024 Manuel Laggner
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
package org.tinymediamanager.core.tvshow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.PostProcessExecutor;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.jmte.NamedArrayRenderer;
import org.tinymediamanager.core.jmte.NamedArrayUniqueRenderer;
import org.tinymediamanager.core.jmte.NamedBitrateRenderer;
import org.tinymediamanager.core.jmte.NamedDateRenderer;
import org.tinymediamanager.core.jmte.NamedFilesizeRenderer;
import org.tinymediamanager.core.jmte.NamedFramerateRenderer;
import org.tinymediamanager.core.jmte.NamedLowerCaseRenderer;
import org.tinymediamanager.core.jmte.NamedNumberRenderer;
import org.tinymediamanager.core.jmte.NamedReplacementRenderer;
import org.tinymediamanager.core.jmte.NamedSplitRenderer;
import org.tinymediamanager.core.jmte.NamedTitleCaseRenderer;
import org.tinymediamanager.core.jmte.NamedUpperCaseRenderer;
import org.tinymediamanager.core.jmte.RegexpProcessor;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.jmte.ZeroNumberRenderer;
import org.tinymediamanager.core.tvshow.TvShowRenamer.TvShowNamedFirstCharacterRenderer;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.extended.ChainedNamedRenderer;

/**
 * the class {@link TvShowEpisodePostProcessExecutor} executes post process steps for movies
 *
 * @author Wolfgang Janes
 */
public class TvShowEpisodePostProcessExecutor extends PostProcessExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowEpisodePostProcessExecutor.class);

  public TvShowEpisodePostProcessExecutor(PostProcess postProcess) {
    super(postProcess);
  }

  public void execute() {
    List<TvShowEpisode> selectedEpisodes = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();

    for (TvShowEpisode episode : selectedEpisodes) {
      LOGGER.info("PostProcessing: START {}", postProcess);
      Map<String, Object> mappings = new HashMap<>();
      mappings.put("tvShow", episode.getTvShow());
      mappings.put("season", episode.getTvShowSeason());
      mappings.put("episode", episode);

      String[] command = substituteTokens(mappings);

      try {
        executeCommand(command, episode);
      }
      catch (InterruptedException e) {
        // ignored
        return;
      }
      catch (Exception e) {
        LOGGER.error("Problem executing post process", e);
      }
    }
  }

  private String[] substituteTokens(Map<String, Object> mappings) {
    Engine engine = Engine.createEngine();
    engine.registerRenderer(Number.class, new ZeroNumberRenderer());
    engine.registerNamedRenderer(new NamedArrayRenderer());
    engine.registerNamedRenderer(new NamedArrayUniqueRenderer());
    engine.registerNamedRenderer(new NamedBitrateRenderer());
    engine.registerNamedRenderer(new NamedDateRenderer());
    engine.registerNamedRenderer(new NamedFilesizeRenderer());
    engine.registerNamedRenderer(new NamedFramerateRenderer());
    engine.registerNamedRenderer(new NamedLowerCaseRenderer());
    engine.registerNamedRenderer(new NamedNumberRenderer());
    engine.registerNamedRenderer(new NamedReplacementRenderer());
    engine.registerNamedRenderer(new NamedSplitRenderer());
    engine.registerNamedRenderer(new NamedTitleCaseRenderer());
    engine.registerNamedRenderer(new NamedUpperCaseRenderer());
    engine.registerNamedRenderer(new TvShowNamedFirstCharacterRenderer());
    engine.registerNamedRenderer(new ChainedNamedRenderer(engine.getAllNamedRenderers()));

    engine.registerAnnotationProcessor(new RegexpProcessor());

    engine.setModelAdaptor(new TmmModelAdaptor());

    if (postProcess.getPath() == null || postProcess.getPath().isEmpty()) {
      // scripting mode - transform as single string
      String transformed = engine.transform(JmteUtils.morphTemplate(postProcess.getCommand(), TvShowRenamer.getTokenMap()), mappings);
      return new String[] { transformed };
    }
    else {
      // parameter mode - transform every line to have separated params
      String[] splitted = postProcess.getCommand().split("\\n");
      for (int i = 0; i < splitted.length; i++) {
        splitted[i] = engine.transform(JmteUtils.morphTemplate(splitted[i], TvShowRenamer.getTokenMap()), mappings);
      }
      return splitted;
    }
  }
}
