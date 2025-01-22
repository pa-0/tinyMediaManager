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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaEntity;

/**
 * the class {@link PostProcessExecutor} executes post process steps for movies
 *
 * @author Manuel Laggner, Wolfgang Janess, Myron Boyle
 */
public abstract class PostProcessExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(PostProcessExecutor.class);

  protected final PostProcess postProcess;

  protected PostProcessExecutor(PostProcess postProcess) {
    this.postProcess = postProcess;
  }

  /**
   * prepare the command end execute it with {@link PostProcessExecutor#executeCommand} in the end
   */
  public abstract void execute();

  protected void executeCommand(String[] cmdline, MediaEntity mediaEntity) throws IOException, InterruptedException {
    List<String> commandList = new ArrayList<>();
    ProcessBuilder pb;
    String p = "";
    if (postProcess.getPath() != null) {
      p = postProcess.getPath().toLowerCase(Locale.ROOT);
    }
    if (SystemUtils.IS_OS_WINDOWS) {
      commandList.add("powershell");

      // path filled with some program
      if (p.endsWith("exe") || p.endsWith("com")) {
        commandList.add("&");
        commandList.add("'" + postProcess.getPath() + "'"); // needs to be quoted
        for (String cmd : cmdline) {
          commandList.add("\\\"" + cmd + "\\\""); // add all params with double quote around
        }
      }
      // powershell scripting file
      else if (p.endsWith("ps1")) {
        commandList.add("-ExecutionPolicy"); // default security restriction bypass
        commandList.add("ByPass");
        commandList.add("-File");
        commandList.add(postProcess.getPath()); // needs to be UNquoted
        commandList.addAll(Arrays.asList(cmdline));
      }
      // standard cmd, ONLY if we not operating on a network share // TODO: find better way
      else if ((p.endsWith("bat") || p.endsWith("cmd")) && !mediaEntity.getDataSource().startsWith("\\\\")) {
        commandList.add("&");
        commandList.add("'" + postProcess.getPath() + "'");
        for (String cmd : cmdline) {
          commandList.add("\\\"" + cmd + "\\\""); // add all params with double quote around
        }
      }
      else {
        // just commands - CONVERT TO SINGLE STRING - be sure to use delimiter ";" if multiple commands!!!
        commandList.add("-Command");
        commandList.add(String.join(" ", cmdline));
      }
      pb = new ProcessBuilder(commandList);
    }
    else {
      // Linux & Mac
      pb = new ProcessBuilder("/bin/sh", "-c", postProcess.getPath() + " " + String.join(" ", cmdline));
    }

    pb.redirectErrorStream(true);
    pb.directory(mediaEntity.getPathNIO().toFile());

    LOGGER.debug("Running command: {}", pb.command());
    final Process process = pb.start();

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      new Thread(() -> {
        try {
          IOUtils.copy(process.getInputStream(), outputStream);
        }
        catch (IOException e) {
          LOGGER.debug("could not get output from the process", e);
        }
      }).start();

      int processValue = process.waitFor();
      String response = outputStream.toString(StandardCharsets.UTF_8);
      if (processValue != 0) {
        LOGGER.warn("error at Script: '{}'", response);
        throw new IOException("error running Script - code '" + processValue + "'");
      }
      if (StringUtils.isNotBlank(response)) {
        LOGGER.info(response);
      }
      LOGGER.info("PostProcessing: END");
    }
    finally {
      process.destroy();
      // Process must be destroyed before closing streams, can't use try-with-resources,
      // as resources are closing when leaving try block, before finally
      IOUtils.close(process.getErrorStream());
    }
  }
}
