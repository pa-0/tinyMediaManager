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
package org.tinymediamanager.ui.components.combobox;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.ParsePosition;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.impl.GlazedListsImpl;
import ca.odell.glazedlists.impl.filter.SearchTerm;
import ca.odell.glazedlists.impl.filter.TextMatcher;
import ca.odell.glazedlists.impl.swing.ComboBoxPopupLocationFix;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.Matchers;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;

/**
 * A modified version of Glazedlists AutocompleteSupport - to support the autocomplete behavior in check combo boxes too
 *
 * @author James Lemieux, Manuel Laggner
 */
public final class AutocompleteSupport<E> {

  private static final ParsePosition      PARSE_POSITION                  = new ParsePosition(0);
  private static final Class<?>[]         VALUE_OF_SIGNATURE              = { String.class };

  /** Marker object for indicating "not found". */
  private static final Object             NOT_FOUND                       = new Object();

  //
  // These member variables control behaviour of the autocompletion support
  //

  /**
   * <tt>true</tt> if user-specified text is converted into the same case as the autocompletion term. <tt>false</tt> will leave user specified text
   * unaltered.
   */
  private boolean                         correctsCase                    = true;

  /**
   * <tt>false</tt> if the user can specify values that do not appear in the ComboBoxModel; <tt>true</tt> otherwise.
   */
  private boolean                         strict                          = false;

  /**
   * <tt>true</tt> indicates a beep sound should be played to the user to indicate their error when attempting to violate the {@link #strict} setting;
   * <tt>false</tt> indicates we should not beep.
   */
  private boolean                         beepOnStrictViolation           = true;

  /**
   * <tt>true</tt> if the text in the combobox editor is selected when the editor gains focus; <tt>false</tt> otherwise.
   */
  private boolean                         selectsTextOnFocusGain          = true;

  /**
   * <tt>true</tt> if the {@link #popupMenu} should <strong>always</strong> be hidden when the {@link #comboBoxEditor} loses focus; <tt>false</tt> if
   * the default behaviour should be preserved. This exists to provide a reasonable alternative to the strange default behaviour in JComboBox in which
   * the tab key will advance focus to the next focusable component and leave the JPopupMenu visible.
   */
  private boolean                         hidesPopupOnFocusLost           = true;

  //
  // These are member variables for convenience
  //

  /** The comboBox being decorated with autocomplete functionality. */
  private JComboBox<E>                    comboBox;

  /** The popup menu of the decorated comboBox. */
  private JPopupMenu                      popupMenu;

  /** The popup that wraps the popupMenu of the decorated comboBox. */
  private ComboPopup                      popup;

  /** The arrow button that invokes the popup. */
  private JButton                         arrowButton;

  /** The model backing the comboBox. */
  private final AutoCompleteComboBoxModel comboBoxModel;

  /** The custom renderer installed on the comboBox or <code>null</code> if one is not required. */
  private final ListCellRenderer<Object>  renderer;

  /** The EventList which holds the items present in the comboBoxModel. */
  private final EventList<E>              items;

  /** The FilterList which filters the items present in the comboBoxModel. */
  private final FilterList<E>             filteredItems;

  /** A single-element EventList for storing the optional first element, typically used to represent "no selection". */
  private final EventList<E>              firstItem;

  /** The CompositeList which is the union of firstItem and filteredItems to produce all filtered items available in the comboBoxModel. */
  private final CompositeList<E>          allItemsFiltered;

  /** The CompositeList which is the union of firstItem and items to produce all unfiltered items available in the comboBoxModel. */
  private final CompositeList<E>          allItemsUnfiltered;

  /** The Format capable of producing Strings from ComboBoxModel elements and vice versa. */
  private final Format                    format;

  /** The MatcherEditor driving the FilterList behind the comboBoxModel. */
  private final TextMatcherEditor<E>      filterMatcherEditor;

  /**
   * The custom ComboBoxEditor that does NOT assume that the text value can be computed using Object.toString(). (i.e. the default ComboBoxEditor
   * *does* assume that, but we decorate it and remove that assumption)
   */
  private FormatComboBoxEditor            comboBoxEditor;

  /** The textfield which acts as the editor of the comboBox. */
  private JTextField                      comboBoxEditorComponent;

  /** The Document backing the comboBoxEditorComponent. */
  private AbstractDocument                document;

  /** A DocumentFilter that controls edits to the document. */
  private final AutoCompleteFilter        documentFilter                  = new AutoCompleteFilter();

  /** The last prefix specified by the user. */
  private String                          prefix                          = "";

  /** The Matcher that decides if a ComboBoxModel element is filtered out. */
  private Matcher<String>                 filterMatcher                   = Matchers.trueMatcher();

  /** <tt>true</tt> while processing a text change to the {@link #comboBoxEditorComponent}; <tt>false</tt> otherwise. */
  private boolean                         isFiltering                     = false;

  /** Controls the selection behavior of the JComboBox when it is used in a JTable DefaultCellEditor. */
  private final boolean                   isTableCellEditor;

  //
  // These listeners work together to enforce different aspects of the autocompletion behaviour
  //

  /**
   * The MouseListener which is installed on the {@link #arrowButton} and is responsible for clearing the filter and then showing / hiding the
   * {@link #popup}.
   */
  private ArrowButtonMouseListener        arrowButtonMouseListener;

  /**
   * A listener which reacts to changes in the ComboBoxModel by resizing the popup appropriately to accomodate the new data.
   */
  private final ListDataListener          listDataHandler                 = new ListDataHandler();

  /**
   * We ensure the popup menu is sized correctly each time it is shown. Namely, we respect the prototype display value of the combo box, if it has
   * one. Regardless of the width of the combo box, we attempt to size the popup to accomodate the width of the prototype display value.
   */
  private final PopupMenuListener         popupSizerHandler               = new PopupSizer();

  /**
   * An unfortunately necessary fixer for a misplaced popup.
   */
  private ComboBoxPopupLocationFix        popupLocationFix;

  /**
   * We ensure that selecting an item from the popup via the mouse never attempts to autocomplete for fear that we will replace the user's newly
   * selected item and the item will effectively be unselectable.
   */
  private final MouseListener             popupMouseHandler               = new PopupMouseHandler();

  /** Handles the special case of the backspace key in strict mode and the enter key. */
  private final KeyListener               strictModeBackspaceHandler      = new AutoCompleteKeyHandler();

  /** Handles selecting the text in the comboBoxEditorComponent when it gains focus. */
  private final FocusListener             selectTextOnFocusGainHandler    = new ComboBoxEditorFocusHandler();

  //
  // These listeners watch for invalid changes to the JComboBox which break our autocompletion
  //

  /**
   * Watches for changes of the Document which backs comboBoxEditorComponent and uninstalls our DocumentFilter from the old Document and reinstalls it
   * on the new.
   */
  private final DocumentWatcher           documentWatcher                 = new DocumentWatcher();

  /** Watches for changes of the ComboBoxModel and reports them as violations. */
  private final ModelWatcher              modelWatcher                    = new ModelWatcher();

  /** Watches for changes of the ComboBoxUI and reinstalls the autocompletion support. */
  private final UIWatcher                 uiWatcher                       = new UIWatcher();

  //
  // These booleans control when certain changes are to be respected and when they aren't
  //

  /**
   * <tt>true</tt> indicates document changes should not be post processed (i.e. just commit changes to the Document and do not cause any
   * side-effects).
   */
  private boolean                         doNotPostProcessDocumentChanges = false;

  /** <tt>true</tt> indicates attempts to filter the ComboBoxModel should be ignored. */
  private boolean                         doNotFilter                     = false;

  /** <tt>true</tt> indicates attempts to change the document should be ignored. */
  private boolean                         doNotChangeDocument             = false;

  /** <tt>true</tt> indicates attempts to select an autocompletion term should be ignored. */
  private boolean                         doNotAutoComplete               = false;

  /**
   * <tt>true</tt> indicates attempts to toggle the state of the popup should be ignored. In general, the only time we should toggle the state of a
   * popup is due to a users keystroke (and not programmatically setting the selected item, for example).
   *
   * When the JComboBox is used within a TableCellEditor, this value is ALWAYS false, since we MUST accept keystrokes, even when they are passed
   * second hand to the JComboBox after it has been installed as the cell editor (as opposed to typed into the JComboBox directly)
   */
  private boolean                         doNotTogglePopup;

  /**
   * <tt>true</tt> indicates attempts to clear the filter when hiding the popup should be ignored. This is because sometimes we hide and reshow a
   * popup in rapid succession and we want to avoid the work to unfiltering/refiltering it.
   */
  private boolean                         doNotClearFilterOnPopupHide     = false;

  //
  // Values present before {@link #install} executes - and are restored when {@link @uninstall} executes
  //

  /** The original setting of the editable field on the comboBox. */
  private final boolean                   originalComboBoxEditable;

  /** The original model installed on the comboBox. */
  private ComboBoxModel<E>                originalModel;

  /** The original ListCellRenderer installed on the comboBox. */
  private ListCellRenderer<? super E>     originalRenderer;

  //
  // Values present before {@link #decorateCurrentUI} executes - and are restored when {@link @undecorateOriginalUI} executes
  //

