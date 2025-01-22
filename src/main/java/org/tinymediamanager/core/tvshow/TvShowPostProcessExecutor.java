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
package org.tinymediamanager.core.tvshow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.PostProcessExecutor;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import com.floreysoft.jmte.Engine;

/**
 * the class {@link TvShowPostProcessExecutor} executes post process steps for movies
 *
 * @author Wolfgang Janes
 */
public class TvShowPostProcessExecutor extends PostProcessExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowPostProcessExecutor.class);

  public TvShowPostProcessExecutor(PostProcess postProcess) {
    super(postProcess);
  }

  public void execute() {

    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShows();

    for (TvShow tvs : selectedTvShows) {
      LOGGER.info("PostProcessing: START {}", postProcess);
      Map<String, Object> mappings = new HashMap<>();
      mappings.put("tvShow", tvs);

      String[] command = substituteTokens(mappings);

      try {
        executeCommand(command, tvs);
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
    Engine engine = TvShowRenamer.createEngine();

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
