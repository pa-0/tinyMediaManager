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
package org.tinymediamanager.ui.components.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * The class TmmTreeModel is the base class for the tree model of the TmmTree
 * 
 * @author Manuel Laggner
 *
 * @param <E>
 */
public class TmmTreeModel<E extends TmmTreeNode> extends DefaultTreeModel {
  protected static final long            TIMER_DELAY               = 100L;

  protected final TmmTreeDataProvider<E> dataProvider;
  protected final TmmTree<E>             tree;

  protected E                            rootNode                  = null;

  // node cache fir quick searching a node
  protected final Map<Object, E>         nodeCache                 = new HashMap<>();
  // nodes cached states (parent -> children cached state).
  protected final Map<Object, Boolean>   nodeCached                = new HashMap<>();
  // cache for children nodes returned by data provider (parent ID -> list of raw child nodes).
  protected final Map<Object, List<E>>   rawNodeChildrenCache      = new HashMap<>();
  // cache for children nodes returned by data provider (parent ID -> list of filtered nodes).
  protected final Map<String, List<E>>   filteredNodeChildrenCache = new HashMap<>();
  // lock for accessing the cache
  protected final ReadWriteLock          readWriteLock             = new ReentrantReadWriteLock();
  // just don't do some UI tasks while the structure is changing
  protected boolean                      isAdjusting               = false;
  // throttle update of the UI
  protected long                         nextUpdateSortAndFilter   = 0;
  protected DelayedUpdateTask            updateSortAndFilterTask;
  protected long                         nextNodeStructureChanged  = 0;
  protected DelayedUpdateTask            nodeStructureChangedTask;

  /**
   * Create a new instance of the TmmTreeModel for the given TmmTree and data provider
   * 
   * @param tree
   *          the TmmTree to create the model for
   * @param dataProvider
   *          the data provider to create the model for
   */
  public TmmTreeModel(final TmmTree<E> tree, final TmmTreeDataProvider<E> dataProvider) {
    super(null);
    this.tree = tree;
    this.dataProvider = dataProvider;
    if (dataProvider.getTreeFilters() == null) {
      this.dataProvider.setTreeFilters(new HashSet<>());
    }

    dataProvider.addPropertyChangeListener(evt -> {
      // a node has been inserted
      if (TmmTreeDataProvider.NODE_INSERTED.equals(evt.getPropertyName()) && evt.getNewValue() instanceof TmmTreeNode) {
        E child = (E) evt.getNewValue();
        E parent = dataProvider.getParent(child);

        // only add if the child has not been added (yet)
        if (child.getParent() == null) {
          addChildNode(parent, child);
        }
      }
      // a node has been removed
      if (TmmTreeDataProvider.NODE_REMOVED.equals(evt.getPropertyName()) && evt.getNewValue() instanceof TmmTreeNode) {
        E child = (E) evt.getNewValue();
        removeChildNode(child);
      }
      // a node has been changed
      if (TmmTreeDataProvider.NODE_CHANGED.equals(evt.getPropertyName()) && evt.getNewValue() instanceof TmmTreeNode) {
        E child = (E) evt.getNewValue();
        nodeChanged(child);

        TreeNode[] path = child.getPath();
        if (path != null && path.length > 1) {
          // remove cache for all children
          readWriteLock.writeLock().lock();
          for (int i = 1; i < path.length; i++) {
            TmmTreeNode parent = (TmmTreeNode) path[i];
            filteredNodeChildrenCache.remove(parent.getId());
          }
          readWriteLock.writeLock().unlock();

          updateSortingAndFiltering();
        }
      }
      // the structure has been changed
      if (TmmTreeDataProvider.NODE_STRUCTURE_CHANGED.equals(evt.getPropertyName()) && evt.getNewValue() instanceof TmmTreeNode) {
        nodeStructureChanged();
      }
    });
    loadTreeData(getRoot());
  }

  /**
   * Get the data provider for this model
   *
   * @return the data provider set for this model
   */
  public TmmTreeDataProvider<E> getDataProvider() {
    return dataProvider;
  }

  /**
   * load the tree data (to build up the cache)
   * 
   * @param node
   *          the parent node to build the tree data for
   */
  protected void loadTreeData(final E node) {
    getChildCount(node);
  }

  /**
   * get the root node
   * 
   * @return the root node
   */
  @Override
  public E getRoot() {
    if (rootNode == null) {
      // Retrieving and caching root node
      rootNode = dataProvider.getRoot();
      cacheNode(rootNode);
    }
    return rootNode;
  }