  /** The original Actions associated with the up and down arrow keys. */
  private Action                          originalSelectNextAction;
  private Action                          originalSelectPreviousAction;
  private Action                          originalSelectNext2Action;
  private Action                          originalSelectPrevious2Action;
  private Action                          originalAquaSelectNextAction;
  private Action                          originalAquaSelectPreviousAction;

  /**
   * This private constructor creates an AutocompleteSupport object which adds autocompletion functionality to the given <code>comboBox</code>. In
   * particular, a custom {@link ComboBoxModel} is installed behind the <code>comboBox</code> containing the given <code>items</code>. The
   * <code>filterator</code> is consulted in order to extract searchable text from each of the <code>items</code>. Non-null <code>format</code>
   * objects are used to convert ComboBoxModel elements to Strings and back again for various functions like filtering, editing, and rendering.
   *
   * @param comboBox
   *          the {@link JComboBox} to decorate with autocompletion
   * @param items
   *          the objects to display in the <code>comboBox</code>
   * @param filterator
   *          extracts searchable text strings from each item
   * @param format
   *          converts combobox elements into strings and vice versa
   */
  private AutocompleteSupport(JComboBox<E> comboBox, EventList<E> items, TextFilterator<? super E> filterator, Format format) {
    this.comboBox = comboBox;
    this.originalComboBoxEditable = comboBox.isEditable();
    this.originalModel = comboBox.getModel();
    this.items = items;
    this.format = format;

    // only build a custom renderer if the user specified their own Format but has not installed a custom renderer of their own
    final boolean defaultRendererInstalled = comboBox.getRenderer() instanceof UIResource;
    this.renderer = format != null && defaultRendererInstalled ? new StringFunctionRenderer() : null;

    // is this combo box a TableCellEditor?
    this.isTableCellEditor = Boolean.TRUE.equals(comboBox.getClientProperty("JComboBox.isTableCellEditor"));
    this.doNotTogglePopup = !isTableCellEditor;

    // lock the items list for reading since we want to prevent writes
    // from occurring until we fully initialize this AutocompleteSupport
    items.getReadWriteLock().readLock().lock();
    try {
      // build the ComboBoxModel capable of filtering its values
      this.filterMatcherEditor = new TextMatcherEditor<>(filterator == null ? new DefaultTextFilterator() : filterator);
      this.filterMatcherEditor.setMode(TextMatcherEditor.STARTS_WITH);
      this.filteredItems = new FilterList<>(items, this.filterMatcherEditor);
      this.firstItem = new BasicEventList<>(items.getPublisher(), items.getReadWriteLock());

      // the ComboBoxModel always contains the firstItem and a filtered view of all other items
      this.allItemsFiltered = new CompositeList<>(items.getPublisher(), items.getReadWriteLock());
      this.allItemsFiltered.addMemberList(this.firstItem);
      this.allItemsFiltered.addMemberList(this.filteredItems);
      this.comboBoxModel = new AutoCompleteComboBoxModel(this.allItemsFiltered);

      // we need an unfiltered view in order to try to locate autocompletion terms
      this.allItemsUnfiltered = new CompositeList<>(items.getPublisher(), items.getReadWriteLock());
      this.allItemsUnfiltered.addMemberList(this.firstItem);
      this.allItemsUnfiltered.addMemberList(this.items);
    }
    finally {
      items.getReadWriteLock().readLock().unlock();
    }

    // customize the comboBox
    this.comboBox.setModel(this.comboBoxModel);
    this.comboBox.setEditable(true);
    decorateCurrentUI();

    // react to changes made to the key parts of JComboBox which affect autocompletion
    this.comboBox.addPropertyChangeListener("UI", this.uiWatcher);
    this.comboBox.addPropertyChangeListener("model", this.modelWatcher);
    this.comboBoxEditorComponent.addPropertyChangeListener("document", this.documentWatcher);
  }

  /**
   * A convenience method to unregister and return all {@link ActionListener}s currently installed on the given <code>comboBox</code>. This is the
   * only technique we can rely on to prevent the <code>comboBox</code> from broadcasting {@link ActionEvent}s at inappropriate times.
   *
   * This method is the logical inverse of {@link #registerAllActionListeners}.
   */
  private static ActionListener[] unregisterAllActionListeners(JComboBox<?> comboBox) {
    final ActionListener[] listeners = comboBox.getActionListeners();
    for (int i = 0; i < listeners.length; i++) {
      comboBox.removeActionListener(listeners[i]);
    }

    return listeners;
  }

  /**
   * A convenience method to register all of the given <code>listeners</code> with the given <code>comboBox</code>.
   *
   * This method is the logical inverse of {@link #unregisterAllActionListeners}.
   */
  private static void registerAllActionListeners(JComboBox<?> comboBox, ActionListener[] listeners) {
    for (int i = 0; i < listeners.length; i++) {
      comboBox.addActionListener(listeners[i]);
    }
  }

  /**
   * A convenience method to search through the given JComboBox for the JButton which toggles the popup up open and closed.
   */
  private static JButton findArrowButton(JComboBox<?> c) {
    for (int i = 0, n = c.getComponentCount(); i < n; i++) {
      final Component comp = c.getComponent(i);
      if (comp instanceof JButton) {
        return (JButton) comp;
      }
    }

    return null;
  }

  /**
   * Decorate all necessary areas of the current UI to install autocompletion support. This method is called in the constructor and when the
   * comboBox's UI delegate is changed.
   */
  private void decorateCurrentUI() {
    // record some original settings of comboBox
    this.originalRenderer = comboBox.getRenderer();
    this.popupMenu = (JPopupMenu) comboBox.getUI().getAccessibleChild(comboBox, 0);
    this.popup = (ComboPopup) popupMenu;
    this.arrowButton = findArrowButton(comboBox);

    // if an arrow button was found, decorate the ComboPopup's MouseListener
    // with logic that unfilters the ComboBoxModel when the arrow button is pressed
    if (this.arrowButton != null) {
      this.arrowButton.removeMouseListener(popup.getMouseListener());
      this.arrowButtonMouseListener = new ArrowButtonMouseListener(popup.getMouseListener());
      this.arrowButton.addMouseListener(arrowButtonMouseListener);
    }

    // start listening for model changes (due to filtering) so we can resize the popup vertically
    this.comboBox.getModel().addListDataListener(listDataHandler);

    // calculate the popup's width according to the prototype value, if one exists
    this.popupMenu.addPopupMenuListener(popupSizerHandler);

    // fix the popup's location
    this.popupLocationFix = ComboBoxPopupLocationFix.install(this.comboBox);

    // start suppressing autocompletion when selecting values from the popup with the mouse
    this.popup.getList().addMouseListener(popupMouseHandler);

    // record the original Up/Down arrow key Actions
    final ActionMap actionMap = comboBox.getActionMap();
    this.originalSelectNextAction = actionMap.get("selectNext");
    this.originalSelectPreviousAction = actionMap.get("selectPrevious");
    this.originalSelectNext2Action = actionMap.get("selectNext2");
    this.originalSelectPrevious2Action = actionMap.get("selectPrevious2");
    this.originalAquaSelectNextAction = actionMap.get("aquaSelectNext");
    this.originalAquaSelectPreviousAction = actionMap.get("aquaSelectPrevious");

    final Action upAction = new MoveAction(-1);
    final Action downAction = new MoveAction(1);

    // install custom actions for the arrow keys in all non-Apple L&Fs
    actionMap.put("selectPrevious", upAction);
    actionMap.put("selectNext", downAction);
    actionMap.put("selectPrevious2", upAction);
    actionMap.put("selectNext2", downAction);

    // install custom actions for the arrow keys in the Apple Aqua L&F
    actionMap.put("aquaSelectPrevious", upAction);
    actionMap.put("aquaSelectNext", downAction);

    // install a custom ComboBoxEditor that decorates the existing one, but uses the
    // convertToString(...) method to produce text for the editor component (rather than .toString())
    this.comboBoxEditor = new FormatComboBoxEditor(comboBox.getEditor());
    this.comboBox.setEditor(comboBoxEditor);

    // add a DocumentFilter to the Document backing the editor JTextField
    this.comboBoxEditorComponent = (JTextField) comboBox.getEditor().getEditorComponent();
    this.document = (AbstractDocument) comboBoxEditorComponent.getDocument();
    this.document.setDocumentFilter(documentFilter);

    // install a custom renderer on the combobox, if we have built one
    if (this.renderer != null) {
      comboBox.setRenderer(renderer);
    }

    // add a KeyListener to the ComboBoxEditor which handles the special case of backspace when in strict mode
    this.comboBoxEditorComponent.addKeyListener(strictModeBackspaceHandler);
    // add a FocusListener to the ComboBoxEditor which selects all text when focus is gained
    this.comboBoxEditorComponent.addFocusListener(selectTextOnFocusGainHandler);
  }

