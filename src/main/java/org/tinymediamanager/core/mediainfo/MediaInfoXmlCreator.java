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
package org.tinymediamanager.core.mediainfo;

import java.io.File;
import java.io.FileWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.thirdparty.MediaInfo;
import org.tinymediamanager.thirdparty.MediaInfo.StreamKind;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MediaInfoXmlCreator {
  private static final Logger       LOGGER                        = LoggerFactory.getLogger(MediaInfoXmlCreator.class);
  private static final String       ORACLE_IS_STANDALONE          = "http://www.oracle.com/xml/is-standalone";
  private static final Pattern      FIRST_CHARACTER_DIGIT_PATTERN = Pattern.compile("^\\d");

  private Document                  document;

  private final MediaFile           mediaFile;
  private final List<MediaInfoFile> mediaInfoFiles;

  public MediaInfoXmlCreator(MediaFile mediaFile, List<MediaInfoFile> mediaInfoFiles) {
    this.mediaFile = mediaFile;
    this.mediaInfoFiles = mediaInfoFiles;
  }

  public void write() throws Exception {
    // create the new NFO file according to the specifications
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // NOSONAR
    document = factory.newDocumentBuilder().newDocument();
    document.setXmlStandalone(true);

    // tmm comment
    Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String dat = formatter.format(new Date());
    document.appendChild(document.createComment("created on " + dat + " - tinyMediaManager " + Settings.getInstance().getVersion()));

    Element mediaInfo = document.createElement("MediaInfo");
    mediaInfo.setAttribute("xmlns", "https://mediaarea.net/mediainfo");
    mediaInfo.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    mediaInfo.setAttribute("version", "2.0");
    document.appendChild(mediaInfo);

    // creating library
    Element creatingLibrary = document.createElement("creatingLibrary");
    creatingLibrary.setAttribute("version", ReleaseInfo.getVersion());
    creatingLibrary.setAttribute("url", "https://www.tinymediamanager.org");
    creatingLibrary.setTextContent("tinyMediaManager");
    mediaInfo.appendChild(creatingLibrary);

    // if we have no video/audio track, and ONLY a general track, it is not valid!!!
    boolean hasValidTracks = false;

    for (MediaInfoFile mediaInfoFile : mediaInfoFiles) {
      Element media = document.createElement("media");
      File file = new File(mediaInfoFile.getPath(), mediaInfoFile.getFilename());
      media.setAttribute("ref", file.getAbsolutePath());

      for (Map.Entry<MediaInfo.StreamKind, List<Map<String, String>>> entry : mediaInfoFile.getSnapshot().entrySet()) {
        for (Map<String, String> map : entry.getValue()) {
          Element track = document.createElement("track");
          track.setAttribute("type", entry.getKey().name());

          switch (StreamKind.valueOf(entry.getKey().name())) {
            case Audio:
            case Video:
            case Text:
              hasValidTracks = true;
            default:
              break;
          }

          String streamKindPos = map.get("StreamKindPos");
          if (StringUtils.isNotBlank(streamKindPos)) {
            track.setAttribute("typeorder", streamKindPos);
          }
          map.forEach((tag, content) -> addItems(track, tag, content));

          media.appendChild(track);
        }
      }

      mediaInfo.appendChild(media);
    }
    if (!hasValidTracks) {
      return;
    }
    File file = new File(mediaFile.getPath(), mediaFile.getBasename() + "-mediainfo.xml");
    try (FileWriter out = new FileWriter(file)) {
      getTransformer().transform(new DOMSource(document), new StreamResult(out));
    }
  }

  private void addItems(Element track, String tag, String textContent) {
    try {
      Element item = document.createElement(cleanTag(tag));
      item.setTextContent(cleanTextContent(tag, textContent));
      track.appendChild(item);
    }
    catch (Exception e) {
      LOGGER.trace("Could not write tag {}={} - {}", tag, textContent, e.getMessage());
    }
  }

  private String cleanTag(String tag) {
    // remove ()
    String normalizedTag = tag.replace("(", ""); // faster than replaceAll
    normalizedTag = normalizedTag.replace(")", ""); // faster than replaceAll
    // replace /*:. with _
    normalizedTag = normalizedTag.replace('/', '_'); // faster than replaceAll
    normalizedTag = normalizedTag.replace('*', '_'); // faster than replaceAll
    normalizedTag = normalizedTag.replace(':', '_'); // faster than replaceAll
    normalizedTag = normalizedTag.replace('.', '_'); // faster than replaceAll
    normalizedTag = normalizedTag.replace(' ', '_'); // no spaces allowed

    // first character must not be a digit
    if (FIRST_CHARACTER_DIGIT_PATTERN.matcher(normalizedTag).find()) {
      normalizedTag = "_" + normalizedTag;
    }

    return normalizedTag;
  }

  /**
   * clean the text content if there is a need to
   * 
   * @param tag
   *          the tag for this text content
   * @param textContent
   *          the text content to tbe cleaned
   * @return the cleaned text content (or the original if there i no cleaning needed)
   */
  private String cleanTextContent(String tag, String textContent) {
    // divide the duration by 1000 and re-format it
    if ("duration".equalsIgnoreCase(tag)) {
      try {
        double value = Double.parseDouble(textContent);
        value = value / 1000;
        return String.format(Locale.US, "%.3f", value);
      }
      catch (Exception e) {
        LOGGER.trace("could not re-format duration - {}", e.getMessage());
      }
    }

    return stripNonValidXMLCharacters(textContent);
  }

  public String stripNonValidXMLCharacters(String in) {
    StringBuffer out = new StringBuffer(); // Used to hold the output.
    char current; // Used to reference the current character.

    if (in == null || ("".equals(in)))
      return ""; // vacancy test.
    for (int i = 0; i < in.length(); i++) {
      current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
      if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
          || ((current >= 0xE000) && (current <= 0xFFFD)) || ((current >= 0x10000) && (current <= 0x10FFFF)))
        out.append(current);
    }
    return out.toString();
  }

  /**
   * get the transformer for XML output
   *
   * @return the transformer
   * @throws Exception
   *           any Exception that has been thrown
   */
  private Transformer getTransformer() throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer(); // NOSONAR

    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    // not supported in all JVMs
    try {
      transformer.setOutputProperty(ORACLE_IS_STANDALONE, "yes");
    }
    catch (Exception ignored) {
      // okay, seems we're not on OracleJDK, OpenJDK or AdopOpenJDK
    }

    return transformer;
  }
}