  /**
   * get the child count for the given node
   * 
   * @param parent
   *          the node to get the child count for
   * @return the child count
   */
  @SuppressWarnings("unchecked")
  @Override
  public int getChildCount(final Object parent) {
    final E node = (E) parent;
    if (isLeaf(node)) {
      return 0;
    }
    else if (areChildrenLoaded(node)) {
      return super.getChildCount(parent);
    }
    else {
      return loadChildren(node);
    }
  }

  /**
   * Returns whether the specified node is leaf or not.
   *
   * @param node
   *          node
   * @return true if node is leaf, false otherwise
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean isLeaf(final Object node) {
    return dataProvider.isLeaf((E) node);
  }

  /**
   * load all children into the cache
   * 
   * @param parent
   *          the parent node to load all children for
   * @return the child count
   */
  protected int loadChildren(final E parent) {
    // Loading children
    final List<E> children = dataProvider.getChildren(parent);

    // Caching raw children
    readWriteLock.writeLock().lock();
    rawNodeChildrenCache.put(parent.getId(), children);
    cacheNodes(children);
    readWriteLock.writeLock().unlock();

    // Filtering and sorting raw children
    final List<E> realChildren = filterAndSort(parent, children);

    // Updating cache
    readWriteLock.writeLock().lock();
    nodeCached.put(parent.getId(), true);
    readWriteLock.writeLock().unlock();

    // Checking if any nodes loaded
    if (realChildren != null && !realChildren.isEmpty()) {
      // Inserting loaded nodes
      insertNodesInto(realChildren, parent, 0);
    }

    return parent.getChildCount();
  }

  /**
   * invalidate the filter cache
   */
  public void invalidateFilterCache() {
    filteredNodeChildrenCache.clear();
  }

  /**
   * Updates nodes sorting and filtering for all nodes.
   */
  public void updateSortingAndFiltering() {
    updateSortingAndFiltering(getRoot());
  }

  /**
   * Updates nodes sorting and filtering for the given subtree.
   */
  public void updateSortingAndFiltering(E parent) {
    long now = System.currentTimeMillis();

    if (now > nextUpdateSortAndFilter) {
      // Saving tree state to restore it right after children update
      TmmTreeState treeState = null;
      if (this.tree != null) {
        treeState = tree.getTreeState();
      }

      // Updating root node children
      setAdjusting(true);
      performFilteringAndSortingRecursively(parent);

      setAdjusting(false);
      nodeStructureChanged(treeState);

      long end = System.currentTimeMillis();

      if ((end - now) < TIMER_DELAY) {
        // logic has been run within the delay time
        nextUpdateSortAndFilter = end + TIMER_DELAY;
      }
      else {
        // logic was slower than the interval - increase the interval adaptively
        nextUpdateSortAndFilter = end + (end - now) * 2;
      }
    }
    else {
      startUpdateSortAndFilterTimer();
    }
  }

  protected void startUpdateSortAndFilterTimer() {
    // lazily update the node structure to prevent UI locking
    if (updateSortAndFilterTask != null) {
      // a timer is already running
      // register for another run
      updateSortAndFilterTask.updateNeeded = true;
      return;
    }

    Timer updateSortAndFilterTimer = new Timer();
    updateSortAndFilterTask = new DelayedUpdateTask() {
      @Override
      public void run() {
        SwingUtilities.invokeLater(() -> {
          updateSortingAndFiltering();
          updateSortAndFilterTask = null;

          // check if one more update is needed
          if (updateNeeded) {
            startUpdateSortAndFilterTimer();
          }
        });
      }
    };

    // fire that after the timer delay
    updateSortAndFilterTimer.schedule(updateSortAndFilterTask, TIMER_DELAY);
  }

  /**
   * check if there are any active filters
   *
   * @return true if there is at least one active filter
   */
  protected boolean hasActiveFilters() {
    if (dataProvider.getTreeFilters() == null || !dataProvider.isFiltersActive()) {
      return false;
    }

    for (ITmmTreeFilter<E> filter : dataProvider.getTreeFilters()) {
      if (filter.isActive()) {
        return true;
      }
    }
    return false;
  }

  public void nodeStructureChanged() {
    nodeStructureChanged((TmmTreeState) null);
  }

