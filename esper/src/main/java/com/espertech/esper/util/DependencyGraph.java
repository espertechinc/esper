/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Model of dependency of lookup, in which one stream supplies values for lookup in another stream.
 */
public class DependencyGraph {
    private final int numStreams;
    private final boolean allowDependencySame;
    private final Map<Integer, SortedSet<Integer>> dependencies;

    /**
     * Ctor.
     *
     * @param numStreams          - number of streams
     * @param allowDependencySame - allow same-dependency stream
     */
    public DependencyGraph(int numStreams, boolean allowDependencySame) {
        this.numStreams = numStreams;
        this.allowDependencySame = allowDependencySame;
        dependencies = new HashMap<Integer, SortedSet<Integer>>();
    }

    /**
     * Returns the number of streams.
     *
     * @return number of streams
     */
    public int getNumStreams() {
        return numStreams;
    }

    public String toString() {
        StringWriter buf = new StringWriter();
        PrintWriter writer = new PrintWriter(buf);

        int count = 0;
        for (Map.Entry<Integer, SortedSet<Integer>> entry : dependencies.entrySet()) {
            count++;
            writer.println("Entry " + count + ": from=" + entry.getKey() + " to=" + entry.getValue());
        }

        return buf.toString();
    }

    /**
     * Adds dependencies that a target may have on required streams.
     *
     * @param target          the stream having dependencies on one or more other streams
     * @param requiredStreams the streams that the target stream has a dependency on
     */
    public void addDependency(int target, SortedSet<Integer> requiredStreams) {
        if (requiredStreams.contains(target)) {
            throw new IllegalArgumentException("Dependency between same streams is not allowed for stream " + target);
        }

        Set<Integer> toSet = dependencies.get(target);
        if (toSet != null) {
            throw new IllegalArgumentException("Dependencies from stream " + target + " already in collection");
        }

        dependencies.put(target, requiredStreams);
    }

    /**
     * Adds a single dependency of target on a required streams.
     *
     * @param target the stream having dependencies on one or more other streams
     * @param from   a single required streams that the target stream has a dependency on
     */
    public void addDependency(int target, int from) {
        if (target == from && !allowDependencySame) {
            throw new IllegalArgumentException("Dependency between same streams is not allowed for stream " + target);
        }

        SortedSet<Integer> toSet = dependencies.get(target);
        if (toSet == null) {
            toSet = new TreeSet<Integer>();
            dependencies.put(target, toSet);
        }

        toSet.add(from);
    }

    /**
     * Returns true if the stream asked for has a dependency.
     *
     * @param stream to check dependency for
     * @return true if a dependency exist, false if not
     */
    public boolean hasDependency(int stream) {
        SortedSet<Integer> dep = dependencies.get(stream);
        if (dep != null) {
            return !dep.isEmpty();
        }
        return false;
    }

    /**
     * Returns the set of dependent streams for a given stream.
     *
     * @param stream to return dependent streams for
     * @return set of stream numbers of stream providing properties
     */
    public Set<Integer> getDependenciesForStream(int stream) {
        SortedSet<Integer> dep = dependencies.get(stream);
        if (dep != null) {
            return dep;
        }
        return Collections.emptySet();
    }

    /**
     * Returns a map of stream number and the streams dependencies.
     *
     * @return map of dependencies
     */
    public Map<Integer, SortedSet<Integer>> getDependencies() {
        return dependencies;
    }

    /**
     * Returns a set of stream numbers that are the root dependencies, i.e. the dependencies with the deepest graph.
     *
     * @return set of stream number of streams
     */
    public Set<Integer> getRootNodes() {
        Set<Integer> rootNodes = new HashSet<Integer>();

        for (int i = 0; i < numStreams; i++) {
            boolean found = false;
            for (Map.Entry<Integer, SortedSet<Integer>> entry : dependencies.entrySet()) {
                if (entry.getValue().contains(i)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                rootNodes.add(i);
            }
        }

        return rootNodes;
    }

    /**
     * Return the root nodes ignoring the nodes provided.
     *
     * @param ignoreList nodes to be ignored
     * @return root nodes
     */
    public Set<Integer> getRootNodes(Set<Integer> ignoreList) {
        Set<Integer> rootNodes = new HashSet<Integer>();

        for (int i = 0; i < numStreams; i++) {
            if (ignoreList.contains(i)) {
                continue;
            }
            boolean found = false;
            for (Map.Entry<Integer, SortedSet<Integer>> entry : dependencies.entrySet()) {
                if (entry.getValue().contains(i)) {
                    if (!ignoreList.contains(entry.getKey())) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                rootNodes.add(i);
            }
        }

        return rootNodes;
    }

    /**
     * Returns any circular dependency as a stack of stream numbers, or null if none exist.
     *
     * @return circular dependency stack
     */
    public Stack<Integer> getFirstCircularDependency() {
        for (int i = 0; i < numStreams; i++) {
            Stack<Integer> deepDependencies = new Stack<Integer>();
            deepDependencies.push(i);

            boolean isCircular = recursiveDeepDepends(deepDependencies, i);
            if (isCircular) {
                return deepDependencies;
            }
        }
        return null;
    }

    private boolean recursiveDeepDepends(Stack<Integer> deepDependencies, int currentStream) {
        Set<Integer> required = dependencies.get(currentStream);
        if (required == null) {
            return false;
        }

        for (Integer stream : required) {
            if (deepDependencies.contains(stream)) {
                return true;
            }
            deepDependencies.push(stream);
            boolean isDeep = recursiveDeepDepends(deepDependencies, stream);
            if (isDeep) {
                return true;
            }
            deepDependencies.pop();
        }

        return false;
    }

    /**
     * Check if the given stream has any dependencies, direct or indirect, to any of the streams that are not in the ignore list.
     *
     * @param ignoreList      ignore list
     * @param navigableStream to-stream
     * @return indicator whether there is an unsatisfied dependency
     */
    public boolean hasUnsatisfiedDependency(int navigableStream, Set<Integer> ignoreList) {
        Set<Integer> deepDependencies = new HashSet<Integer>();
        recursivePopulateDependencies(navigableStream, deepDependencies);

        for (int dependency : deepDependencies) {
            if (!ignoreList.contains(dependency)) {
                return true;
            }
        }
        return false;
    }

    private void recursivePopulateDependencies(int navigableStream, Set<Integer> deepDependencies) {
        Set<Integer> dependencies = getDependenciesForStream(navigableStream);
        deepDependencies.addAll(dependencies);
        for (int dependency : dependencies) {
            recursivePopulateDependencies(dependency, deepDependencies);
        }
    }
}
