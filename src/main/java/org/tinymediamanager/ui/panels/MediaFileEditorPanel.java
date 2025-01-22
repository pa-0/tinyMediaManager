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

import static org.tinymediamanager.core.MediaFileType.AUDIO;
import static org.tinymediamanager.core.MediaFileType.NFO;
import static org.tinymediamanager.core.MediaFileType.SAMPLE;
import static org.tinymediamanager.core.MediaFileType.SUBTITLE;
import static org.tinymediamanager.core.MediaFileType.TRAILER;
import static org.tinymediamanager.core.MediaFileType.VIDEO;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Converter;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.AspectRatio;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.tasks.ARDetectorTask;
import org.tinymediamanager.core.tasks.MediaFileARDetectorTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskListener;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.thirdparty.FFmpeg;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.IntegerInputVerifier;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.MediaFileAudioStreamEditTable;
import org.tinymediamanager.ui.components.MediaFileSubtitleEditTable;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import net.miginfocom.swing.MigLayout;

/**
 * The class MediaFileEditorPanel is used to maintain associated media files
 *
 * @author Manuel Laggner
 */
public class MediaFileEditorPanel extends JPanel {
  private final BindingGroup                    bindingGroup  = new BindingGroup();

  private final List<MediaFileContainer>        mediaFiles;
  private final TmmTable                        tableMediaFiles;
  private final JLabel                          lblFilename;
  private final JTextField                      tfCodec;
  private final JTextField                      tfContainerFormat;
  private final JTextField                      tfWidth;
  private final JTextField                      tfHeight;
  private final MediaFileAudioStreamEditTable   tableAudioStreams;
  private final MediaFileSubtitleEditTable      tableSubtitles;
  private final JButton                         btnAddAudioStream;
  private final JButton                         btnRemoveAudioStream;
  private final JButton                         btnAddSubtitle;
  private final JButton                         btnRemoveSubtitle;
  private final JComboBox<String>               cb3dFormat;
  private final JComboBox                       cbAspectRatio;
  private final JComboBox                       cbAspectRatio2;
  private final JSpinner                        spFrameRate;
  private final JTextField                      tfBitDepth;
  private final JTextField                      tfHdrFormat;
  private final JTextField                      tfVideoBitrate;
  private final JTextField                      tfRuntime;
  private final JButton                         btnARD;

  private final EventList<MediaFileAudioStream> audioStreams;
  private final EventList<MediaFileSubtitle>    subtitles;
  private final List<AspectRatioContainer>      aspectRatios  = new ArrayList<>();
  private final List<AspectRatioContainer>      aspectRatios2 = new ArrayList<>();

  private TmmTask                               ardTask;