  public void nodeStructureChanged(TmmTreeState treeState) {
    long now = System.currentTimeMillis();

    if (now > nextNodeStructureChanged) {
      nodeStructureChanged(getRoot());

      // Restoring tree state including all selections and expansions
      if (treeState != null && tree != null) {
        tree.setTreeState(treeState);
      }

      long end = System.currentTimeMillis();

      if ((end - now) < TIMER_DELAY) {
        // logic has been run within the delay time
        nextNodeStructureChanged = end + TIMER_DELAY;
      }
      else {
        // logic was slower than the interval - increase the interval adaptively
        nextNodeStructureChanged = end + (end - now) * 2;
      }
    }
    else {
      startNodeStructureChangedTimer(treeState);
    }
  }

  protected void startNodeStructureChangedTimer(TmmTreeState treeState) {
    // lazily update the node structure to prevent UI locking
    if (nodeStructureChangedTask != null) {
      // a timer is already running
      // register for another run
      nodeStructureChangedTask.updateNeeded = true;
      return;
    }

    Timer nodeStructureChangedTimer = new Timer();
    nodeStructureChangedTask = new DelayedUpdateTask() {
      @Override
      public void run() {
        SwingUtilities.invokeLater(() -> {
          // since we do this not often, we can call it on root
          nodeStructureChanged(getRoot());

          // Restoring tree state including all selections and expansions
          if (treeState != null && tree != null) {
            tree.setTreeState(treeState);
          }

          nodeStructureChangedTask = null;

          // check if one more update is needed
          if (updateNeeded) {
            startNodeStructureChangedTimer(treeState);
          }
        });
      }
    };

    // fire that after the timer delay
    nodeStructureChangedTimer.schedule(nodeStructureChangedTask, TIMER_DELAY);
  }

  /**
   * Updates node children using current comparator and filter.
   *
   * @param parentNode
   *          node to update
   */
  @SuppressWarnings("unchecked")
  protected boolean performFilteringAndSortingRecursively(final E parentNode) {
    boolean nodesChanged = false;
    nodesChanged = performFilteringAndSorting(parentNode) || nodesChanged;
    for (int i = 0; i < parentNode.getChildCount(); i++) {
      nodesChanged = performFilteringAndSortingRecursively((E) parentNode.getChildAt(i)) || nodesChanged;
    }
    return nodesChanged;
  }

  /**
   * Updates node children recursively using current comparator and filter.
   *
   * @param parentNode
   *          node to update
   */
  protected boolean performFilteringAndSorting(final E parentNode) {
    boolean nodesChanged = false;

    // Retrieving raw children
    final List<E> children = rawNodeChildrenCache.get(parentNode.getId());

    // get all _old_ children from the parent
    final List<E> oldChildren = getChildren(parentNode);
    final List<E> newChildren = filterAndSort(parentNode, children);

    // Process this action only if node children are already loaded and cached
    if (children != null && !oldChildren.equals(newChildren)) {
      nodesChanged = true;
      // Removing old children
      parentNode.removeAllChildren();

      // Filtering and sorting raw children
      final List<E> realChildren = filterAndSort(parentNode, children);

      // Inserting new children
      for (final E child : realChildren) {
        parentNode.add(child);
      }
    }
    return nodesChanged;
  }

  /**
   * get a list of all children from the given node
   * 
   * @param parent
   *          the given node to get all children for
   * @return a list of all children
   */
  private List<E> getChildren(final E parent) {
    List<E> children = new ArrayList<>();

    Enumeration e = parent.children();
    while (e.hasMoreElements()) {
      children.add((E) e.nextElement());
    }

    return children;
  }

  /**
   * filter and sort the children
   * 
   * @param parentNode
   *          the parent node to filter and sort the children for
   * @param children
   *          a list of all children to filter/sort
   * @return a list of all filtered/sorted children
   */
  protected List<E> filterAndSort(final E parentNode, List<E> children) {
    // Simply return an empty array if there is no children
    if (children == null || children.isEmpty()) {
      return Collections.emptyList();
    }

    // get cache
    readWriteLock.readLock().lock();
    List<E> filteredNodes = filteredNodeChildrenCache.get(parentNode.getId());
    readWriteLock.readLock().unlock();
    if (filteredNodes != null) {
      return filteredNodes;
    }

    // Filter children
    final List<E> filteredAndSorted = new ArrayList<>();

    if (hasActiveFilters()) {
      final Set<ITmmTreeFilter<E>> filters = dataProvider.getTreeFilters();

      // filter
      for (final E element : new ArrayList<>(children)) {
        // filter over all set filters
        boolean accepted = true;
        for (ITmmTreeFilter<E> filter : filters) {
          if (!filter.accept(element)) {
            accepted = false;
          }
        }
        if (accepted) {
          filteredAndSorted.add(element);
        }
      }
    }
    else {
      filteredAndSorted.addAll(children);
    }

    // sort
    final Comparator<E> comparator = dataProvider.getTreeComparator();
    if (comparator != null) {
      try {
        filteredAndSorted.sort(comparator);
      }
      catch (Exception e) {
        // sometimes if the underlying is actively updated, an IllegalArgumentException can be thrown here, so just catch it
      }
    }

    // get cache
    readWriteLock.writeLock().lock();
    filteredNodeChildrenCache.put(parentNode.getId(), filteredAndSorted);
    readWriteLock.writeLock().unlock();

    return filteredAndSorted;
  }