  /**
   * Remove all customizations installed to various areas of the current UI in order to uninstall autocompletion support. This method is invoked after
   * the comboBox's UI delegate is changed.
   */
  private void undecorateOriginalUI() {
    // if an arrow button was found, remove our custom MouseListener and
    // reinstall the normal popup MouseListener
    if (this.arrowButton != null) {
      this.arrowButton.removeMouseListener(arrowButtonMouseListener);
      this.arrowButton.addMouseListener(arrowButtonMouseListener.getDecorated());
    }

    // stop listening for model changes
    this.comboBox.getModel().removeListDataListener(listDataHandler);

    // remove the DocumentFilter from the Document backing the editor JTextField
    this.document.setDocumentFilter(null);

    // restore the original ComboBoxEditor if our custom ComboBoxEditor is still installed
    if (this.comboBox.getEditor() == comboBoxEditor) {
      this.comboBox.setEditor(comboBoxEditor.getDelegate());
    }

    // stop adjusting the popup's width according to the prototype value
    this.popupMenu.removePopupMenuListener(popupSizerHandler);

    // stop fixing the combobox's popup location
    this.popupLocationFix.uninstall();

    // stop suppressing autocompletion when selecting values from the popup with the mouse
    this.popup.getList().removeMouseListener(popupMouseHandler);

    final ActionMap actionMap = comboBox.getActionMap();
    // restore the original actions for the arrow keys in all non-Apple L&Fs
    actionMap.put("selectPrevious", originalSelectPreviousAction);
    actionMap.put("selectNext", originalSelectNextAction);
    actionMap.put("selectPrevious2", originalSelectPrevious2Action);
    actionMap.put("selectNext2", originalSelectNext2Action);

    // restore the original actions for the arrow keys in the Apple Aqua L&F
    actionMap.put("aquaSelectPrevious", originalAquaSelectPreviousAction);
    actionMap.put("aquaSelectNext", originalAquaSelectNextAction);

    // remove the KeyListener from the ComboBoxEditor which handles the special case of backspace when in strict mode
    this.comboBoxEditorComponent.removeKeyListener(strictModeBackspaceHandler);
    // remove the FocusListener from the ComboBoxEditor which selects all text when focus is gained
    this.comboBoxEditorComponent.removeFocusListener(selectTextOnFocusGainHandler);

    // remove the custom renderer if it is still installed
    if (this.comboBox.getRenderer() == renderer) {
      this.comboBox.setRenderer(originalRenderer);
    }

    // erase some original settings of comboBox
    this.originalRenderer = null;
    this.comboBoxEditor = null;
    this.comboBoxEditorComponent = null;
    this.document = null;
    this.popupMenu = null;
    this.popup = null;
    this.arrowButton = null;
  }

  /**
   * Installs support for autocompletion into the <code>comboBox</code> and returns the support object that is actually providing those facilities.
   * The support object is returned so that the caller may invoke {@link #uninstall} at some later time to remove the autocompletion features.
   *
   * <p>
   * This method assumes that the <code>items</code> can be converted into reasonable String representations via {@link Object#toString()}.
   *
   * <p>
   * The following must be true in order to successfully install support for autocompletion on a {@link JComboBox}:
   *
   * <ul>
   * <li>The JComboBox must use a {@link JTextField} as its editor component
   * <li>The JTextField must use an {@link AbstractDocument} as its model
   * </ul>
   *
   * @param comboBox
   *          the {@link JComboBox} to decorate with autocompletion
   * @param items
   *          the objects to display in the <code>comboBox</code>
   * @return an instance of the support class providing autocomplete features
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public static <E> AutocompleteSupport<E> install(JComboBox<E> comboBox, EventList<E> items) {
    return install(comboBox, items, null);
  }

  /**
   * Installs support for autocompletion into the <code>comboBox</code> and returns the support object that is actually providing those facilities.
   * The support object is returned so that the caller may invoke {@link #uninstall} at some later time to remove the autocompletion features.
   *
   * <p>
   * This method assumes that the <code>items</code> can be converted into reasonable String representations via {@link Object#toString()}.
   *
   * <p>
   * The <code>filterator</code> will be used to extract searchable text strings from each of the <code>items</code>. A <code>null</code> filterator
   * implies the item's toString() method should be used when filtering it.
   *
   * <p>
   * The following must be true in order to successfully install support for autocompletion on a {@link JComboBox}:
   *
   * <ul>
   * <li>The JComboBox must use a {@link JTextField} as its editor component
   * <li>The JTextField must use an {@link AbstractDocument} as its model
   * </ul>
   *
   * @param comboBox
   *          the {@link JComboBox} to decorate with autocompletion
   * @param items
   *          the objects to display in the <code>comboBox</code>
   * @param filterator
   *          extracts searchable text strings from each item; <code>null</code> implies the item's toString() method should be used when filtering it
   * @return an instance of the support class providing autocomplete features
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public static <E> AutocompleteSupport<E> install(JComboBox<E> comboBox, EventList<E> items, TextFilterator<? super E> filterator) {
    return install(comboBox, items, filterator, null);
  }

  /**
   * Installs support for autocompletion into the <code>comboBox</code> and returns the support object that is actually providing those facilities.
   * The support object is returned so that the caller may invoke {@link #uninstall} at some later time to remove the autocompletion features.
   *
   * <p>
   * This method uses the given <code>format</code> to convert the given <code>items</code> into Strings and back again. In other words, this method
   * does <strong>NOT</strong> rely on {@link Object#toString()} to produce a reasonable String representation of each item. Likewise, it does not
   * rely on the existence of a valueOf(String) method for creating items out of Strings as is the default behaviour of JComboBox.
   *
   * <p>
   * It can be assumed that the only methods called on the given <code>format</code> are:
   * <ul>
   * <li>{@link Format#format(Object)}
   * <li>{@link Format#parseObject(String, ParsePosition)}
   * </ul>
   *
   * <p>
   * As a convenience, this method will install a custom {@link ListCellRenderer} on the <code>comboBox</code> that displays the String value returned
   * by the <code>format</code>. Though this is only done if the given <code>format</code> is not <code>null</code> and if the <code>comboBox</code>
   * does not already use a custom renderer.
   *
   * <p>
   * The <code>filterator</code> will be used to extract searchable text strings from each of the <code>items</code>. A <code>null</code> filterator
   * implies one of two default strategies will be used. If the <code>format</code> is not null then the String value returned from the
   * <code>format</code> object will be used when filtering a given item. Otherwise, the item's toString() method will be used when it is filtered.
   *
   * <p>
   * The following must be true in order to successfully install support for autocompletion on a {@link JComboBox}:
   *
   * <ul>
   * <li>The JComboBox must use a {@link JTextField} as its editor component
   * <li>The JTextField must use an {@link AbstractDocument} as its model
   * </ul>
   *
   * @param comboBox
   *          the {@link JComboBox} to decorate with autocompletion
   * @param items
   *          the objects to display in the <code>comboBox</code>
   * @param filterator
   *          extracts searchable text strings from each item. If the <code>format</code> is not null then the String value returned from the
   *          <code>format</code> object will be used when filtering a given item. Otherwise, the item's toString() method will be used when it is
   *          filtered.
   * @param format
   *          a Format object capable of converting <code>items</code> into Strings and back. <code>null</code> indicates the standard JComboBox
   *          methods of converting are acceptable.
   * @return an instance of the support class providing autocomplete features
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public static <E> AutocompleteSupport<E> install(JComboBox<E> comboBox, EventList<E> items, TextFilterator<? super E> filterator, Format format) {
    return new AutocompleteSupport<>(comboBox, items, filterator, format);
  }

  /**
   * This method is used to report environmental invariants which are violated when the user adjusts the combo box in a way that is incompatible with
   * the requirements for autocompletion. A message can be specified which will be included in the {@link IllegalStateException} that is throw out of
   * this method after the autocompletion support is uninstalled.
   *
   * @param message
   *          a message to the programmer explaining the environmental invariant that was violated
   */
  private void throwIllegalStateException(String message) {
    final String exceptionMsg = message + "\n" + "In order for AutocompleteSupport to continue to "
        + "work, the following invariants must be maintained after " + "AutocompleteSupport.install() has been called:\n"
        + "* the ComboBoxModel may not be removed\n"
        + "* the AbstractDocument behind the JTextField can be changed but must be changed to some subclass of AbstractDocument\n"
        + "* the DocumentFilter on the AbstractDocument behind the JTextField may not be removed\n";

    uninstall();

    throw new IllegalStateException(exceptionMsg);
  }

  /**
   * A convenience method to produce a String from the given <code>comboBoxElement</code>.
   */
  private String convertToString(Object comboBoxElement) {
    if (comboBoxElement == NOT_FOUND) {
      return "NOT_FOUND";
    }

    if (format != null) {
      return format.format(comboBoxElement);
    }

    return comboBoxElement == null ? "" : comboBoxElement.toString();
  }

  /**
   * Returns the autocompleting {@link JComboBox} or <code>null</code> if {@link AutocompleteSupport} has been {@link #uninstall}ed.
   */
  public JComboBox<E> getComboBox() {
    return this.comboBox;
  }

  /**
   * Returns the {@link TextFilterator} that extracts searchable strings from each item in the {@link ComboBoxModel}.
   */
  public TextFilterator<? super E> getTextFilterator() {
    return this.filterMatcherEditor.getFilterator();
  }

  /**
   * Returns the filtered {@link EventList} of items which backs the {@link ComboBoxModel} of the autocompleting {@link JComboBox}.
   */
  public EventList<E> getItemList() {
    return this.filteredItems;
  }

  /**
   * Returns <tt>true</tt> if user specified strings are converted to the case of the autocompletion term they match; <tt>false</tt> otherwise.
   */
  public boolean getCorrectsCase() {
    return correctsCase;
  }

