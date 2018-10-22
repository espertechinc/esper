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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.type.NameAndModule;

public class ModuleDependenciesRuntime {
    private NameAndModule[] pathEventTypes;
    private NameAndModule[] pathNamedWindows;
    private NameAndModule[] pathTables;
    private NameAndModule[] pathVariables;
    private NameAndModule[] pathContexts;
    private NameAndModule[] pathExpressions;
    private ModuleIndexMeta[] pathIndexes;
    private NameParamNumAndModule[] pathScripts;
    private String[] publicEventTypes;
    private String[] publicVariables;

    public NameAndModule[] getPathEventTypes() {
        return pathEventTypes;
    }

    public void setPathEventTypes(NameAndModule[] pathEventTypes) {
        this.pathEventTypes = pathEventTypes;
    }

    public NameAndModule[] getPathNamedWindows() {
        return pathNamedWindows;
    }

    public void setPathNamedWindows(NameAndModule[] pathNamedWindows) {
        this.pathNamedWindows = pathNamedWindows;
    }

    public NameAndModule[] getPathTables() {
        return pathTables;
    }

    public void setPathTables(NameAndModule[] pathTables) {
        this.pathTables = pathTables;
    }

    public NameAndModule[] getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(NameAndModule[] pathVariables) {
        this.pathVariables = pathVariables;
    }

    public NameAndModule[] getPathContexts() {
        return pathContexts;
    }

    public void setPathContexts(NameAndModule[] pathContexts) {
        this.pathContexts = pathContexts;
    }

    public NameAndModule[] getPathExpressions() {
        return pathExpressions;
    }

    public void setPathExpressions(NameAndModule[] pathExpressions) {
        this.pathExpressions = pathExpressions;
    }

    public String[] getPublicEventTypes() {
        return publicEventTypes;
    }

    public void setPublicEventTypes(String[] publicEventTypes) {
        this.publicEventTypes = publicEventTypes;
    }

    public String[] getPublicVariables() {
        return publicVariables;
    }

    public void setPublicVariables(String[] publicVariables) {
        this.publicVariables = publicVariables;
    }

    public ModuleIndexMeta[] getPathIndexes() {
        return pathIndexes;
    }

    public void setPathIndexes(ModuleIndexMeta[] pathIndexes) {
        this.pathIndexes = pathIndexes;
    }

    public NameParamNumAndModule[] getPathScripts() {
        return pathScripts;
    }

    public void setPathScripts(NameParamNumAndModule[] pathScripts) {
        this.pathScripts = pathScripts;
    }
}