  /**
   * Adds a single child node for the specified parent node
   * 
   * @param parent
   *          the parent node to add the child to
   * 
   * @param child
   *          the child to be added
   */
  public void addChildNode(final E parent, final E child) {
    // ignore null nodes
    if (child == null || parent == null) {
      return;
    }

    addChildNodes(parent, Collections.singletonList(child));
  }

  /**
   * Adds child nodes for the specified parent node.
   *
   * @param parent
   *          the parent node to add the children to
   * @param children
   *          the children to be added
   */
  public void addChildNodes(final E parent, final List<E> children) {
    // ignore null nodes
    if (children == null || children.isEmpty() || parent == null) {
      return;
    }
    setAdjusting(true);

    // Adding new raw children
    readWriteLock.writeLock().lock();
    List<E> cachedChildren = rawNodeChildrenCache.get(parent.getId());
    if (cachedChildren == null) {
      cachedChildren = new ArrayList<>(children.size());
      rawNodeChildrenCache.put(parent.getId(), cachedChildren);
    }
    cachedChildren.addAll(children);
    cacheNodes(children);

    // force re-calculate of the whole subtree
    for (Object obj : parent.getPath()) {
      if (obj instanceof TmmTreeNode) {
        filteredNodeChildrenCache.remove(((TmmTreeNode) obj).getId());
      }
    }
    filteredNodeChildrenCache.remove(getRoot().getId());

    readWriteLock.writeLock().unlock();

    // Clearing nodes cache
    // That might be required in case nodes were moved inside the tree
    clearNodeChildrenCache(children, false);

    // Inserting nodes
    insertNodesInto(children, parent, parent.getChildCount());

    setAdjusting(false);

    // Updating parent node sorting and filtering
    updateSortingAndFiltering();
  }

  /**
   * Removes specified node from its parent node.
   *
   * @param node
   *          node to be removed
   */
  @SuppressWarnings("unchecked")
  public void removeChildNode(final E node) {
    // Simply ignore null nodes
    if (node == null) {
      return;
    }
    setAdjusting(true);

    final E parent = (E) node.getParent();

    // Simply ignore if parent node is null or not yet loaded
    if (parent == null) {
      return;
    }

    // Removing raw children
    readWriteLock.writeLock().lock();
    final List<E> children = rawNodeChildrenCache.get(parent.getId());
    if (children != null) {
      children.remove(node);
    }

    // force re-calculate of the whole subtree
    for (Object obj : parent.getPath()) {
      if (obj instanceof TmmTreeNode) {
        filteredNodeChildrenCache.remove(((TmmTreeNode) obj).getId());
      }
    }
    filteredNodeChildrenCache.remove(getRoot().getId());

    readWriteLock.writeLock().unlock();

    // Clearing node cache
    clearNodeChildrenCache(node, true);

    // Removing node children so they won't mess up anything when we place node back
    // into tree
    node.removeAllChildren();

    // Removing node from parent
    super.removeNodeFromParent(node);

    setAdjusting(false);

    // Updating parent node sorting and filtering
    updateSortingAndFiltering();
  }

  /**
   * remove the node from its parent
   * 
   * @param node
   *          the node to remove
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeNodeFromParent(MutableTreeNode node) {
    removeChildNode((E) node);
  }

  /**
   * Inserts a child node into parent node.
   *
   * @param child
   *          new child node
   * @param parent
   *          parent node
   * @param index
   *          insert index
   */
  protected void insertNodeIntoImpl(final E child, final E parent, final int index) {
    super.insertNodeInto(child, parent, index);

    // Forcing child node to load its structure
    loadTreeData(child);
  }