  /**
   * If <code>correctCase</code> is <tt>true</tt>, user specified strings will be converted to the case of the element they match. Otherwise they will
   * be left unaltered.
   *
   * <p>
   * Note: this flag only has meeting when strict mode is turned off. When strict mode is on, case is corrected regardless of this setting.
   *
   * @see #setStrict(boolean)
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public void setCorrectsCase(boolean correctCase) {
    this.correctsCase = correctCase;
  }

  /**
   * Returns <tt>true</tt> if the user is able to specify values which do not appear in the popup list of suggestions; <tt>false</tt> otherwise.
   */
  public boolean isStrict() {
    return strict;
  }

  /**
   * If <code>strict</code> is <tt>false</tt>, the user can specify values not appearing within the ComboBoxModel. If it is <tt>true</tt> each
   * keystroke must continue to match some value in the ComboBoxModel or it will be discarded.
   *
   * <p>
   * Note: When strict mode is enabled, all user input is corrected to the case of the autocompletion term, regardless of the correctsCase setting.
   *
   * @see #setCorrectsCase(boolean)
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public void setStrict(boolean strict) {

    if (this.strict == strict) {
      return;
    }

    this.strict = strict;

    // if strict mode was just turned on, ensure the comboBox contains a
    // value from the ComboBoxModel (i.e. start being strict!)
    if (strict) {
      final String currentText = comboBoxEditorComponent.getText();
      Object currentItem = findAutoCompleteTerm(currentText);
      String currentItemText = convertToString(currentItem);
      boolean itemMatches = currentItem == comboBox.getSelectedItem();
      boolean textMatches = GlazedListsImpl.equal(currentItemText, currentText);

      // select the first element if no autocompletion term could be found
      if (currentItem == NOT_FOUND && !allItemsUnfiltered.isEmpty()) {
        currentItem = allItemsUnfiltered.get(0);
        currentItemText = convertToString(currentItem);
        itemMatches = currentItem == comboBox.getSelectedItem();
        textMatches = GlazedListsImpl.equal(currentItemText, currentText);
      }

      // return all elements to the ComboBoxModel
      applyFilter("");

      doNotPostProcessDocumentChanges = true;
      try {
        // adjust the editor's text, if necessary
        if (!textMatches) {
          comboBoxEditorComponent.setText(currentItemText);
        }

        // adjust the model's selected item, if necessary
        if (!itemMatches || comboBox.getSelectedIndex() == -1) {
          comboBox.setSelectedItem(currentItem);
        }
      }
      finally {
        doNotPostProcessDocumentChanges = false;
      }
    }
  }

  /**
   * Returns <tt>true</tt> if a beep sound is played when the user attempts to violate the strict invariant; <tt>false</tt> if no beep sound is
   * played. This setting is only respected if {@link #isStrict()} returns <tt>true</tt>.
   *
   * @see #setStrict(boolean)
   */
  public boolean getBeepOnStrictViolation() {
    return beepOnStrictViolation;
  }

  /**
   * Sets the policy for indicating strict-mode violations to the user by way of a beep sound.
   *
   * @param beepOnStrictViolation
   *          <tt>true</tt> if a beep sound should be played when the user attempts to violate the strict invariant; <tt>false</tt> if no beep sound
   *          should be played
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public void setBeepOnStrictViolation(boolean beepOnStrictViolation) {
    this.beepOnStrictViolation = beepOnStrictViolation;
  }

  /**
   * Returns <tt>true</tt> if the combo box editor text is selected when it gains focus; <tt>false</tt> otherwise.
   */
  public boolean getSelectsTextOnFocusGain() {
    return selectsTextOnFocusGain;
  }

  /**
   * If <code>selectsTextOnFocusGain</code> is <tt>true</tt>, all text in the editor is selected when the combo box editor gains focus. If it is
   * <tt>false</tt> the selection state of the editor is not effected by focus changes.
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public void setSelectsTextOnFocusGain(boolean selectsTextOnFocusGain) {
    this.selectsTextOnFocusGain = selectsTextOnFocusGain;
  }

  /**
   * Returns <tt>true</tt> if the popup menu is hidden whenever the combo box editor loses focus; <tt>false</tt> otherwise.
   */
  public boolean getHidesPopupOnFocusLost() {
    return hidesPopupOnFocusLost;
  }

  /**
   * If <code>hidesPopupOnFocusLost</code> is <tt>true</tt>, then the popup menu of the combo box is <strong>always</strong> hidden whenever the combo
   * box editor loses focus. If it is <tt>false</tt> the default behaviour is preserved. In practice this means that if focus is lost because of a
   * MouseEvent, the behaviour is reasonable, but if focus is lost because of a KeyEvent (e.g. tabbing to the next focusable component) then the popup
   * menu remains visible.
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public void setHidesPopupOnFocusLost(boolean hidesPopupOnFocusLost) {
    this.hidesPopupOnFocusLost = hidesPopupOnFocusLost;
  }

  /**
   * Returns the manner in which the contents of the {@link ComboBoxModel} are filtered. This method will return one of
   * {@link TextMatcherEditor#CONTAINS} or {@link TextMatcherEditor#STARTS_WITH}.
   *
   * <p>
   * {@link TextMatcherEditor#CONTAINS} indicates elements of the {@link ComboBoxModel} are matched when they contain the text entered by the user.
   *
   * <p>
   * {@link TextMatcherEditor#STARTS_WITH} indicates elements of the {@link ComboBoxModel} are matched when they start with the text entered by the
   * user.
   *
   * <p>
   * In both modes, autocompletion only occurs when a given item starts with user-specified text. The filter mode only affects the filtering aspect of
   * autocomplete support.
   */
  public int getFilterMode() {
    return filterMatcherEditor.getMode();
  }

  /**
   * Sets the manner in which the contents of the {@link ComboBoxModel} are filtered. The given <code>mode</code> must be one of
   * {@link TextMatcherEditor#CONTAINS} or {@link TextMatcherEditor#STARTS_WITH}.
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   *
   * @see #getFilterMode()
   */
  public void setFilterMode(int mode) {

    // adjust the MatcherEditor that filters the AutoCompleteComboBoxModel to respect the given mode
    // but ONLY adjust the contents of the model, avoid changing the text in the JComboBox's textfield
    doNotChangeDocument = true;
    try {
      filterMatcherEditor.setMode(mode);
    }
    finally {
      doNotChangeDocument = false;
    }
  }

  /**
   * Sets the manner in which the contents of the {@link ComboBoxModel} are filtered and autocompletion terms are matched. The given
   * <code>strategy</code> must be one of {@link TextMatcherEditor#IDENTICAL_STRATEGY} or {@link TextMatcherEditor#NORMALIZED_STRATEGY} or the Unicode
   * strategy of the ICU4J extension.
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   *
   * @see #getTextMatchingStrategy()
   */
  public void setTextMatchingStrategy(Object strategy) {

    // adjust the MatcherEditor that filters the AutoCompleteComboBoxModel to respect the given strategy
    // but ONLY adjust the contents of the model, avoid changing the text in the JComboBox's textfield
    doNotChangeDocument = true;
    try {
      filterMatcherEditor.setStrategy(strategy);
      // do we need to update the filterMatcher here?
    }
    finally {
      doNotChangeDocument = false;
    }
  }

  /**
   * Returns the manner in which the contents of the {@link ComboBoxModel} are filtered and autocompletion terms are matched. The returned
   * <code>strategy</code> is one of {@link TextMatcherEditor#IDENTICAL_STRATEGY} or {@link TextMatcherEditor#NORMALIZED_STRATEGY} or the Unicode
   * strategy of the ICU4J extension.
   */
  public Object getTextMatchingStrategy() {
    return filterMatcherEditor.getStrategy();
  }

  /**
   * This method set a single optional value to be used as the first element in the {@link ComboBoxModel}. This value typically represents "no
   * selection" or "blank". This value is always present and is not filtered away during autocompletion.
   *
   * @param item
   *          the first value to present in the {@link ComboBoxModel}
   */
  public void setFirstItem(E item) {

    doNotChangeDocument = true;
    firstItem.getReadWriteLock().writeLock().lock();
    try {
      if (firstItem.isEmpty()) {
        firstItem.add(item);
      }
      else {
        firstItem.set(0, item);
      }
    }
    finally {
      firstItem.getReadWriteLock().writeLock().unlock();
      doNotChangeDocument = false;
    }
  }

  /**
   * Returns the optional single value used as the first element in the {@link ComboBoxModel} or <tt>null</tt> if no first item has been set.
   *
   * @return the special first value presented in the {@link ComboBoxModel} or <tt>null</tt> if no first item has been set
   */
  public E getFirstItem() {
    firstItem.getReadWriteLock().readLock().lock();
    try {
      return firstItem.isEmpty() ? null : firstItem.get(0);
    }
    finally {
      firstItem.getReadWriteLock().readLock().unlock();
    }
  }

  /**
   * Removes and returns the optional single value used as the first element in the {@link ComboBoxModel} or <tt>null</tt> if no first item has been
   * set.
   *
   * @return the special first value presented in the {@link ComboBoxModel} or <tt>null</tt> if no first item has been set
   */
  public E removeFirstItem() {
    doNotChangeDocument = true;
    firstItem.getReadWriteLock().writeLock().lock();
    try {
      return firstItem.isEmpty() ? null : firstItem.remove(0);
    }
    finally {
      firstItem.getReadWriteLock().writeLock().unlock();
      doNotChangeDocument = false;
    }
  }

