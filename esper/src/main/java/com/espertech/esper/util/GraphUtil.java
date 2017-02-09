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

import java.util.*;

/**
 * Utility for working with acyclic graph: determines cyclic dependency and dependency-satisfying processing order.
 */
public class GraphUtil {
    /**
     * Deep-merge a map into another map returning a result map.
     * <p>
     * Copies all values present in the original map to a new map,
     * adding additional value present in the second map passed in,
     * ignoring same-key values in the second map that are present in the original.
     * <p>
     * If the value is a Map itself, repeats the operation on the Map value.
     *
     * @param original   nestable Map of entries to retain and not overwrite
     * @param additional nestable Map of entries to add to the original
     * @return merge of original and additional nestable map
     */
    public static Map<String, Object> mergeNestableMap(Map<String, Object> original, Map<String, Object> additional) {
        Map<String, Object> result = new LinkedHashMap<String, Object>(original);

        for (Map.Entry<String, Object> additionalEntry : additional.entrySet()) {
            String name = additionalEntry.getKey();
            Object additionalValue = additionalEntry.getValue();

            Object originalValue = original.get(name);

            Object newValue;
            if ((originalValue instanceof Map) &&
                    (additionalValue instanceof Map)) {
                Map<String, Object> innerAdditional = (Map<String, Object>) additionalValue;
                Map<String, Object> innerOriginal = (Map<String, Object>) originalValue;
                newValue = mergeNestableMap(innerOriginal, innerAdditional);
                result.put(name, newValue);
                continue;
            }

            if (original.containsKey(name)) {
                continue;
            }
            result.put(name, additionalValue);
        }
        return result;
    }

    /**
     * Check cyclic dependency and determine processing order for the given graph.
     *
     * @param graph is represented as child nodes that have one or more parent nodes that they are dependent on
     * @return set of parent and child nodes in order such that no node's dependency is not satisfied
     * by a prior nodein the set
     * @throws GraphCircularDependencyException if a dependency has been detected
     */
    public static Set<String> getTopDownOrder(Map<String, Set<String>> graph) throws GraphCircularDependencyException {
        Stack<String> circularDependency = getFirstCircularDependency(graph);
        if (circularDependency != null) {
            throw new GraphCircularDependencyException("Circular dependency detected between " + circularDependency);
        }

        Map<String, Set<String>> reversedGraph = new HashMap<String, Set<String>>();

        // Reverse the graph - build a list of children per parent
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            Set<String> parents = entry.getValue();
            String child = entry.getKey();

            for (String parent : parents) {
                Set<String> childList = reversedGraph.get(parent);
                if (childList == null) {
                    childList = new LinkedHashSet<String>();
                    reversedGraph.put(parent, childList);
                }
                childList.add(child);
            }
        }

        // Determine all root nodes, which are those without parent
        TreeSet<String> roots = new TreeSet<String>();
        for (Set<String> parents : graph.values()) {
            if (parents == null) {
                continue;
            }
            for (String parent : parents) {
                // node not itself a child
                if (!graph.containsKey(parent)) {
                    roots.add(parent);
                }
            }
        }

        // for each root, recursively add its child nodes, this becomes the default order
        Set<String> graphFlattened = new LinkedHashSet<String>();
        for (String root : roots) {
            recusiveAdd(graphFlattened, root, reversedGraph);
        }

        // now walk down the default order and for each node ensure all parents are created
        Set<String> created = new LinkedHashSet<String>();
        Set<String> removeList = new HashSet<String>();
        while (!graphFlattened.isEmpty()) {
            removeList.clear();
            for (String node : graphFlattened) {
                if (!recursiveParentsCreated(node, created, graph)) {
                    continue;
                }
                created.add(node);
                removeList.add(node);
            }
            graphFlattened.removeAll(removeList);
        }

        return created;
    }

    // Determine if all the node's parents and their parents have been added to the created set
    private static boolean recursiveParentsCreated(String node, Set<String> created, Map<String, Set<String>> graph) {
        Set<String> parents = graph.get(node);
        if (parents == null) {
            return true;
        }
        for (String parent : parents) {
            if (!created.contains(parent)) {
                return false;
            }
            boolean allParentsCreated = recursiveParentsCreated(parent, created, graph);
            if (!allParentsCreated) {
                return false;
            }
        }
        return true;
    }

    private static void recusiveAdd(Set<String> graphFlattened, String root, Map<String, Set<String>> reversedGraph) {
        graphFlattened.add(root);
        Set<String> childNodes = reversedGraph.get(root);
        if (childNodes == null) {
            return;
        }
        for (String child : childNodes) {
            recusiveAdd(graphFlattened, child, reversedGraph);
        }
    }

    /**
     * Returns any circular dependency as a stack of stream numbers, or null if none exist.
     *
     * @param graph the dependency graph
     * @return circular dependency stack
     */
    private static Stack<String> getFirstCircularDependency(Map<String, Set<String>> graph) {
        for (String child : graph.keySet()) {
            Stack<String> deepDependencies = new Stack<String>();
            deepDependencies.push(child);

            boolean isCircular = recursiveDeepDepends(deepDependencies, child, graph);
            if (isCircular) {
                return deepDependencies;
            }
        }
        return null;
    }

    private static boolean recursiveDeepDepends(Stack<String> deepDependencies, String currentChild, Map<String, Set<String>> graph) {
        Set<String> required = graph.get(currentChild);
        if (required == null) {
            return false;
        }

        for (String parent : required) {
            if (deepDependencies.contains(parent)) {
                return true;
            }
            deepDependencies.push(parent);
            boolean isDeep = recursiveDeepDepends(deepDependencies, parent, graph);
            if (isDeep) {
                return true;
            }
            deepDependencies.pop();
        }

        return false;
    }
}
