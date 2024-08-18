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
package org.tinymediamanager.core;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.jmte.HtmlEncoder;
import org.tinymediamanager.core.jmte.JSONEncoder;
import org.tinymediamanager.core.jmte.NamedArrayRenderer;
import org.tinymediamanager.core.jmte.NamedArrayUniqueRenderer;
import org.tinymediamanager.core.jmte.NamedBitrateRenderer;
import org.tinymediamanager.core.jmte.NamedDateRenderer;
import org.tinymediamanager.core.jmte.NamedFilesizeRenderer;
import org.tinymediamanager.core.jmte.NamedFirstCharacterRenderer;
import org.tinymediamanager.core.jmte.NamedFramerateRenderer;
import org.tinymediamanager.core.jmte.NamedLowerCaseRenderer;
import org.tinymediamanager.core.jmte.NamedNumberRenderer;
import org.tinymediamanager.core.jmte.NamedReplacementRenderer;
import org.tinymediamanager.core.jmte.NamedSplitRenderer;
import org.tinymediamanager.core.jmte.NamedTitleCaseRenderer;
import org.tinymediamanager.core.jmte.NamedUpperCaseRenderer;
import org.tinymediamanager.core.jmte.RegexpProcessor;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;
import com.floreysoft.jmte.encoder.XMLEncoder;
import com.floreysoft.jmte.extended.ChainedNamedRenderer;

public abstract class MediaEntityExporter {
  private static final Logger   LOGGER             = LoggerFactory.getLogger(MediaEntityExporter.class);
  protected static final String TEMPLATE_DIRECTORY = "templates";

  protected Engine              engine;
  protected Properties          properties;
  protected String              fileName;
  protected String              fileExtension;
  protected String              listTemplate       = "";
  protected String              detailTemplate     = "";
  protected Path                templateDir;
  protected boolean             cancel             = false;

  public enum TemplateType {
    MOVIE,
    MOVIE_SET,
    TV_SHOW
  }

  protected MediaEntityExporter(Path templatePath, TemplateType type) throws Exception {
    templateDir = templatePath;

    // check if template exists and is valid
    if (!Files.isDirectory(templateDir)) {
      throw new FileNotFoundException("illegal template path");
    }

    Path configFile = templateDir.resolve("template.conf");
    if (!Files.exists(configFile)) {
      throw new FileNotFoundException("no template config found");
    }

    // load settings from template
    properties = new Properties();
    try (InputStream is = Files.newInputStream(configFile); BufferedInputStream bis = new BufferedInputStream(is)) {
      properties.load(bis);
    }

    // check needed settings
    String typeInConfig = properties.getProperty("type");
    if (!typeInConfig.equalsIgnoreCase(type.name())) {
      throw new Exception("illegal template");
    }

    String listTemplateFile = properties.getProperty("list");
    if (StringUtils.isBlank(listTemplateFile)) {
      throw new Exception("illegal template");
    }

    // get other settings
    String detailTemplateFile = properties.getProperty("detail");
    fileName = StringUtils.isBlank(properties.getProperty("filename")) ? "" : properties.getProperty("filename");
    fileExtension = StringUtils.isBlank(properties.getProperty("extension")) ? "html" : properties.getProperty("extension").toLowerCase();

    // set up engine
    engine = Engine.createEngine();
    engine.registerAnnotationProcessor(new RegexpProcessor());

    if ("html".equals(fileExtension)) {
      engine.setEncoder(new HtmlEncoder()); // special char replacement
    }
    else if ("xml".equals(fileExtension)) {
      engine.setEncoder(new XMLEncoder()); // special char replacement
    }
    else if ("json".equals(fileExtension)) {
      engine.setEncoder(new JSONEncoder());
    }

    // load list template from File
    listTemplate = Utils.readFileToString(templateDir.resolve(listTemplateFile));
    if (StringUtils.isNotBlank(detailTemplateFile)) {
      detailTemplate = Utils.readFileToString(templateDir.resolve(detailTemplateFile));
    }
  }

  protected void registerDefaultRenderers() {
    engine.registerNamedRenderer(new NamedArrayRenderer());
    engine.registerNamedRenderer(new NamedArrayUniqueRenderer());
    engine.registerNamedRenderer(new NamedBitrateRenderer());
    engine.registerNamedRenderer(new NamedDateRenderer());
    engine.registerNamedRenderer(new NamedFilesizeRenderer());
    engine.registerNamedRenderer(new NamedFirstCharacterRenderer());
    engine.registerNamedRenderer(new NamedFramerateRenderer());
    engine.registerNamedRenderer(new NamedLowerCaseRenderer());
    engine.registerNamedRenderer(new NamedNumberRenderer());
    engine.registerNamedRenderer(new NamedReplacementRenderer());
    engine.registerNamedRenderer(new NamedSplitRenderer());
    engine.registerNamedRenderer(new NamedTitleCaseRenderer());
    engine.registerNamedRenderer(new NamedUpperCaseRenderer());

    // NEEDS TO BE THE LAST !
    engine.registerNamedRenderer(new ChainedNamedRenderer(engine.getAllNamedRenderers()));
  }

