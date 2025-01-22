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
package org.tinymediamanager.ui.panels;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.ui.components.MediaTrailerTable;
import org.tinymediamanager.ui.components.table.NullSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TrailerPanel} is used to display trailers
 *
 * @author Manuel Laggner
 */
public abstract class TrailerPanel extends JPanel {
  protected MediaTrailerTable        table;
  protected SortedList<MediaTrailer> trailerEventList;

  protected void createLayout() {
    trailerEventList = new SortedList<>(
        GlazedListsSwing.swingThreadProxyList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MediaTrailer.class))));
    setLayout(new MigLayout("", "[400lp,grow]", "[250lp,grow]"));
    table = new MediaTrailerTable(trailerEventList, false) {
      @Override
      protected void downloadTrailer(MediaTrailer mediaTrailer) {
        TrailerPanel.this.downloadTrailer(mediaTrailer);
      }

      @Override
      protected String refreshUrlFromId(MediaTrailer trailer) {
        return TrailerPanel.this.refreshUrlFromId(trailer);
      }
    };
    table.setSelectionModel(new NullSelectionModel());
    table.installComparatorChooser(trailerEventList);

    JScrollPane scrollPane = new JScrollPane();
    table.configureScrollPane(scrollPane);
    add(scrollPane, "cell 0 0,grow");
    scrollPane.setViewportView(table);
  }

  protected abstract void downloadTrailer(MediaTrailer trailer);

  protected abstract String refreshUrlFromId(MediaTrailer trailer);
}
