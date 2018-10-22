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
package com.espertech.esper.common.client.module;

import com.espertech.esper.common.internal.util.DependencyGraph;

import java.util.*;

/**
 * Module ordering utility.
 */
public class ModuleOrderUtil {
    /**
     * Compute a deployment order among the modules passed in considering their uses-dependency declarations.
     * <p>
     * The operation also checks and reports circular dependencies.
     * <p>
     * Pass in @{link ModuleOrderOptions} to customize the behavior if this method. When passing no options
     * or passing default options, the default behavior checks uses-dependencies and circular dependencies.
     *
     * @param modules         to determine ordering for
     * @param options         operation options or null for default options
     * @param deployedModules deployed modules
     * @return ordered modules
     * @throws ModuleOrderException when any module dependencies are not satisfied
     */
    public static ModuleOrder getModuleOrder(Collection<Module> modules, Set<String> deployedModules, ModuleOrderOptions options) throws ModuleOrderException {
        if (options == null) {
            options = new ModuleOrderOptions();
        }

        List<Module> proposedModules = new ArrayList<Module>();
        proposedModules.addAll(modules);

        Set<String> availableModuleNames = new HashSet<String>();
        for (Module proposedModule : proposedModules) {
            if (proposedModule.getName() != null) {
                availableModuleNames.add(proposedModule.getName());
            }
        }

        // Collect all deployed modules
        Set<String> allDeployedModules = new HashSet<>();
        allDeployedModules.addAll(deployedModules);
        for (Module proposedModule : proposedModules) {
            allDeployedModules.add(proposedModule.getName());
        }

        // Collect uses-dependencies of proposed modules
        Map<String, Set<String>> usesPerModuleName = new HashMap<String, Set<String>>();
        for (Module proposedModule : proposedModules) {

            // check uses-dependency is available
            if (options.isCheckUses()) {
                if (proposedModule.getUses() != null) {
                    for (String uses : proposedModule.getUses()) {
                        if (availableModuleNames.contains(uses)) {
                            continue;
                        }
                        boolean deployed = allDeployedModules.contains(uses);
                        if (deployed) {
                            continue;
                        }
                        String message = "Module-dependency not found";
                        if (proposedModule.getName() != null) {
                            message += " as declared by module '" + proposedModule.getName() + "'";
                        }
                        message += " for uses-declaration '" + uses + "'";
                        throw new ModuleOrderException(message);
                    }
                }
            }

            if ((proposedModule.getName() == null) || (proposedModule.getUses() == null)) {
                continue;
            }
            Set<String> usesSet = usesPerModuleName.get(proposedModule.getName());
            if (usesSet == null) {
                usesSet = new HashSet<String>();
                usesPerModuleName.put(proposedModule.getName(), usesSet);
            }
            usesSet.addAll(proposedModule.getUses());
        }

        Map<String, SortedSet<Integer>> proposedModuleNames = new HashMap<String, SortedSet<Integer>>();
        int count = 0;
        for (Module proposedModule : proposedModules) {
            SortedSet<Integer> moduleNumbers = proposedModuleNames.get(proposedModule.getName());
            if (moduleNumbers == null) {
                moduleNumbers = new TreeSet<Integer>();
                proposedModuleNames.put(proposedModule.getName(), moduleNumbers);
            }
            moduleNumbers.add(count);
            count++;
        }

        DependencyGraph graph = new DependencyGraph(proposedModules.size(), false);
        int fromModule = 0;
        for (Module proposedModule : proposedModules) {
            if ((proposedModule.getUses() == null) || (proposedModule.getUses().isEmpty())) {
                fromModule++;
                continue;
            }
            SortedSet<Integer> dependentModuleNumbers = new TreeSet<Integer>();
            for (String use : proposedModule.getUses()) {
                SortedSet<Integer> moduleNumbers = proposedModuleNames.get(use);
                if (moduleNumbers == null) {
                    continue;
                }
                dependentModuleNumbers.addAll(moduleNumbers);
            }
            dependentModuleNumbers.remove(fromModule);
            graph.addDependency(fromModule, dependentModuleNumbers);
            fromModule++;
        }

        if (options.isCheckCircularDependency()) {
            Stack<Integer> circular = graph.getFirstCircularDependency();
            if (circular != null) {
                String message = "";
                String delimiter = "";
                for (int i : circular) {
                    message += delimiter;
                    message += "module '" + proposedModules.get(i).getName() + "'";
                    delimiter = " uses (depends on) ";
                }
                throw new ModuleOrderException("Circular dependency detected in module uses-relationships: " + message);
            }
        }

        List<Module> reverseDeployList = new ArrayList<Module>();
        Set<Integer> ignoreList = new HashSet<Integer>();
        while (ignoreList.size() < proposedModules.size()) {

            // seconardy sort according to the order of listing
            Set<Integer> rootNodes = new TreeSet<Integer>(new Comparator<Integer>() {
                public int compare(Integer o1, Integer o2) {
                    return -1 * o1.compareTo(o2);
                }
            });
            rootNodes.addAll(graph.getRootNodes(ignoreList));

            if (rootNodes.isEmpty()) {   // circular dependency could cause this
                for (int i = 0; i < proposedModules.size(); i++) {
                    if (!ignoreList.contains(i)) {
                        rootNodes.add(i);
                        break;
                    }
                }
            }

            for (Integer root : rootNodes) {
                ignoreList.add(root);
                reverseDeployList.add(proposedModules.get(root));
            }
        }

        Collections.reverse(reverseDeployList);
        return new ModuleOrder(reverseDeployList);
    }
}