  public abstract <T extends MediaEntity> void export(List<T> entitiesToExport, Path pathToExport) throws Exception;

  /**
   * cancel the export
   */
  public void cancel() {
    this.cancel = true;
  }

  /**
   * Find templates for the given type.
   *
   * @param type
   *          the template type
   * @return the list of all found template types
   */
  public static List<ExportTemplate> findTemplates(TemplateType type) {
    List<ExportTemplate> templatesFound = new ArrayList<>();

    // search in template folder for templates
    // first in tmm dir
    Path templateInTmmDir = Paths.get(TEMPLATE_DIRECTORY).toAbsolutePath();
    templatesFound.addAll(findTemplatesInFolder(templateInTmmDir, type));

    // and on content dir
    Path templateInContentDir = Paths.get(Globals.CONTENT_FOLDER, TEMPLATE_DIRECTORY).toAbsolutePath();
    if (!templateInContentDir.equals(templateInTmmDir)) {
      templatesFound.addAll(findTemplatesInFolder(templateInContentDir, type));
    }

    // sort by title
    templatesFound.sort((o1, o2) -> StringUtils.compare(o1.getName(), o2.getName()));

    return templatesFound;
  }

  /**
   * search for {@link ExportTemplate}s in the given {@link Path}
   *
   * @param folder
   *          the {@link Path} to search templates for
   * @param type
   *          the {@link TemplateType}
   * @return a {@link List} of found {@link ExportTemplate}s
   */
  private static List<ExportTemplate> findTemplatesInFolder(Path folder, TemplateType type) {
    if (!Files.isDirectory(folder)) {
      return Collections.emptyList();
    }

    List<ExportTemplate> templatesFound = new ArrayList<>();

    // search ever subdir
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
      for (Path path : directoryStream) {
        if (Files.isDirectory(path)) {

          // get type of template
          Path config = path.resolve("template.conf");
          if (!Files.exists(config)) {
            continue;
          }

          // load settings from template
          Properties properties = new Properties();
          try (InputStream is = Files.newInputStream(config); BufferedInputStream bis = new BufferedInputStream(is)) {
            properties.load(bis);
          }
          catch (Exception e) {
            LOGGER.warn("error in config \"{}\" - {} ", path, e.getMessage());
            continue;
          }

          // get template type
          String typeInConfig = properties.getProperty("type");
          if (StringUtils.isBlank(typeInConfig)) {
            continue;
          }

          if (typeInConfig.equalsIgnoreCase(type.name())) {
            ExportTemplate template = new ExportTemplate();
            template.setName(properties.getProperty("name"));
            template.setType(type);
            template.setPath(path.toAbsolutePath().toString());
            template.setUrl(properties.getProperty("url"));
            template.setDescription(properties.getProperty("description"));
            if (StringUtils.isNotBlank(properties.getProperty("detail"))) {
              template.setDetail(true);
            }
            else {
              template.setDetail(false);
            }

            templatesFound.add(template);
          }
        }
      }
    }
    catch (IOException ignored) {
      // nothing to do
    }

    return templatesFound;
  }

  /**
   * this renderer is used to copy artwork into the exported template
   *
   * @author Manuel Laggner
   */
  protected abstract static class ArtworkCopyRenderer implements NamedRenderer {
    protected Path pathToExport;

    public ArtworkCopyRenderer(Path pathToExport) {
      this.pathToExport = pathToExport;
    }

    @Override
    public RenderFormatInfo getFormatInfo() {
      return null;
    }

    @Override
    public String getName() {
      return "copyArtwork";
    }

    /**
     * parse the parameters out of the parameters string
     *
     * @param parameters
     *          the parameters as string
     * @return a map containing all parameters
     */
    protected Map<String, Object> parseParameters(String parameters) {
      Map<String, Object> parameterMap = new HashMap<>();

      // defaults
      parameterMap.put("thumb", Boolean.FALSE);
      parameterMap.put("destination", "images");

      String[] details = parameters.split(",");
      for (String detail : details) {
        String key = "";
        String value = "";
        try {
          String[] d = detail.split("=");
          key = d[0].strip();
          value = d[1].strip();
        }
        catch (Exception ignored) {
          // ignored
        }

        if (StringUtils.isAnyBlank(key, value)) {
          continue;
        }

        switch (key.toLowerCase(Locale.ROOT)) {
          case "type" -> {
            try {
              MediaFileType type = MediaFileType.valueOf(value.toUpperCase(Locale.ROOT));
              parameterMap.put(key, type);
            }
            catch (Exception e) {
              // do not let the exporter crash
            }
          }
          case "destination", "default" -> parameterMap.put(key, value);
          case "thumb", "escape" -> parameterMap.put(key, Boolean.parseBoolean(value));
          case "width" -> {
            try {
              parameterMap.put(key, Integer.parseInt(value));
            }
            catch (Exception ignored) {
              // do not let the exporter crash
            }
          }
          default -> {
          }
        }
      }

      return parameterMap;
    }
  }
}