  /**
   * Inserts a list of child nodes into parent node.
   *
   * @param children
   *          array of new child nodes
   * @param parent
   *          parent node
   * @param index
   *          insert index
   */
  public void insertNodesInto(final List<E> children, final E parent, final int index) {
    for (int i = children.size() - 1; i >= 0; i--) {
      parent.insert(children.get(i), index);
    }

    final int[] indices = new int[children.size()];
    for (int i = 0; i < children.size(); i++) {
      indices[i] = index + i;
    }

    try {
      nodesWereInserted(parent, indices);
    }
    catch (Exception ignored) {
      // sometimes this crashes when the tree is refreshing and the cursor is in the wrong location
    }

    // Forcing child nodes to load their structures
    for (final E child : children) {
      loadTreeData(child);
    }
  }

  /**
   * Inserts an array of child nodes into parent node.
   *
   * @param children
   *          array of new child nodes
   * @param parent
   *          parent node
   * @param index
   *          insert index
   */
  public void insertNodesInto(final E[] children, final E parent, final int index) {
    for (int i = children.length - 1; i >= 0; i--) {
      parent.insert(children[i], index);
    }

    final int[] indices = new int[children.length];
    for (int i = 0; i < children.length; i++) {
      indices[i] = index + i;
    }

    nodesWereInserted(parent, indices);

    // Forcing child nodes to load their structures
    for (final E child : children) {
      loadTreeData(child);
    }
  }

  /**
   * Clears node and all of its child nodes children cached states.
   *
   * @param node
   *          node to clear cache for
   * @param clearNode
   *          whether should clear node cache or not
   */
  protected void clearNodeChildrenCache(final E node, final boolean clearNode) {
    readWriteLock.writeLock().lock();
    // Clears node cache
    if (clearNode) {
      nodeCache.remove(node.getId());
    }

    // Clears node children cached state
    nodeCached.remove(node.getId());

    // Clears node raw children cache
    final List<E> children = rawNodeChildrenCache.remove(node.getId());
    readWriteLock.writeLock().unlock();

    // Clears child nodes cache
    if (children != null) {
      clearNodeChildrenCache(children, true);
    }
  }

  /**
   * Clears nodes children cached states.
   *
   * @param nodes
   *          nodes to clear cache for
   * @param clearNodes
   *          whether should clear nodes cache or not
   */
  protected void clearNodeChildrenCache(final List<E> nodes, final boolean clearNodes) {
    for (final E node : nodes) {
      clearNodeChildrenCache(node, clearNodes);
    }
  }

  /**
   * Clears nodes children cached states.
   *
   * @param nodes
   *          nodes to clear cache for
   * @param clearNodes
   *          whether should clear nodes cache or not
   */
  protected void clearNodeChildrenCache(final E[] nodes, final boolean clearNodes) {
    for (final E node : nodes) {
      clearNodeChildrenCache(node, clearNodes);
    }
  }

  /**
   * cache the given node
   * 
   * @param node
   *          the node to cache
   */
  protected void cacheNode(final E node) {
    readWriteLock.writeLock().lock();
    nodeCache.put(node.getId(), node);
    readWriteLock.writeLock().unlock();
  }

  /**
   * cache the given nodes
   * 
   * @param nodes
   *          a list of all nodes to cache
   */
  protected void cacheNodes(final List<E> nodes) {
    readWriteLock.writeLock().lock();
    for (final E node : nodes) {
      nodeCache.put(node.getId(), node);
    }
    readWriteLock.writeLock().unlock();
  }

  /**
   * detect whether the children are already loaded or not
   * 
   * @param node
   *          the parent to check its children
   * @return true if all children has been loaded; false otherwise
   */
  protected boolean areChildrenLoaded(final E node) {
    readWriteLock.readLock().lock();
    final Boolean cached = nodeCached.get(node.getId());
    readWriteLock.readLock().unlock();
    return cached != null && cached;
  }

  /**
   * the the adjusting flag - like in the JTable. To inform listeners that there are ongoing actions
   * 
   * @return true/false
   */
  public boolean isAdjusting() {
    return isAdjusting;
  }

  /**
   * the the adjusting flag - like in the JTable. To inform listeners that there are ongoing actions
   * 
   * @param adjusting
   *          true if there is an ongoing work started, false otherwise
   */
  public void setAdjusting(boolean adjusting) {
    isAdjusting = adjusting;
  }

  private abstract class DelayedUpdateTask extends TimerTask {
    boolean updateNeeded = false;
  }
}