  /**
   * Returns <tt>true</tt> if this autocomplete support instance is currently installed and altering the behaviour of the combo box; <tt>false</tt> if
   * it has been {@link #uninstall}ed.
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public boolean isInstalled() {
    return comboBox != null;
  }

  /**
   * This method removes autocompletion support from the {@link JComboBox} it was installed on. This method is useful when the {@link EventList} of
   * items that backs the combo box must outlive the combo box itself. Calling this method will return the combo box to its original state before
   * autocompletion was installed, and it will be available for garbage collection independently of the {@link EventList} of items.
   *
   * @throws IllegalStateException
   *           if this method is called from any Thread other than the Swing Event Dispatch Thread
   */
  public void uninstall() {

    if (this.comboBox == null) {
      throw new IllegalStateException("This AutocompleteSupport has already been uninstalled");
    }

    items.getReadWriteLock().readLock().lock();
    try {
      // 1. stop listening for changes
      this.comboBox.removePropertyChangeListener("UI", this.uiWatcher);
      this.comboBox.removePropertyChangeListener("model", this.modelWatcher);
      this.comboBoxEditorComponent.removePropertyChangeListener("document", this.documentWatcher);

      // 2. undecorate the original UI components
      this.undecorateOriginalUI();

      // 3. restore the original model to the JComboBox
      this.comboBox.setModel(originalModel);
      this.originalModel = null;

      // 4. restore the original editable flag to the JComboBox
      this.comboBox.setEditable(originalComboBoxEditable);

      // 5. dispose of our ComboBoxModel
      this.comboBoxModel.dispose();

      // 6. dispose of our EventLists so that they are severed from the given items EventList
      this.allItemsFiltered.dispose();
      this.allItemsUnfiltered.dispose();
      this.filteredItems.dispose();

      // null out the comboBox to indicate that this support class is uninstalled
      this.comboBox = null;
    }
    finally {
      items.getReadWriteLock().readLock().unlock();
    }
  }

  /**
   * This method updates the value which filters the items in the ComboBoxModel.
   *
   * @param newFilter
   *          the new value by which to filter the item
   */
  private void applyFilter(String newFilter) {
    // break out early if we're flagged to ignore filter updates for the time being
    if (doNotFilter) {
      return;
    }

    // ignore attempts to change the text in the combo box editor while
    // the filtering is taking place
    doNotChangeDocument = true;
    final ActionListener[] listeners = unregisterAllActionListeners(comboBox);
    isFiltering = true;
    try {
      filterMatcherEditor.setFilterText(new String[] { newFilter });
    }
    finally {
      isFiltering = false;
      registerAllActionListeners(comboBox, listeners);
      doNotChangeDocument = false;
    }
  }

  /**
   * This method updates the {@link #prefix} to be the current value in the ComboBoxEditor.
   */
  private void updateFilter() {
    prefix = comboBoxEditorComponent.getText();

    if (prefix.length() == 0) {
      filterMatcher = Matchers.trueMatcher();
    }
    else {
      filterMatcher = new TextMatcher<>(new SearchTerm[] { new SearchTerm<>(prefix) }, GlazedLists.toStringTextFilterator(),
          TextMatcherEditor.STARTS_WITH, getTextMatchingStrategy());
    }
  }

  /**
   * A small convenience method to try showing the ComboBoxPopup.
   */
  private void togglePopup() {
    // break out early if we're flagged to ignore attempts to toggle the popup state
    if (doNotTogglePopup) {
      return;
    }

    if (comboBoxModel.getSize() == 0) {
      comboBox.hidePopup();
    }
    else if (comboBox.isShowing() && !comboBox.isPopupVisible() && comboBoxEditorComponent.hasFocus()) {
      comboBox.showPopup();
    }
  }

  /**
   * Performs a linear scan of ALL ITEMS, regardless of the filtering state of the ComboBoxModel, to locate the autocomplete term. If an exact match
   * of the given <code>value</code> can be found, then the item is returned. If an exact match cannot be found, the first term that <strong>starts
   * with</strong> the given <code>value</code> is returned.
   *
   * <p>
   * If no exact or partial match can be located, <code>null</code> is returned.
   */
  private Object findAutoCompleteTerm(String value) {
    // determine if our value is empty
    final boolean prefixIsEmpty = "".equals(value);

    final Matcher<String> valueMatcher = new TextMatcher<>(new SearchTerm[] { new SearchTerm<>(value) }, GlazedLists.toStringTextFilterator(),
        TextMatcherEditor.STARTS_WITH, getTextMatchingStrategy());

    Object partialMatchItem = NOT_FOUND;

    // search the list of ALL UNFILTERED items for an autocompletion term for the given value
    for (int i = 0, n = allItemsUnfiltered.size(); i < n; i++) {
      final E item = allItemsUnfiltered.get(i);
      final String itemString = convertToString(item);

      // if we have an exact match, return the given value immediately
      if (value.equals(itemString)) {
        return item;
      }

      // if we have not yet located a partial match, check the current itemString for a partial match
      // (to be returned if an exact match cannot be found)
      if (partialMatchItem == NOT_FOUND) {
        if (prefixIsEmpty ? "".equals(itemString) : valueMatcher.matches(itemString))
          partialMatchItem = item;
      }
    }

    return partialMatchItem;
  }

  /**
   * This special version of EventComboBoxModel simply marks a flag to indicate the items in the ComboBoxModel should not be filtered as a side-effect
   * of setting the selected item. It also marks another flag to indicate that the selected item is being explicitly set, and thus autocompletion
   * should not execute and possibly overwrite the programmer's specified value.
   */
  private class AutoCompleteComboBoxModel extends DefaultEventComboBoxModel<E> {
    public AutoCompleteComboBoxModel(EventList<E> source) {
      super(source);
    }

    /**
     * Overridden because AutocompleteSupport needs absolute control over when a JComboBox's ActionListeners are notified.
     */
    @Override
    public void setSelectedItem(Object selected) {
      doNotFilter = true;
      doNotAutoComplete = true;
      // remove all ActionListeners from the JComboBox since setting the selected item
      // would normally notify them, but in normal autocompletion behaviour, we don't want that
      final ActionListener[] listeners = unregisterAllActionListeners(comboBox);
      try {
        super.setSelectedItem(selected);

        if (comboBoxEditorComponent != null) {
          // remove any text selection that might exist when an item is selected
          final int caretPos = comboBoxEditorComponent.getCaretPosition();
          comboBoxEditorComponent.select(caretPos, caretPos);
        }
      }
      finally {
        // reinstall the ActionListeners we removed
        registerAllActionListeners(comboBox, listeners);
        doNotFilter = false;
        doNotAutoComplete = false;
      }
    }

    /**
     * Overridden because ListEvents produce ListDataEvents from this ComboBoxModel, which notify the BasicComboBoxUI of the data change, which in
     * turn tries to set the text of the ComboBoxEditor to match the text of the selected item. We don't want that. AutocompleteSupport is the
     * ultimate authority on the text value in the ComboBoxEditor. We override this method to set doNotChangeDocument to ensure that attempts to
     * change the ComboBoxEditor's Document are ignored and our control is absolute.
     */
    @Override
    public void listChanged(ListEvent<E> listChanges) {
      doNotChangeDocument = true;
      try {
        super.listChanged(listChanges);
      }
      finally {
        doNotChangeDocument = false;
      }
    }
  }

  /**
   * This class is the crux of the entire solution. This custom DocumentFilter controls all edits which are attempted against the Document of the
   * ComboBoxEditor component. It is our hook to either control when to respect edits as well as the side-effects the edit has on autocompletion and
   * filtering.
   */
  private class AutoCompleteFilter extends DocumentFilter {
    @Override
    public void replace(FilterBypass filterBypass, int offset, int length, String string, AttributeSet attributeSet) throws BadLocationException {
      if (doNotChangeDocument)
        return;

      // collect rollback information before performing the replace
      final String valueBeforeEdit = comboBoxEditorComponent.getText();
      final int selectionStart = comboBoxEditorComponent.getSelectionStart();
      final int selectionEnd = comboBoxEditorComponent.getSelectionEnd();

      // this short-circuit corrects the PlasticLookAndFeel behaviour. Hitting the enter key in Plastic
      // will cause the popup to reopen because the Plastic ComboBoxEditor forwards on unnecessary updates
      // to the document, including ones where the text isn't really changing
      final boolean isReplacingAllText = offset == 0 && document.getLength() == length;
      if (isReplacingAllText && valueBeforeEdit.equals(string)) {
        return;
      }

      super.replace(filterBypass, offset, length, string, attributeSet);
      postProcessDocumentChange(filterBypass, attributeSet, valueBeforeEdit, selectionStart, selectionEnd, true);
    }

