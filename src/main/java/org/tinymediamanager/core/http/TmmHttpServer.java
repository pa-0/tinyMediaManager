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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * the class {@link TmmHttpServer} is used to provide a restful server for HTTP access to tinyMediaManager
 *
 * @author Manuel Laggner
 */
public class TmmHttpServer {
  private static final Logger            LOGGER         = LoggerFactory.getLogger(TmmHttpServer.class);
  private static final String            CONTEXT_PREFIX = "/api/";

  private static TmmHttpServer           instance;

  private final ObjectWriter             objectWriter;
  private final Map<String, HttpHandler> contextMap;

  private HttpServer                     httpServer;
  private boolean                        running        = false;
  private String                         apiKey         = "";

  private TmmHttpServer() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setTimeZone(TimeZone.getDefault());
    objectWriter = objectMapper.writerFor(Response.class);

    contextMap = new LinkedHashMap<>();

    httpServer = HttpServer.create();

    // default context
    createContext("command", new CommandHandler());

    updateConfiguration(Settings.getInstance().isEnableHttpServer(), Settings.getInstance().getHttpServerPort(),
        Settings.getInstance().getHttpApiKey());
  }

  public void start() {
    httpServer.start();
    running = true;
  }

  private void stop() {
    httpServer.stop(1);
    running = false;
  }

  private void bindPort(int port) throws IOException {
    httpServer.bind(new InetSocketAddress(port), 0);
  }

  public static synchronized TmmHttpServer getInstance() throws Exception {
    if (instance == null) {
      instance = new TmmHttpServer();
    }

    return instance;
  }

  public void createContext(String contextPath, ITmmCommandHandler commandHandler) {
    HttpHandler httpHandler = httpExchange -> {
      int responseCode;
      String responseMessage;

      if (StringUtils.isNotBlank(this.apiKey)) {
        // API key check
        List<String> apiKeyFromRequest = httpExchange.getRequestHeaders().get("api-key");
        if (apiKeyFromRequest == null || !apiKeyFromRequest.contains(this.apiKey)) {
          responseCode = 403;
          responseMessage = "Invalid API key";
          sendResponse(responseCode, responseMessage, httpExchange);
          return;
        }
      }

      // delegate the request to the handler
      try {
        TmmCommandResponse commandResponse = commandHandler.post(httpExchange);
        responseCode = commandResponse.getResponseCode();
        responseMessage = commandResponse.getResponseMessage();
      }
      catch (Exception e) {
        LOGGER.error("could not process command '{}' - '{}'", commandHandler.getClass().getName(), e.getMessage());
        responseCode = 500;
        responseMessage = e.getMessage();
      }

      sendResponse(responseCode, responseMessage, httpExchange);

    };

    httpServer.createContext(CONTEXT_PREFIX + contextPath, httpHandler);
    contextMap.put(CONTEXT_PREFIX + contextPath, httpHandler);
  }

  private void sendResponse(int responseCode, String responseMessage, HttpExchange httpExchange) {
    Response response = new Response();
    response.message = responseMessage;

    try (OutputStream out = httpExchange.getResponseBody()) {
      String body = objectWriter.writeValueAsString(response);
      httpExchange.getResponseHeaders().add("Content-Type", "application/json");
      httpExchange.sendResponseHeaders(responseCode, body.length());
      out.write(body.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException ex) {
      LOGGER.error("could not send response - '{}'", ex.getMessage());
    }
  }

  public void updateConfiguration(boolean enabled, int port, String apiKey) throws IOException {
    if (!enabled && !running) {
      // not enabled and not running -> nothing to do
      return;
    }
    else if (!enabled && running) {
      // we want to shut down the running server
      stop();
      createNewServer();
      return;
    }

    if (httpServer.getAddress() == null || port != httpServer.getAddress().getPort()) {
      // change of the port - need to create a new server for that
      stop();
      createNewServer();
      bindPort(port);
      start();
    }

    this.apiKey = apiKey;
  }

  private void createNewServer() throws IOException {
    // we cannot re-use the http server once stopped
    httpServer = HttpServer.create();
    for (Map.Entry<String, HttpHandler> entry : contextMap.entrySet()) {
      httpServer.createContext(entry.getKey(), entry.getValue());
    }
  }

  public static class Response {
    public String message;
  }
}