  public MediaFileEditorPanel(List<MediaFile> mediaFiles) {
    this.mediaFiles = ObservableCollections.observableList(new ArrayList<>());
    for (MediaFile mediaFile : mediaFiles) {
      MediaFileContainer container = new MediaFileContainer(mediaFile);
      this.mediaFiles.add(container);
    }
    audioStreams = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()),
        GlazedLists.beanConnector(MediaFileAudioStream.class));
    subtitles = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(MediaFileSubtitle.class));

    Set<MediaFileType> videoTypes = new HashSet<>(Arrays.asList(VIDEO, SAMPLE, TRAILER));

    // predefined 3D Formats
    String[] threeDFormats = { "", MediaFileHelper.VIDEO_3D, MediaFileHelper.VIDEO_3D_SBS, MediaFileHelper.VIDEO_3D_HSBS,
        MediaFileHelper.VIDEO_3D_TAB, MediaFileHelper.VIDEO_3D_HTAB, MediaFileHelper.VIDEO_3D_MVC };

    setLayout(new MigLayout("", "[300lp:450lp,grow]", "[200lp:450lp,grow]"));
    {
      JSplitPane splitPane = new JSplitPane();
      splitPane.setName("mediafileEditor.splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      add(splitPane, "cell 0 0,grow");
      {
        JPanel panelMediaFiles = new JPanel();
        panelMediaFiles.setLayout(new MigLayout("", "[200lp:250lp,grow]", "[200lp:300lp,grow]"));

        JScrollPane scrollPaneMediaFiles = new JScrollPane();
        panelMediaFiles.add(scrollPaneMediaFiles, "cell 0 0,grow");
        splitPane.setLeftComponent(panelMediaFiles);

        tableMediaFiles = new TmmTable();
        tableMediaFiles.configureScrollPane(scrollPaneMediaFiles);
      }
      {
        JPanel panelDetails = new JPanel();
        splitPane.setRightComponent(panelDetails);
        panelDetails
            .setLayout(new MigLayout("", "[][75lp:n][20lp:n][][75lp:n][20lp:n][][75lp:n][50lp:n,grow]", "[][][][][][][100lp:150lp][100lp:150lp]"));
        {
          lblFilename = new JLabel("");
          TmmFontHelper.changeFont(lblFilename, 1.167, Font.BOLD);
          panelDetails.add(lblFilename, "cell 0 0 9 1,growx");
        }
        {
          JLabel lblCodec = new TmmLabel(TmmResourceBundle.getString("metatag.codec"));
          panelDetails.add(lblCodec, "cell 0 1,alignx right");

          tfCodec = new JTextField();
          panelDetails.add(tfCodec, "cell 1 1,growx");
          tfCodec.setColumns(10);
        }
        {
          JLabel lblContainerFormat = new TmmLabel(TmmResourceBundle.getString("metatag.container"));
          panelDetails.add(lblContainerFormat, "cell 3 1,alignx right");

          tfContainerFormat = new JTextField();
          panelDetails.add(tfContainerFormat, "cell 4 1,growx");
          tfContainerFormat.setColumns(10);
        }
        {
          JLabel lblWidth = new TmmLabel(TmmResourceBundle.getString("metatag.width"));
          panelDetails.add(lblWidth, "cell 0 2,alignx right");

          tfWidth = new JTextField();
          tfWidth.setInputVerifier(new IntegerInputVerifier());
          panelDetails.add(tfWidth, "cell 1 2,growx");
          tfWidth.setColumns(10);
        }
        {
          JLabel lblHeight = new TmmLabel(TmmResourceBundle.getString("metatag.height"));
          panelDetails.add(lblHeight, "cell 3 2,alignx right");

          tfHeight = new JTextField();
          tfHeight.setInputVerifier(new IntegerInputVerifier());
          panelDetails.add(tfHeight, "cell 4 2,growx");
          tfHeight.setColumns(10);
        }
        {
          btnARD = new JButton(new ScanAspectRationAction());
          btnARD.setFocusable(false);
          MediaFile mf = MediaFileEditorPanel.this.mediaFiles.get(0).mediaFile;
          btnARD.setEnabled(videoTypes.contains(mf.getType()));
          panelDetails.add(btnARD, "cell 7 1");
        }
        {
          JLabel lblAspectT = new TmmLabel(TmmResourceBundle.getString("metatag.aspect"));
          panelDetails.add(lblAspectT, "cell 6 2,alignx right");

          cbAspectRatio = new JComboBox(getAspectRatios().toArray(new AspectRatioContainer[0]));
          panelDetails.add(cbAspectRatio, "cell 7 2");
        }
        {
          JLabel lblAspectT = new TmmLabel(TmmResourceBundle.getString("metatag.aspect2"));
          panelDetails.add(lblAspectT, "cell 6 3,alignx right");

          cbAspectRatio2 = new JComboBox(getAspectRatios2().toArray(new AspectRatioContainer[0]));
          panelDetails.add(cbAspectRatio2, "cell 7 3");
        }
        {
          JLabel lblFrameRate = new TmmLabel(TmmResourceBundle.getString("metatag.framerate"));
          panelDetails.add(lblFrameRate, "cell 0 3,alignx trailing");

          spFrameRate = new JSpinner(new SpinnerNumberModel(0, 0, 999, 0.01d));
          panelDetails.add(spFrameRate, "cell 1 3,growx");
        }
        {
          JLabel lblVideoBitrate = new TmmLabel(TmmResourceBundle.getString("metatag.bitrate"));
          panelDetails.add(lblVideoBitrate, "cell 3 3,alignx trailing");

          tfVideoBitrate = new JTextField();
          panelDetails.add(tfVideoBitrate, "cell 4 3");
          tfVideoBitrate.setColumns(10);
          tfVideoBitrate.setInputVerifier(new IntegerInputVerifier());
        }
        {
          JLabel lblBitDepthT = new TmmLabel(TmmResourceBundle.getString("metatag.videobitdepth"));
          panelDetails.add(lblBitDepthT, "cell 0 4,alignx trailing");

          tfBitDepth = new JTextField();
          tfBitDepth.setInputVerifier(new IntegerInputVerifier());
          panelDetails.add(tfBitDepth, "cell 1 4,growx");
          tfBitDepth.setColumns(10);
        }
        {
          JLabel lblHdrFormatT = new TmmLabel(TmmResourceBundle.getString("metatag.hdrformat"));
          panelDetails.add(lblHdrFormatT, "cell 3 4,alignx trailing");

          tfHdrFormat = new JTextField();
          panelDetails.add(tfHdrFormat, "cell 4 4,growx");
          tfHdrFormat.setColumns(10);
        }
        {
          JLabel lblRuntimeT = new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
          panelDetails.add(lblRuntimeT, "cell 0 5,alignx trailing");

          tfRuntime = new JTextField();
          panelDetails.add(tfRuntime, "cell 1 5");
          tfRuntime.setColumns(5);
          tfRuntime.setInputVerifier(new IntegerInputVerifier());
        }
        {
          JLabel lbl3d = new TmmLabel(TmmResourceBundle.getString("metatag.3dformat"));
          panelDetails.add(lbl3d, "cell 3 5,alignx right");

          cb3dFormat = new JComboBox(threeDFormats);
          panelDetails.add(cb3dFormat, "cell 4 5");
        }
        {
          JLabel lblAudiostreams = new TmmLabel(TmmResourceBundle.getString("metatag.countAudioStreams"));
          panelDetails.add(lblAudiostreams, "flowy,cell 0 6,alignx right,aligny top");

          JScrollPane scrollPane = new JScrollPane();
          panelDetails.add(scrollPane, "cell 1 6 8 1,grow");

          tableAudioStreams = new MediaFileAudioStreamEditTable(audioStreams) {
            @Override
            public void onAddAudioStream() {
              syncAudioStreams();
            }

            @Override
            public void onEditAudioStream() {
              syncAudioStreams();
            }
          };
          tableAudioStreams.configureScrollPane(scrollPane);
          tableAudioStreams.setFocusable(false);
        }
        {
          JLabel lblSubtitles = new TmmLabel(TmmResourceBundle.getString("metatag.subtitles"));
          panelDetails.add(lblSubtitles, "flowy,cell 0 7,alignx right,aligny top");

          JScrollPane scrollPane = new JScrollPane();
          panelDetails.add(scrollPane, "cell 1 7 8 1,grow");

          tableSubtitles = new MediaFileSubtitleEditTable(subtitles) {
            @Override
            public void onEditSubtitle() {
              syncSubtitles();
            }

            @Override
            public void onAddSubtitle() {
              syncSubtitles();
            }
          };
          tableSubtitles.configureScrollPane(scrollPane);
          tableSubtitles.setFocusable(false);
        }
        {
          btnAddAudioStream = new SquareIconButton(new AddAudioStreamAction());
          btnAddAudioStream.setFocusable(false);
          panelDetails.add(btnAddAudioStream, "cell 0 6,alignx right,aligny top");

          btnRemoveAudioStream = new SquareIconButton(new RemoveAudioStreamAction());
          btnRemoveAudioStream.setFocusable(false);
          panelDetails.add(btnRemoveAudioStream, "cell 0 6,alignx right,aligny top");
        }
        {
          btnAddSubtitle = new SquareIconButton(new AddSubtitleAction());
          btnAddSubtitle.setFocusable(false);
          panelDetails.add(btnAddSubtitle, "cell 0 7,alignx right,aligny top");

          btnRemoveSubtitle = new SquareIconButton(new RemoveSubtitleAction());
          btnRemoveSubtitle.setFocusable(false);
          panelDetails.add(btnRemoveSubtitle, "cell 0 7,alignx right,aligny top");
        }
      }
    }

    initDataBindings();

    // add selection listener to disable editing when needed
    tableMediaFiles.getSelectionModel().addListSelectionListener(listener -> {
      if (!listener.getValueIsAdjusting()) {
        int selectedRow = tableMediaFiles.convertRowIndexToModel(tableMediaFiles.getSelectedRow());
        if (selectedRow > -1) {
          MediaFileContainer container = MediaFileEditorPanel.this.mediaFiles.get(selectedRow);
          // codec should not be enabled for NFOs
          tfCodec.setEnabled(container.mediaFile.getType() != NFO);

          // audio streams and subtitles should not be enabled for anything except VIDEOS/TRAILER/SAMPLES/AUDIO/SUBTITLES
          btnAddAudioStream.setEnabled(videoTypes.contains(container.mediaFile.getType()) || container.mediaFile.getType() == AUDIO);
          btnRemoveAudioStream.setEnabled(videoTypes.contains(container.mediaFile.getType()) || container.mediaFile.getType() == AUDIO);
          btnAddSubtitle.setEnabled(videoTypes.contains(container.mediaFile.getType()) || container.mediaFile.getType() == SUBTITLE);
          btnRemoveSubtitle.setEnabled(videoTypes.contains(container.mediaFile.getType()) || container.mediaFile.getType() == SUBTITLE);

          // 3D is only available for video types
          cb3dFormat.setEnabled(videoTypes.contains(container.mediaFile.getType()));

          // runtime is also only available for video types
          tfRuntime.setEnabled(videoTypes.contains(container.mediaFile.getType()));
          btnARD.setEnabled(videoTypes.contains(container.mediaFile.getType()) && FFmpeg.isAvailable());

          audioStreams.clear();
          audioStreams.addAll(container.getAudioStreams());
          tableAudioStreams.adjustColumnPreferredWidths(6);

          subtitles.clear();
          subtitles.addAll(container.getSubtitles());
          tableSubtitles.adjustColumnPreferredWidths(6);
        }
      }
    });

    // select first
    if (!this.mediaFiles.isEmpty()) {
      tableMediaFiles.getSelectionModel().setSelectionInterval(0, 0);
    }
  }

  private List<AspectRatioContainer> getAspectRatios() {
    if (aspectRatios.isEmpty()) {
      aspectRatios.add(new AspectRatioContainer(0f, TmmResourceBundle.getString("aspectratio.calculated")));
      for (Float ar : AspectRatio.getDefaultValues()) {
        aspectRatios.add(new AspectRatioContainer(ar));
      }
    }
    return aspectRatios;
  }

  private List<AspectRatioContainer> getAspectRatios2() {
    if (aspectRatios2.isEmpty()) {
      aspectRatios2.add(new AspectRatioContainer(null, TmmResourceBundle.getString("aspectratio.nomultiformat")));
      for (Float ar : AspectRatio.getDefaultValues()) {
        aspectRatios2.add(new AspectRatioContainer(ar));
      }
    }
    return aspectRatios2;
  }

  private void syncAudioStreams() {
    int selectedRow = tableMediaFiles.convertRowIndexToModel(tableMediaFiles.getSelectedRow());
    if (selectedRow > -1) {
      MediaFileContainer container = MediaFileEditorPanel.this.mediaFiles.get(selectedRow);
      container.setAudioStreams(audioStreams);
    }
  }

  private void syncSubtitles() {
    int selectedRow = tableMediaFiles.convertRowIndexToModel(tableMediaFiles.getSelectedRow());
    if (selectedRow > -1) {
      MediaFileContainer container = MediaFileEditorPanel.this.mediaFiles.get(selectedRow);
      container.setSubtitles(subtitles);
    }
  }

  private class AddAudioStreamAction extends AbstractAction {
    public AddAudioStreamAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("audiostream.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tableAudioStreams.addAudioStream();
      syncAudioStreams();
    }
  }

  private class RemoveAudioStreamAction extends AbstractAction {
    public RemoveAudioStreamAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("audiostream.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int[] audioRows = convertSelectedRowsToModelRows(tableAudioStreams);
      for (int index : audioRows) {
        audioStreams.remove(index);
      }
      syncAudioStreams();
    }
  }

  private class AddSubtitleAction extends AbstractAction {
    public AddSubtitleAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("subtitle.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tableSubtitles.addSubtitle();
      syncSubtitles();
    }
  }

  private class RemoveSubtitleAction extends AbstractAction {
    public RemoveSubtitleAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("subtitle.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int[] subtitleRows = convertSelectedRowsToModelRows(tableSubtitles);
      for (int index : subtitleRows) {
        subtitles.remove(index);
      }
      syncSubtitles();
    }
  }

  private class ScanAspectRationAction extends AbstractAction implements TmmTaskListener {
    public ScanAspectRationAction() {
      putValue(NAME, TmmResourceBundle.getString("task.ard"));
      putValue(SMALL_ICON, IconManager.ASPECT_RATIO);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int mediaFileRow = tableMediaFiles.getSelectedRow();
      if (mediaFileRow > -1) {
        mediaFileRow = tableMediaFiles.convertRowIndexToModel(mediaFileRow);
        MediaFileContainer mf = mediaFiles.get(mediaFileRow);

        ARDetectorTask task = new MediaFileARDetectorTask(mf.getMediaFile());
        task.addListener(this);
        TmmTaskManager.getInstance().addUnnamedTask(task);
        MediaFileEditorPanel.this.ardTask = task;
      }
    }

    @Override
    public void processTaskEvent(TmmTaskHandle task) {
      if (TmmTaskHandle.TaskState.QUEUED.equals(task.getState())) {
        btnARD.setEnabled(false);
      }
      else if (TmmTaskHandle.TaskState.FINISHED.equals(task.getState())) {
        btnARD.setEnabled(true);
      }
    }
  }

  private int[] convertSelectedRowsToModelRows(JTable table) {
    int[] tableRows = table.getSelectedRows();
    int[] modelRows = new int[tableRows.length];
    for (int i = 0; i < tableRows.length; i++) {
      modelRows[i] = table.convertRowIndexToModel(tableRows[i]);
    }

    // sort it (descending)
    ArrayUtils.reverse(modelRows);
    return modelRows;
  }

  /*
   * Container needed to make the audio streams and subtitles editable
   */
  public static class MediaFileContainer {
    private final MediaFile                  mediaFile;
    private final List<MediaFileAudioStream> audioStreams;
    private final List<MediaFileSubtitle>    subtitles;

    private MediaFileContainer(MediaFile mediaFile) {
      this.mediaFile = mediaFile;
      this.audioStreams = ObservableCollections.observableList(new ArrayList<>(mediaFile.getAudioStreams()));
      this.subtitles = ObservableCollections.observableList(new ArrayList<>(mediaFile.getSubtitles()));
    }

    public MediaFile getMediaFile() {
      return mediaFile;
    }

    public List<MediaFileAudioStream> getAudioStreams() {
      return audioStreams;
    }

    public void setAudioStreams(List<MediaFileAudioStream> audioStreams) {
      this.audioStreams.clear();
      this.audioStreams.addAll(audioStreams);
      mediaFile.setAudioStreams(audioStreams);
    }

    public List<MediaFileSubtitle> getSubtitles() {
      return subtitles;
    }

    public void setSubtitles(List<MediaFileSubtitle> subtitles) {
      this.subtitles.clear();
      this.subtitles.addAll(subtitles);
      mediaFile.setSubtitles(subtitles);
    }
  }

  /**
   * Sync media files edited from this editor with the ones from the media entity without removing/adding all of them
   * 
   * @param mfsFromEditor
   *          the edited media files
   * @param mfsFromMediaEntity
   *          the original media files
   */
  public static void syncMediaFiles(List<MediaFile> mfsFromEditor, List<MediaFile> mfsFromMediaEntity) {
    for (MediaFile mfEditor : mfsFromEditor) {
      for (MediaFile mfOriginal : mfsFromMediaEntity) {
        if (mfEditor.equals(mfOriginal)) {
          // here we check all field which can be edited from the editor
          if (!mfEditor.getVideoCodec().equals(mfOriginal.getVideoCodec())) {
            mfOriginal.setVideoCodec(mfEditor.getVideoCodec());
          }
          if (!mfEditor.getContainerFormat().equals(mfOriginal.getContainerFormat())) {
            mfOriginal.setContainerFormat(mfEditor.getContainerFormat());
          }
          if (mfEditor.getVideoWidth() != mfOriginal.getVideoWidth()) {
            mfOriginal.setVideoWidth(mfEditor.getVideoWidth());
          }
          if (mfEditor.getVideoHeight() != mfOriginal.getVideoHeight()) {
            mfOriginal.setVideoHeight(mfEditor.getVideoHeight());
          }
          if (MetadataUtil.unboxFloat(mfEditor.getAspectRatio()) != MetadataUtil.unboxFloat(mfOriginal.getAspectRatio())) {
            mfOriginal.setAspectRatio(mfEditor.getAspectRatio());
          }
          if (MetadataUtil.unboxFloat(mfEditor.getAspectRatio2()) != MetadataUtil.unboxFloat(mfOriginal.getAspectRatio2())) {
            mfOriginal.setAspectRatio2(mfEditor.getAspectRatio2());
          }
          if (mfEditor.getFrameRate() != mfOriginal.getFrameRate()) {
            mfOriginal.setFrameRate(mfEditor.getFrameRate());
          }
          if (!mfEditor.getVideo3DFormat().equals(mfOriginal.getVideo3DFormat())) {
            mfOriginal.setVideo3DFormat(mfEditor.getVideo3DFormat());
          }
          if (!mfEditor.getHdrFormat().equals(mfOriginal.getHdrFormat())) {
            mfOriginal.setHdrFormat(mfEditor.getHdrFormat());
          }
          if (mfEditor.getBitDepth() != mfOriginal.getBitDepth()) {
            mfOriginal.setBitDepth(mfEditor.getBitDepth());
          }
          if (mfEditor.getOverallBitRate() != mfOriginal.getOverallBitRate()) {
            mfOriginal.setOverallBitRate(mfEditor.getOverallBitRate());
          }
          if (mfEditor.getDuration() != mfOriginal.getDuration()) {
            mfOriginal.setDuration(mfEditor.getDuration());
          }

          // audio streams and subtitles will be completely set (except empty ones)
          List<MediaFileAudioStream> audioStreams = new ArrayList<>();
          for (MediaFileAudioStream audioStream : mfEditor.getAudioStreams()) {
            if (StringUtils.isBlank(audioStream.getCodec()) || audioStream.getAudioChannels() == 0) {
              continue;
            }
            audioStreams.add(audioStream);
          }
          mfOriginal.setAudioStreams(audioStreams);

          List<MediaFileSubtitle> subtitles = new ArrayList<>();
          for (MediaFileSubtitle subtitle : mfEditor.getSubtitles()) {
            if (StringUtils.isBlank(subtitle.getLanguage())) {
              continue;
            }
            subtitles.add(subtitle);
          }
          mfOriginal.setSubtitles(subtitles);

          break;
        }
      }
    }
  }

  public void unbindBindings() {
    try {
      bindingGroup.unbind();
    }
    catch (Exception ignored) {
      // just not crash
    }
  }

  protected void initDataBindings() {
    JTableBinding jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, mediaFiles, tableMediaFiles);
    //
    Property mediaFileContainerBeanProperty = BeanProperty.create("mediaFile.filename");
    jTableBinding.addColumnBinding(mediaFileContainerBeanProperty).setColumnName("Filename").setEditable(false);
    //
    jTableBinding.setEditable(false);
    jTableBinding.bind();
    //
    Property jTableBeanProperty = BeanProperty.create("selectedElement.mediaFile.filename");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty, lblFilename,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    Property jTableBeanProperty_1 = BeanProperty.create("selectedElement.mediaFile.videoCodec");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_1, tfCodec,
        jTextFieldBeanProperty);
    autoBinding_1.bind();
    //
    Property jTableBeanProperty_3 = BeanProperty.create("selectedElement.mediaFile.containerFormat");
    Property jTextFieldBeanProperty_2 = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_3, tfContainerFormat,
        jTextFieldBeanProperty_2);
    autoBinding_3.bind();
    //
    Property jTableBeanProperty_5 = BeanProperty.create("selectedElement.mediaFile.videoWidth");
    Property jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_5, tfWidth,
        jTextFieldBeanProperty_4);
    autoBinding_5.bind();
    //
    Property jTableBeanProperty_6 = BeanProperty.create("selectedElement.mediaFile.videoHeight");
    Property jTextFieldBeanProperty_5 = BeanProperty.create("text");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_6, tfHeight,
        jTextFieldBeanProperty_5);
    autoBinding_6.bind();
    //
    Property jTableBeanProperty_7 = BeanProperty.create("selectedElement.mediaFile.video3DFormat");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_7, cb3dFormat,
        jComboBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property tmmTableBeanProperty = BeanProperty.create("selectedElement.mediaFile.aspectRatio");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty, cbAspectRatio,
        jComboBoxBeanProperty);
    autoBinding_4.setConverter(new AspectRatioConverter(getAspectRatios()));
    autoBinding_4.bind();
    //
    Property tmmTableBeanProperty_6 = BeanProperty.create("selectedElement.mediaFile.aspectRatio2");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_6, cbAspectRatio2,
        jComboBoxBeanProperty);
    autoBinding_12.setConverter(new AspectRatioConverter(getAspectRatios2()));
    autoBinding_12.bind();
    //
    Property tmmTableBeanProperty_1 = BeanProperty.create("selectedElement.mediaFile.frameRate");
    Property jSpinnerBeanProperty = BeanProperty.create("value");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_1, spFrameRate,
        jSpinnerBeanProperty);
    autoBinding_7.bind();
    //
    Property tmmTableBeanProperty_2 = BeanProperty.create("selectedElement.mediaFile.bitDepth");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_2, tfBitDepth,
        jTextFieldBeanProperty_1);
    autoBinding_8.bind();
    //
    Property tmmTableBeanProperty_3 = BeanProperty.create("selectedElement.mediaFile.overallBitRate");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_3, tfVideoBitrate,
        jTextFieldBeanProperty_3);
    autoBinding_9.bind();
    //
    Property tmmTableBeanProperty_4 = BeanProperty.create("selectedElement.mediaFile.hdrFormat");
    Property jTextFieldBeanProperty_6 = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_4, tfHdrFormat,
        jTextFieldBeanProperty_6);
    autoBinding_10.bind();
    //
    Property tmmTableBeanProperty_5 = BeanProperty.create("selectedElement.mediaFile.durationInMinutes");
    Property jTextFieldBeanProperty_7 = BeanProperty.create("text");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_5, tfRuntime,
        jTextFieldBeanProperty_7);
    autoBinding_11.bind();
    //
    bindingGroup.addBinding(jTableBinding);
  }

  public void cancelTask() {
    if (this.ardTask != null) {
      this.ardTask.cancel();
      this.ardTask = null;
    }
  }

  public static class AspectRatioContainer {
    private final Float  aspectRatio;
    private final String customText;

    private AspectRatioContainer(Float aspectRatio) {
      this(aspectRatio, null);
    }

    private AspectRatioContainer(Float aspectRatio, String customText) {
      this.aspectRatio = aspectRatio;
      this.customText = customText;
    }

    private Float getAspectRatio() {
      return aspectRatio;
    }

    @Override
    public String toString() {
      if (StringUtils.isNotBlank(customText)) {
        if (aspectRatio != null && aspectRatio > 0) {
          return customText + String.format(" (%.2f:1)", aspectRatio);
        }
        else {
          return customText;
        }
      }
      return AspectRatio.getDescription(aspectRatio);
    }
  }

  public static class AspectRatioConverter extends Converter<Float, AspectRatioContainer> {

    private final List<AspectRatioContainer> values;

    public AspectRatioConverter(List<AspectRatioContainer> values) {
      this.values = values;
    }

    @Override
    public AspectRatioContainer convertForward(Float value) {
      return values.stream().filter(entry -> Objects.equals(entry.aspectRatio, value)).findFirst().orElse(null);
    }

    @Override
    public Float convertReverse(AspectRatioContainer value) {
      return value.aspectRatio;
    }
  }
}