    @Override
    public void insertString(FilterBypass filterBypass, int offset, String string, AttributeSet attributeSet) throws BadLocationException {
      if (doNotChangeDocument) {
        return;
      }

      // collect rollback information before performing the insert
      final String valueBeforeEdit = comboBoxEditorComponent.getText();
      final int selectionStart = comboBoxEditorComponent.getSelectionStart();
      final int selectionEnd = comboBoxEditorComponent.getSelectionEnd();

      super.insertString(filterBypass, offset, string, attributeSet);
      postProcessDocumentChange(filterBypass, attributeSet, valueBeforeEdit, selectionStart, selectionEnd, true);
    }

    @Override
    public void remove(FilterBypass filterBypass, int offset, int length) throws BadLocationException {
      if (doNotChangeDocument) {
        return;
      }

      // collect rollback information before performing the remove
      final String valueBeforeEdit = comboBoxEditorComponent.getText();
      final int selectionStart = comboBoxEditorComponent.getSelectionStart();
      final int selectionEnd = comboBoxEditorComponent.getSelectionEnd();

      super.remove(filterBypass, offset, length);
      postProcessDocumentChange(filterBypass, null, valueBeforeEdit, selectionStart, selectionEnd, isStrict());
    }

    /**
     * This method generically post processes changes to the ComboBox editor's Document. The generic algorithm, regardless of the type of change, is
     * as follows:
     *
     * <ol>
     * <li>save the prefix as the user has entered it
     * <li>filter the combo box items against the prefix
     * <li>update the text in the combo box editor with an autocomplete suggestion
     * <li>try to show the popup, if possible
     * </ol>
     */
    private void postProcessDocumentChange(FilterBypass filterBypass, AttributeSet attributeSet, String valueBeforeEdit, int selectionStart,
        int selectionEnd, boolean allowPartialAutoCompletionTerm) throws BadLocationException {
      // break out early if we're flagged to not post process the Document change
      if (doNotPostProcessDocumentChanges) {
        return;
      }

      final String valueAfterEdit = comboBoxEditorComponent.getText();

      // if an autocomplete term could not be found and we're in strict mode, rollback the edit
      if (isStrict() && (findAutoCompleteTerm(valueAfterEdit) == NOT_FOUND) && !allItemsUnfiltered.isEmpty()) {
        // indicate the error to the user
        if (getBeepOnStrictViolation()) {
          UIManager.getLookAndFeel().provideErrorFeedback(comboBoxEditorComponent);
        }

        // rollback the edit
        doNotPostProcessDocumentChanges = true;
        try {
          comboBoxEditorComponent.setText(valueBeforeEdit);
        }
        finally {
          doNotPostProcessDocumentChanges = false;
        }

        // restore the selection as it existed
        comboBoxEditorComponent.select(selectionStart, selectionEnd);

        // do not continue post processing changes
        return;
      }

      // record the selection before post processing the Document change
      // (we'll use this to decide whether to broadcast an ActionEvent when choosing the next selected index)
      final Object selectedItemBeforeEdit = comboBox.getSelectedItem();

      updateFilter();
      applyFilter(prefix);
      selectAutoCompleteTerm(filterBypass, attributeSet, selectedItemBeforeEdit, allowPartialAutoCompletionTerm);
      togglePopup();
    }

    /**
     * This method will attempt to locate a reasonable autocomplete item from all combo box items and select it. It will also populate the combo box
     * editor with the remaining text which matches the autocomplete item and select it. If the selection changes and the JComboBox is not a Table
     * Cell Editor, an ActionEvent will be broadcast from the combo box.
     */
    private void selectAutoCompleteTerm(FilterBypass filterBypass, AttributeSet attributeSet, Object selectedItemBeforeEdit,
        boolean allowPartialAutoCompletionTerm) throws BadLocationException {
      // break out early if we're flagged to ignore attempts to autocomplete
      if (doNotAutoComplete) {
        return;
      }

      // determine if our prefix is empty (in which case we cannot use our filterMatcher to locate an autocompletion term)
      final boolean prefixIsEmpty = "".equals(prefix);

      // record the original caret position in case we don't want to disturb the text (occurs when an exact autocomplete term match is found)
      final int originalCaretPosition = comboBoxEditorComponent.getCaretPosition();

      // a flag to indicate whether a partial match or exact match exists on the autocomplete term
      boolean autoCompleteTermIsExactMatch = false;

      // search the combobox model for a value that starts with our prefix (called an autocompletion term)
      for (int i = 0, n = comboBoxModel.getSize(); i < n; i++) {
        String itemString = convertToString(comboBoxModel.getElementAt(i));

        // if itemString does not match the prefix, continue searching for an autocompletion term
        if (prefixIsEmpty ? !"".equals(itemString) : !filterMatcher.matches(itemString)) {
          continue;
        }

        // record the index and value that are our "best" autocomplete terms so far
        int matchIndex = i;
        String matchString = itemString;

        // search for an *exact* match in the remainder of the ComboBoxModel
        // before settling for the partial match we have just found
        for (int j = i; j < n; j++) {
          itemString = convertToString(comboBoxModel.getElementAt(j));

          // if we've located an exact match, use its index and value rather than the partial match
          if (prefix.equals(itemString)) {
            matchIndex = j;
            matchString = itemString;
            autoCompleteTermIsExactMatch = true;
            break;
          }
        }

        // if partial autocompletion terms are not allowed, and we only have a partial term, bail early
        if (!allowPartialAutoCompletionTerm && !prefix.equals(itemString)) {
          return;
        }

        // either keep the user's prefix or replace it with the itemString's prefix
        // depending on whether we correct the case
        if (getCorrectsCase() || isStrict()) {
          filterBypass.replace(0, prefix.length(), matchString, attributeSet);
        }
        else {
          final String itemSuffix = matchString.substring(prefix.length());
          filterBypass.insertString(prefix.length(), itemSuffix, attributeSet);
        }

        // select the autocompletion term
        final boolean silently = isTableCellEditor || GlazedListsImpl.equal(selectedItemBeforeEdit, matchString);
        selectItem(matchIndex, silently);

        if (autoCompleteTermIsExactMatch) {
          // if the term matched the original text exactly, return the caret to its original location
          comboBoxEditorComponent.setCaretPosition(originalCaretPosition);
        }
        else {
          // select the text after the prefix but before the end of the text (it represents the autocomplete text)
          comboBoxEditorComponent.select(prefix.length(), document.getLength());
        }

        return;
      }

      // reset the selection since we couldn't find the prefix in the model
      // (this has the side-effect of scrolling the popup to the top)
      final boolean silently = isTableCellEditor || selectedItemBeforeEdit == null;
      selectItem(-1, silently);
    }

    /**
     * Select the item at the given <code>index</code>. If <code>silent</code> is <tt>true</tt>, the JComboBox will not broadcast an ActionEvent.
     */
    private void selectItem(int index, boolean silently) {
      final Object valueToSelect = index == -1 ? null : comboBoxModel.getElementAt(index);

      // if nothing is changing about the selection, return immediately
      if (GlazedListsImpl.equal(comboBoxModel.getSelectedItem(), valueToSelect)) {
        return;
      }

      doNotChangeDocument = true;
      try {
        if (silently) {
          comboBoxModel.setSelectedItem(valueToSelect);
        }
        else {
          comboBox.setSelectedItem(valueToSelect);
        }
      }
      finally {
        doNotChangeDocument = false;
      }
    }
  }

  /**
   * Select the item at the given <code>index</code>. This method behaves differently in strict mode vs. non-strict mode.
   *
   * <p>
   * In strict mode, the selected index must always be valid, so using the down arrow key on the last item or the up arrow key on the first item
   * simply wraps the selection to the opposite end of the model.
   *
   * <p>
   * In non-strict mode, the selected index can be -1 (no selection), so we allow -1 to mean "adjust the value of the ComboBoxEditor to be the user's
   * text" and only wrap to the end of the model when -2 is reached. In short, <code>-1</code> is interpreted as "clear the selected item".
   * <code>-2</code> is interpreted as "the last element".
   */
  private void selectPossibleValue(int index) {
    if (isStrict()) {
      // wrap the index from past the start to the end of the model
      if (index < 0) {
        index = comboBox.getModel().getSize() - 1;
      }

      // wrap the index from past the end to the start of the model
      if (index > comboBox.getModel().getSize() - 1) {
        index = 0;
      }
    }
    else {
      // wrap the index from past the start to the end of the model
      if (index == -2) {
        index = comboBox.getModel().getSize() - 1;
      }
    }

    // check if the index is within a valid range
    final boolean validIndex = index >= 0 && index < comboBox.getModel().getSize();

    // if the index isn't valid, select nothing
    if (!validIndex) {
      index = -1;
    }

    // adjust only the value in the comboBoxEditorComponent, but leave the comboBoxModel unchanged
    doNotPostProcessDocumentChanges = true;
    try {
      // select the index
      if (isTableCellEditor) {
        // while operating as a TableCellEditor, no ActionListeners must be notified
        // when using the arrow keys to adjust the selection
        final ActionListener[] listeners = unregisterAllActionListeners(comboBox);
        try {
          comboBox.setSelectedIndex(index);
        }
        finally {
          registerAllActionListeners(comboBox, listeners);
        }
      }
      else {
        comboBox.setSelectedIndex(index);
      }

      // if the original index wasn't valid, we've cleared the selection
      // and must set the user's prefix into the editor
      if (!validIndex) {
        comboBoxEditorComponent.setText(prefix);

        // don't bother unfiltering the popup since we'll redisplay the popup immediately
        doNotClearFilterOnPopupHide = true;
        try {
          comboBox.hidePopup();
        }
        finally {
          doNotClearFilterOnPopupHide = false;
        }
        comboBox.showPopup();
      }
    }
    finally {
      doNotPostProcessDocumentChanges = false;
    }

    // if the comboBoxEditorComponent's values begins with the user's prefix, highlight the remainder of the value
    final String newSelection = comboBoxEditorComponent.getText();
    if (filterMatcher.matches(newSelection)) {
      comboBoxEditorComponent.select(prefix.length(), newSelection.length());
    }
  }

