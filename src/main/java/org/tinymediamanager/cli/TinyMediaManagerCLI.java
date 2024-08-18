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
package org.tinymediamanager.cli;

import static org.tinymediamanager.TinyMediaManager.shutdown;
import static org.tinymediamanager.TinyMediaManager.shutdownLogger;

import java.awt.GraphicsEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.http.TmmHttpServer;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.updater.UpdateCheck;
import org.tinymediamanager.updater.UpdaterTask;

import picocli.CommandLine;

// @formatter:off
@CommandLine.Command(
        name = "tinyMediaManager",
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        version = "tinyMediaManager CLI",
        synopsisHeading = "%nUsage:%n  ",
        optionListHeading = "%n",
        commandListHeading = "%nCommands:%n",
        footerHeading = "%nExamples:%n",
        footer = {
                "  tinyMediaManager movie -u -n -r      to find/scrape and rename new movies",
                "  tinyMediaManager movie -t -s         to download missing trailer/subtitles",
                "  tinyMediaManager movie -h            to display the help for the movie command",
                "",
                "  tinyMediaManager tvshow -u -n -r     to find/scrape and rename new TV shows/episodes",
                "  tinyMediaManager tvshow -t -s        to download missing trailer/subtitles",
                "  tinyMediaManager tvshow -h           to display the help for the tvshow command",
                "",
                "  tinyMediaManager --update            to download the latest updates for tinyMediaManager",
        },
        subcommands = {
                MovieCommand.class,
                TvShowCommand.class
        }
)
// @formatter:on
/**
 * the class {@link TinyMediaManagerCLI} handles all command line requests for tinyMediaManager
 *
 * @author Wolfgang Janes, Manuel Laggner
 */
public class TinyMediaManagerCLI implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(TinyMediaManagerCLI.class);

  @CommandLine.Option(names = { "--update" }, description = "Download the latest updates for tinyMediaManager")
  boolean                     update;

  @CommandLine.Option(names = { "--start-api" }, description = "Start the API server and keep tinyMediaManager running")
  boolean                     startApi;

  public static boolean checkArgs(String... args) {
    CommandLine cmd = new CommandLine(TinyMediaManagerCLI.class);
    try {
      cmd.parseArgs(args);
    }
    catch (CommandLine.ParameterException e) {
      try {
        cmd.getParameterExceptionHandler().handleParseException(e, args);
        return false;
      }
      catch (Exception e1) {
        LOGGER.warn("could not handle picocli exception - {}", e1.getMessage());
        return false;
      }
    }

    String fullCommand = String.join(" ", args);
    if (fullCommand.contains("-h")) {
      cmd.execute(args);
      return false;
    }

    return true;
  }

  public static void printHelp() {
    CommandLine cmd = new CommandLine(TinyMediaManagerCLI.class);
    cmd.execute("-h");
  }

  public static void start(String... args) {
    CommandLine cmd = new CommandLine(TinyMediaManagerCLI.class);
    cmd.execute(args);
  }

  private TinyMediaManagerCLI() {
  }

  @Override
  public void run() {
    if (update) {
      LOGGER.info("Checking for new updates...");

      if (new UpdateCheck().isUpdateAvailable()) {
        LOGGER.info("New update available - downloading...");

        UpdaterTask updaterTask = new UpdaterTask();
        updaterTask.doInBackground();

        if (updaterTask.isDownloadSuccessful()) {
          LOGGER.info("Update downloaded successful - restart to apply");
        }
      }
    }

    if (startApi) {
      // init http server
      if (Settings.getInstance().isEnableHttpServer()) {
        // register the shutdown handler
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          LOGGER.info("received shutdown signal");

          // save window layout
          if (!GraphicsEnvironment.isHeadless()) {
            MainWindow.getInstance().saveWindowLayout();
          }

          shutdown();
          shutdownLogger();
        }));

        try {
          // no need for start, because after creation the server is automatically started
          TmmHttpServer.getInstance();

          // endless loop - blocks here
          Thread.sleep(Long.MAX_VALUE);
        }
        catch (InterruptedException e) {
          // just finish
          System.out.println("stop");
        }
        catch (Exception e) {
          LOGGER.error("could not start webserver: {}", e.getMessage());
        }
      }
      else {
        LOGGER.error("HTTP API is not enabled in the settings!");
      }
    }
  }
}
