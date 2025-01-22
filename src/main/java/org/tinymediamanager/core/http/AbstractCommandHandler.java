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
package org.tinymediamanager.core.http;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.sun.net.httpserver.HttpExchange;

public abstract class AbstractCommandHandler implements ITmmCommandHandler {

  private final ObjectReader commandsObjectReader;
  private final ObjectReader singleCommandObjectReader;

  protected AbstractCommandHandler() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setTimeZone(TimeZone.getDefault());
    commandsObjectReader = objectMapper.readerFor(new TypeReference<List<Command>>() {
    });
    singleCommandObjectReader = objectMapper.readerFor(Command.class);
  }

  @Override
  public TmmCommandResponse post(HttpExchange httpExchange) throws Exception {
    List<Command> commands;

    String postContent = "";

    try (InputStream is = httpExchange.getRequestBody()) {
      postContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      commands = new ArrayList<>(commandsObjectReader.readValue(postContent));
    }
    catch (Exception e) {
      // maybe just one command sent?
      Command command = singleCommandObjectReader.readValue(postContent);
      commands = Collections.singletonList(command);
    }

    return processCommands(commands);
  }

  /**
   * process the commands offered by the JSON payload
   * 
   * @param commands
   *          a {@link List} of all {@link Command}s to process
   * @return the {@link TmmCommandResponse} to return
   * @throws Exception
   *           any {@link Exception} occurred while preparing the commands
   */
  protected abstract TmmCommandResponse processCommands(List<Command> commands) throws Exception;

  public static class Command {
    public String              action;
    public CommandScope        scope = new CommandScope();
    public Map<String, String> args  = new HashMap<>();
  }

  public static class CommandScope {
    public String   name;
    public String[] args;
  }
}