  /**
   * The action invoked by hitting the up or down arrow key.
   */
  private class MoveAction extends AbstractAction {
    private final int offset;

    public MoveAction(int offset) {
      this.offset = offset;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (comboBox.isShowing()) {
        if (comboBox.isPopupVisible()) {
          selectPossibleValue(comboBox.getSelectedIndex() + offset);
        }
        else {
          applyFilter(prefix);
          comboBox.showPopup();
        }
      }
    }
  }

  /**
   * This class listens to the ComboBoxModel and redraws the popup if it must grow or shrink to accomodate the latest list of items.
   */
  private class ListDataHandler implements ListDataListener {
    private int            previousItemCount                = -1;
    private final Runnable checkStrictModeInvariantRunnable = new ListDataHandler.CheckStrictModeInvariantRunnable();

    @Override
    public void contentsChanged(ListDataEvent e) {
      final int newItemCount = comboBox.getItemCount();

      // if the size of the model didn't change, the popup size won't change
      if (previousItemCount != newItemCount) {
        final int maxPopupItemCount = comboBox.getMaximumRowCount();

        // if the popup is showing, check if it must be resized
        if (popupMenu.isShowing()) {
          if (comboBox.isShowing()) {
            // if either the previous or new item count is less than the max,
            // hide and show the popup to recalculate its new height
            if (newItemCount < maxPopupItemCount || previousItemCount < maxPopupItemCount) {
              // don't bother unfiltering the popup since we'll redisplay the popup immediately
              doNotClearFilterOnPopupHide = true;
              try {
                comboBox.hidePopup();
              }
              finally {
                doNotClearFilterOnPopupHide = false;
              }

              comboBox.showPopup();
            }
          }
          else {
            // if the comboBox is not showing, simply hide the popup to avoid:
            // "java.awt.IllegalComponentStateException: component must be showing on the screen to determine its location"
            // this case can occur when the comboBox is used as a TableCellEditor
            // and is uninstalled (removed from the component hierarchy) before
            // receiving this callback
            comboBox.hidePopup();
          }
        }

        previousItemCount = newItemCount;
      }

      // if the comboBoxModel was changed and it wasn't due to the filter changing
      // (i.e. !isFiltering) and it wasn't because the user selected a new
      // selectedItem (i.e. !userSelectedNewItem) then those changes may have
      // invalidated the invariant that strict mode places on the text in the
      // JTextField, so we must either:
      //
      // a) locate the text within the model (proving that the strict mode invariant still holds)
      // b) set the text to that of the first element in the model (to reestablish the invariant)
      final boolean userSelectedNewItem = e.getIndex0() == -1 || e.getIndex1() == -1;
      if (isStrict() && !userSelectedNewItem && !isFiltering) {
        // notice that instead of doing the work directly, we post a Runnable here
        // to check the strict mode invariant and repair it if it is broken. That's
        // important. It's necessary because we must let the current ListEvent
        // finish its dispatching before we attempt to change the filter of the
        // filteredItems list by setting new text into the comboBoxEditorComponent
        SwingUtilities.invokeLater(checkStrictModeInvariantRunnable);
      }
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
      contentsChanged(e);
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
      contentsChanged(e);
    }

    private class CheckStrictModeInvariantRunnable implements Runnable {
      @Override
      public void run() {
        final JTextField editor = comboBoxEditorComponent;
        if (editor != null) {
          final String currentText = editor.getText();
          final Object item = findAutoCompleteTerm(currentText);
          String itemText = convertToString(item);

          // if we did not find the same autocomplete term
          if (!currentText.equals(itemText)) {
            // select the first item if we could not find an autocomplete term with the currentText
            if (item == NOT_FOUND && !allItemsUnfiltered.isEmpty()) {
              itemText = convertToString(allItemsUnfiltered.get(0));
            }

            // set the new strict value text into the editor component
            editor.setText(itemText);
          }
        }
      }
    }
  }

  /**
   * This class sizes the popup menu of the combo box immediately before it is shown on the screen. In particular, it will adjust the width of the
   * popup to accomodate a prototype display value if the combo box contains one.
   */
  private class PopupSizer implements PopupMenuListener {
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      // if the combo box does not contain a prototype display value, skip our sizing logic
      final E prototypeValue = comboBox.getPrototypeDisplayValue();
      if (prototypeValue == null) {
        return;
      }

      final JComponent popupComponent = (JComponent) e.getSource();

      // attempt to extract the JScrollPane that scrolls the popup
      if (popupComponent.getComponent(0) instanceof JScrollPane) {
        final JScrollPane scroller = (JScrollPane) popupComponent.getComponent(0);

        // fetch the existing preferred size of the scroller, and we'll check if it is large enough
        final Dimension scrollerSize = scroller.getPreferredSize();

        // calculates the preferred size of the renderer's component for the prototype value
        final Dimension prototypeSize = getPrototypeSize(prototypeValue);

        // add to the preferred width, the width of the vertical scrollbar, when it is visible
        prototypeSize.width += scroller.getVerticalScrollBar().getPreferredSize().width;

        // adjust the preferred width of the scroller, if necessary
        if (prototypeSize.width > scrollerSize.width) {
          scrollerSize.width = prototypeSize.width;

          // set the new size of the scroller
          scroller.setMaximumSize(scrollerSize);
          scroller.setPreferredSize(scrollerSize);
          scroller.setMinimumSize(scrollerSize);
        }
      }
    }

