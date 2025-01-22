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

import javax.swing.event.HyperlinkEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.tinymediamanager.ui.TmmUIHelper;

/**
 * Same as the normal pane, but this one renders text as HTML.<br>
 * So you get all http/www textual links as clickable HTML &lt;a href&gt; tags, and linebreaks to &lt;br/&gt;
 *
 * @author Myron Boyle
 */
public class ReadOnlyTextPaneHTML extends ReadOnlyTextPane {

  private static final long                    serialVersionUID = 1L;
  private static final Document.OutputSettings NO_PRETTYPRINT   = new Document.OutputSettings().prettyPrint(false);

  public ReadOnlyTextPaneHTML() {
    super();

    setContentType("text/html");

    addHyperlinkListener(e -> {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
        try {
          TmmUIHelper.browseUrl(e.getURL().toURI().toString());
        }
        catch (Exception ex) {
          // ex.printStackTrace();
        }
      }
    });
  }

  @Override
  public void setText(String t) {
    if (t == null || t.isEmpty()) {
      super.setText("<html></html>");
    }
    else {
      if (t.startsWith("<html>")) {
        // already HTML? just set correct content type - no replacement done here
        super.setText(t);
      }
      else {
        // performance: just do a quick contains, before doing all the fancy stuff
        if (t.contains("http") || t.contains("www")) {
          // remove all existing href tags, to not reHTMLify existing ones
          t = Jsoup.clean(t, "", Safelist.simpleText(), NO_PRETTYPRINT);

          t = t.replaceAll("\\n", " <br/> ");

          // with space around, so a line concatenating has a whitespace delimiter
          t = t.replaceAll("(?:https|http)://([^\\s]+)", "<a href=\"$0\">$1</a>");
          // whitespace before WWW to not include former style!!!
          t = t.replaceAll("(?:^|\\s)(www\\.[^\\s]+)", " <a href=\"https://$1\">$1</a>");

          super.setText("<html>" + t + "</html>");
        }
        else {
          // no HTML links found to upgrade
          t = t.replaceAll("\\n", " <br/> ");
          super.setText(t);
        }
      }
    }
  }
}
