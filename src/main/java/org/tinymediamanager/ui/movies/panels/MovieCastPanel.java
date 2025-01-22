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
package org.tinymediamanager.ui.movies.panels;

import static org.tinymediamanager.core.Constants.ACTORS;
import static org.tinymediamanager.core.Constants.PRODUCERS;

import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.ActorImageLabel;
import org.tinymediamanager.ui.components.PersonTable;
import org.tinymediamanager.ui.components.ProducerImageLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.movies.MovieSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * the class {@link MovieCastPanel} to display the movie actors, writer and director
 * 
 * @author Manuel Laggner
 */
public class MovieCastPanel extends JPanel {
  private final MovieSelectionModel selectionModel;

  private final EventList<Person>   actorEventList;
  private final EventList<Person>   producerEventList;

  /**
   * UI elements
   */
  private JLabel                    lblDirector;
  private JLabel                    lblWriter;
  private ActorImageLabel           lblActorThumb;
  private ProducerImageLabel        lblProducerThumb;
  private TmmTable                  tableProducer;
  private TmmTable                  tableActors;

  public MovieCastPanel(MovieSelectionModel model) {
    selectionModel = model;
    producerEventList = GlazedLists.threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(Person.class)));
    actorEventList = GlazedLists.threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(Person.class)));

    initComponents();
    initDataBindings();

    lblActorThumb.enableLightbox();
    lblProducerThumb.enableLightbox();

    lblActorThumb.setCacheUrl(true);
    lblProducerThumb.setCacheUrl(true);

    // selectionlistener for the selected actor
    tableActors.getSelectionModel().addListSelectionListener(arg0 -> {
      if (!arg0.getValueIsAdjusting()) {
        int selectedRow = tableActors.convertRowIndexToModel(tableActors.getSelectedRow());
        if (selectedRow >= 0 && selectedRow < actorEventList.size()) {
          Person actor = actorEventList.get(selectedRow);
          lblActorThumb.setActor(selectionModel.getSelectedMovie(), actor);
        }
        else {
          lblActorThumb.clearImage();
        }
      }
    });

    // selectionlistener for the selected producer
    tableProducer.getSelectionModel().addListSelectionListener(arg0 -> {
      if (!arg0.getValueIsAdjusting()) {
        int selectedRow = tableProducer.convertRowIndexToModel(tableProducer.getSelectedRow());
        if (selectedRow >= 0 && selectedRow < producerEventList.size()) {
          Person producer = producerEventList.get(selectedRow);
          lblProducerThumb.setProducer(selectionModel.getSelectedMovie(), producer);
        }
        else {
          lblProducerThumb.clearImage();
        }
      }
    });

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != MovieSelectionModel.class) {
        return;
      }

      // react on selection of a movie and change of a movie
      if ("selectedMovie".equals(property) || ACTORS.equals(property)) {
        actorEventList.clear();
        actorEventList.addAll(selectionModel.getSelectedMovie().getActors());
        if (!actorEventList.isEmpty()) {
          tableActors.getSelectionModel().setSelectionInterval(0, 0);
        }
      }
      if ("selectedMovie".equals(property) || PRODUCERS.equals(property)) {
        producerEventList.clear();
        producerEventList.addAll(selectionModel.getSelectedMovie().getProducers());
        if (!producerEventList.isEmpty()) {
          tableProducer.getSelectionModel().setSelectionInterval(0, 0);
        }
      }
    };

    selectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  @Override
  public String getName() {
    return "movie.moviecast";
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[][400lp,grow][150lp,grow]", "[][][150lp:200lp,grow][][150lp:200lp,grow]"));
    {
      JLabel lblDirectorT = new TmmLabel(TmmResourceBundle.getString("metatag.director"));
      add(lblDirectorT, "cell 0 0");

      lblDirector = new JLabel("");
      add(lblDirector, "cell 1 0 2 1,growx,wmin 0");
    }
    {
      JLabel lblWriterT = new TmmLabel(TmmResourceBundle.getString("metatag.writer"));
      add(lblWriterT, "cell 0 1");

      lblWriter = new JLabel("");
      add(lblWriter, "cell 1 1 2 1,growx,wmin 0");
    }
    {
      JLabel lblProducersT = new TmmLabel(TmmResourceBundle.getString("metatag.producers"));
      add(lblProducersT, "cell 0 2,aligny top");

      tableProducer = new PersonTable(producerEventList) {
        @Override
        public void onPersonChanged(Person person) {
          super.onPersonChanged(person);
          MovieCastPanel.this.selectionModel.getSelectedMovie().saveToDb();
          MovieCastPanel.this.selectionModel.getSelectedMovie().writeNFO();
        }
      };
      tableProducer.setName(getName() + ".producerTable");
      TmmUILayoutStore.getInstance().install(tableProducer);
      JScrollPane scrollPanePerson = new JScrollPane();
      tableProducer.configureScrollPane(scrollPanePerson);
      add(scrollPanePerson, "cell 1 2,grow");
    }
    {
      lblProducerThumb = new ProducerImageLabel();
      add(lblProducerThumb, "cell 2 2,grow");
    }
    {
      JSeparator separator = new JSeparator();
      add(separator, "cell 0 3 3 1, growx");
    }
    {
      JLabel lblActorsT = new TmmLabel(TmmResourceBundle.getString("metatag.actors"));
      add(lblActorsT, "cell 0 4,aligny top");

      tableActors = new PersonTable(actorEventList) {
        @Override
        public void onPersonChanged(Person person) {
          super.onPersonChanged(person);
          MovieCastPanel.this.selectionModel.getSelectedMovie().saveToDb();
          MovieCastPanel.this.selectionModel.getSelectedMovie().writeNFO();
        }
      };
      tableActors.setName(getName() + ".actorTable");
      TmmUILayoutStore.getInstance().install(tableActors);
      JScrollPane scrollPanePersons = new JScrollPane();
      tableActors.configureScrollPane(scrollPanePersons);
      add(scrollPanePersons, "cell 1 4,grow");
    }
    {
      lblActorThumb = new ActorImageLabel();
      add(lblActorThumb, "cell 2 4,grow");
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSelectionModel, String> movieSelectionModelBeanProperty = BeanProperty.create("selectedMovie.directorsAsString");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<MovieSelectionModel, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel,
        movieSelectionModelBeanProperty, lblDirector, jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieSelectionModel, String> movieSelectionModelBeanProperty_1 = BeanProperty.create("selectedMovie.writersAsString");
    AutoBinding<MovieSelectionModel, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel,
        movieSelectionModelBeanProperty_1, lblWriter, jLabelBeanProperty);
    autoBinding_1.bind();
  }
}