    private Dimension getPrototypeSize(E prototypeValue) {
      // get the renderer responsible for drawing the prototype value
      ListCellRenderer<? super E> renderer = comboBox.getRenderer();
      if (renderer == null) {
        renderer = new DefaultListCellRenderer();
      }

      // get the component from the renderer
      final Component comp = renderer.getListCellRendererComponent((JList<E>) popup.getList(), prototypeValue, -1, false, false);

      // determine the preferred size of the component
      comp.setFont(comboBox.getFont());
      return comp.getPreferredSize();
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      if (doNotClearFilterOnPopupHide) {
        return;
      }

      // the popup menu is being hidden, so clear the filter to return the ComboBoxModel to its unfiltered state
      applyFilter("");
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }
  }

  /**
   * When the user selects a value from the popup with the mouse, we want to honour their selection *without* attempting to autocomplete it to a new
   * term. Otherwise, it is possible that selections which are prefixes for values that appear higher in the ComboBoxModel cannot be selected by the
   * mouse since they can always be successfully autocompleted to another term.
   */
  private class PopupMouseHandler extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      doNotAutoComplete = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      doNotAutoComplete = false;
    }
  }

  /**
   * When the user clicks on the arrow button, we always clear the filtering from the model to emulate Firefox style autocompletion.
   */
  private class ArrowButtonMouseListener implements MouseListener {
    private final MouseListener decorated;

    public ArrowButtonMouseListener(MouseListener decorated) {
      this.decorated = decorated;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // clear the filter if we're about to hide or show the popup
      // by clicking on the arrow button (this is EXPLICITLY different
      // than using the up/down arrow keys to show the popup)
      applyFilter("");
      decorated.mousePressed(e);
    }

    public MouseListener getDecorated() {
      return decorated;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      decorated.mouseClicked(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      decorated.mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      decorated.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      decorated.mouseExited(e);
    }
  }

  /**
   * This KeyListener handles the case when the user hits the backspace key and the {@link AutocompleteSupport} is strict. Normally backspace would
   * delete the selected text, if it existed, or delete the character immediately preceding the cursor. In strict mode the ComboBoxEditor must always
   * contain a value from the ComboBoxModel, so the backspace key <strong>NEVER</strong> alters the Document. Rather, it alters the text selection to
   * include one more character to the left. This is a nice compromise, since the editor continues to retain a valid value from the ComboBoxModel, but
   * the user may type a key at any point to replace the selection with another valid entry.
   *
   * This KeyListener also makes up for a bug in normal JComboBox when handling the enter key. Specifically, hitting enter in an stock JComboBox that
   * is editable produces <strong>TWO</strong> ActionEvents. When the enter key is detected we actually unregister all ActionListeners, process the
   * keystroke as normal, then reregister the listeners and broadcast an event to them, producing a single ActionEvent.
   */
  private class AutoCompleteKeyHandler extends KeyAdapter {
    private ActionListener[] actionListeners;

    @Override
    public void keyPressed(KeyEvent e) {
      if (!isTableCellEditor) {
        doNotTogglePopup = false;
      }

      // this KeyHandler performs ALL processing of the ENTER key otherwise multiple
      // ActionEvents are fired to ActionListeners by the default JComboBox processing.
      // To control processing of the enter key, we set a flag to avoid changing the
      // editor's Document in any way, and also unregister the ActionListeners temporarily.
      if (e.getKeyChar() == KeyEvent.VK_ENTER) {
        doNotChangeDocument = true;
        this.actionListeners = unregisterAllActionListeners(comboBox);
      }

      // make sure this backspace key does not modify our comboBoxEditorComponent's Document
      if (isTrigger(e)) {
        doNotChangeDocument = true;
      }
    }

    @Override
    public void keyTyped(KeyEvent e) {
      if (isTrigger(e)) {
        // if no content exists in the comboBoxEditorComponent, bail early
        if (comboBoxEditorComponent.getText().length() == 0) {
          return;
        }

        // calculate the current beginning of the selection
        int selectionStart = Math.min(comboBoxEditorComponent.getSelectionStart(), comboBoxEditorComponent.getSelectionEnd());

        // if we cannot extend the selection to the left, indicate the error
        if (selectionStart == 0) {
          if (getBeepOnStrictViolation()) {
            UIManager.getLookAndFeel().provideErrorFeedback(comboBoxEditorComponent);
          }
          return;
        }

        // add one character to the left of the selection
        selectionStart--;

        // select the text from the end of the Document to the new selectionStart
        // (which positions the caret at the selectionStart)
        comboBoxEditorComponent.setCaretPosition(comboBoxEditorComponent.getText().length());
        comboBoxEditorComponent.moveCaretPosition(selectionStart);
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      // resume the ability to modify our comboBoxEditorComponent's Document
      if (isTrigger(e)) {
        doNotChangeDocument = false;
      }

      // keyPressed(e) has disabled the JComboBox's normal processing of the enter key
      // so now it is time to perform our own processing. We reattach all ActionListeners
      // and simulate exactly ONE ActionEvent in the JComboBox and then reenable Document changes.
      if (e.getKeyChar() == KeyEvent.VK_ENTER) {
        updateFilter();

        // reregister all ActionListeners and then notify them due to the ENTER key

        // Note: We *must* check for a null ActionListener[]. The reason
        // is that it is possible to receive a keyReleased() callback
        // *without* a corresponding keyPressed() callback! It occurs
        // when focus is transferred away from the ComboBoxEditor and
        // then the ENTER key transfers focus back to the ComboBoxEditor.
        if (actionListeners != null) {
          registerAllActionListeners(comboBox, actionListeners);
          comboBox.actionPerformed(new ActionEvent(e.getSource(), e.getID(), null));
        }

        // null out our own reference to the ActionListeners
        actionListeners = null;

        // reenable Document changes once more
        doNotChangeDocument = false;
      }

      if (!isTableCellEditor) {
        doNotTogglePopup = true;
      }
    }

    private boolean isTrigger(KeyEvent e) {
      return isStrict() && e.getKeyChar() == KeyEvent.VK_BACK_SPACE;
    }
  }

  /**
   * To emulate Firefox behaviour, all text in the ComboBoxEditor is selected from beginning to end when the ComboBoxEditor gains focus if the value
   * returned from {@link AutocompleteSupport#getSelectsTextOnFocusGain()} allows this behaviour. In addition, the JPopupMenu is hidden when the
   * ComboBoxEditor loses focus if the value returned from {@link AutocompleteSupport#getHidesPopupOnFocusLost()} allows this behaviour.
   */
  private class ComboBoxEditorFocusHandler extends FocusAdapter {
    @Override
    public void focusGained(FocusEvent e) {
      if (getSelectsTextOnFocusGain()) {
        comboBoxEditorComponent.select(0, comboBoxEditorComponent.getText().length());
      }
    }

    @Override
    public void focusLost(FocusEvent e) {
      if (comboBox.isPopupVisible() && getHidesPopupOnFocusLost()) {
        comboBox.setPopupVisible(false);
      }
    }
  }

  /**
   * Watch for a change of the ComboBoxUI and reinstall the necessary behaviour customizations.
   */
  private class UIWatcher implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      undecorateOriginalUI();
      decorateCurrentUI();
    }
  }

  /**
   * Watch for a change of the ComboBoxModel and report it as a violation.
   */
  private class ModelWatcher implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      throwIllegalStateException("The ComboBoxModel cannot be changed. It was changed to: " + evt.getNewValue());
    }
  }

  /**
   * Watch the Document behind the editor component in case it changes. If a new Document is swapped in, uninstall our DocumentFilter from the old
   * Document and install it on the new.
   */
  private class DocumentWatcher implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      final Document newDocument = (Document) evt.getNewValue();

      if (!(newDocument instanceof AbstractDocument)) {
        throwIllegalStateException(
            "The Document behind the JTextField was changed to no longer be an AbstractDocument. It was changed to: " + newDocument);
      }

      // remove our DocumentFilter from the old document
      document.setDocumentFilter(null);

      // update the document we track internally
      document = (AbstractDocument) newDocument;

      // add our DocumentFilter to the new Document
      document.setDocumentFilter(documentFilter);
    }
  }

  /**
   * A custom renderer which honours the custom Format given by the user when they invoked the install method.
   */
  private class StringFunctionRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      String string = convertToString(value);

      // JLabels require some text before they can correctly determine their height, so we convert "" to " "
      if (string.length() == 0) {
        string = " ";
      }
      return super.getListCellRendererComponent(list, string, index, isSelected, cellHasFocus);
    }
  }

  /**
   * A decorated version of the ComboBoxEditor that does NOT assume that Object.toString() is the proper way to convert values from the ComboBoxModel
   * into Strings for the ComboBoxEditor's component. It uses convertToString(E) instead.
   *
   * We implement the UIResource interface here so that changes in the UI delegate of the JComboBox will *replace* this ComboBoxEditor with one that
   * is correct for the new L&F. We will then react to the change of UI delegate by installing a new FormatComboBoxEditor overtop of the UI Delegate's
   * default ComboBoxEditor.
   */
  private class FormatComboBoxEditor implements ComboBoxEditor, UIResource {

    /** This is the ComboBoxEditor installed by the current UI Delegate of the JComboBox. */
    private final ComboBoxEditor delegate;
    private Object               oldValue;

    public FormatComboBoxEditor(ComboBoxEditor delegate) {
      this.delegate = delegate;
    }

    public ComboBoxEditor getDelegate() {
      return delegate;
    }

    /**
     * BasicComboBoxEditor defines this method to call:
     *
     * editor.setText(anObject.toString());
     *
     * we intercept and replace it with our own String conversion logic to remain consistent throughout.
     */
    @Override
    public void setItem(Object anObject) {
      oldValue = anObject;
      if (getEditorComponent() instanceof JTextField) {
        ((JTextField) getEditorComponent()).setText(convertToString(anObject));
      }
    }

    /**
     * BasicComboBoxEditor defines this method to use reflection to try finding a method called valueOf(String) in order to return the item. We
     * attempt to find a user-supplied Format before resorting to the valueOf(String) call.
     */
    @Override
    public Object getItem() {
      final String oldValueString = convertToString(oldValue);
      final String currentString = ((JTextField) getEditorComponent()).getText();

      // if the String value in the editor matches the String version of
      // the last item that was set in the editor, return the item
      if (GlazedListsImpl.equal(oldValueString, currentString)) {
        return oldValue;
      }

      // if the user specified a Format, use it
      if (format != null) {
        return format.parseObject(currentString, PARSE_POSITION);
      }

      // otherwise, use the default algorithm from BasicComboBoxEditor to produce a value
      if (oldValue != null && !(oldValue instanceof String)) {
        try {
          final Method method = oldValue.getClass().getMethod("valueOf", VALUE_OF_SIGNATURE);
          return method.invoke(oldValue, currentString);
        }
        catch (Exception ex) {
          // fail silently and return the current string
        }
      }

      return currentString;
    }

    @Override
    public Component getEditorComponent() {
      return delegate.getEditorComponent();
    }

    @Override
    public void selectAll() {
      delegate.selectAll();
    }

    @Override
    public void addActionListener(ActionListener l) {
      delegate.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
      delegate.removeActionListener(l);
    }
  }

  /**
   * This default implementation of the TextFilterator interface uses the same strategy for producing Strings from ComboBoxModel objects as the
   * renderer and editor.
   */
  class DefaultTextFilterator implements TextFilterator<E> {
    @Override
    public void getFilterStrings(List<String> baseList, E element) {
      baseList.add(convertToString(element));
    }
  }

  /**
   * This extension of DefaultCellEditor exists solely to provide a handle to the AutocompleteSupport object that is providing autocompletion
   * capabilities to the JComboBox.
   */
  public static class AutoCompleteCellEditor<E> extends DefaultCellEditor {
    private final AutocompleteSupport<E> autoCompleteSupport;

    /**
     * Construct a TableCellEditor using the JComboBox supplied by the given <code>autoCompleteSupport</code>. Specifically, the JComboBox is
     * retrieved using {@link AutocompleteSupport#getComboBox()}.
     */
    public AutoCompleteCellEditor(AutocompleteSupport<E> autoCompleteSupport) {
      super(autoCompleteSupport.getComboBox());
      this.autoCompleteSupport = autoCompleteSupport;
    }

    /**
     * Returns the AutocompleteSupport object that controls the autocompletion behaviour for the JComboBox.
     */
    public AutocompleteSupport<E> getAutoCompleteSupport() {
      return autoCompleteSupport;
    }
  }
}
