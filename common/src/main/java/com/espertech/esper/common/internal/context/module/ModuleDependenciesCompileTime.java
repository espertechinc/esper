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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.namedwindow.compile.NamedWindowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeRegistry;
import com.espertech.esper.common.internal.type.NameAndModule;

import java.util.Collection;
import java.util.HashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ModuleDependenciesCompileTime {
    private final Collection<NameAndModule> pathEventTypes = new HashSet<>();
    private final Collection<NameAndModule> pathNamedWindows = new HashSet<>();
    private final Collection<NameAndModule> pathTables = new HashSet<>();
    private final Collection<NameAndModule> pathVariables = new HashSet<>();
    private final Collection<NameAndModule> pathContexts = new HashSet<>();
    private final Collection<NameAndModule> pathExpressions = new HashSet<>();
    private final Collection<ModuleIndexMeta> pathIndexes = new HashSet<>();
    private final Collection<NameParamNumAndModule> pathScripts = new HashSet<>();
    private final Collection<String> publicEventTypes = new HashSet<>();
    private final Collection<String> publicVariables = new HashSet<>();

    public void addPathEventType(String eventTypeName, String moduleName) {
        pathEventTypes.add(new NameAndModule(eventTypeName, moduleName));
    }

    public void addPathNamedWindow(String namedWindowName, String moduleName) {
        pathNamedWindows.add(new NameAndModule(namedWindowName, moduleName));
    }

    public void addPathTable(String tableName, String moduleName) {
        pathTables.add(new NameAndModule(tableName, moduleName));
    }

    public void addPathVariable(String variableName, String moduleName) {
        pathVariables.add(new NameAndModule(variableName, moduleName));
    }

    public void addPathContext(String contextName, String moduleName) {
        pathContexts.add(new NameAndModule(contextName, moduleName));
    }

    public void addPathExpression(String expressionName, String moduleName) {
        pathExpressions.add(new NameAndModule(expressionName, moduleName));
    }

    public void addPathScript(NameAndParamNum key, String moduleName) {
        pathScripts.add(new NameParamNumAndModule(key.getName(), key.getParamNum(), moduleName));
    }

    public void addPublicEventType(String eventTypeName) {
        publicEventTypes.add(eventTypeName);
    }

    public void addPublicVariable(String variableName) {
        publicVariables.add(variableName);
    }

    public void addPathIndex(boolean namedWindow, String infraName, String infraModuleName, String indexName, String indexModuleName, NamedWindowCompileTimeRegistry namedWindowCompileTimeRegistry, TableCompileTimeRegistry tableCompileTimeRegistry) {
        if (indexName == null) { // ignore unnamed non-explicit indexes
            return;
        }
        if (!namedWindow && infraName.equals(indexName)) {
            return; // not tracking primary key index as a dependency
        }
        if (namedWindow && namedWindowCompileTimeRegistry.getNamedWindows().get(infraName) != null) {
            return; // ignore when the named window was registered in the same EPL
        }
        if (!namedWindow && tableCompileTimeRegistry.getTables().get(infraName) != null) {
            return; // ignore when the table was registered in the same EPL
        }
        pathIndexes.add(new ModuleIndexMeta(namedWindow, infraName, infraModuleName, indexName, indexModuleName));
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ModuleDependenciesRuntime.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ModuleDependenciesRuntime.class, "md", newInstance(ModuleDependenciesRuntime.class))
                .exprDotMethod(ref("md"), "setPathEventTypes", NameAndModule.makeArray(pathEventTypes))
                .exprDotMethod(ref("md"), "setPathNamedWindows", NameAndModule.makeArray(pathNamedWindows))
                .exprDotMethod(ref("md"), "setPathTables", NameAndModule.makeArray(pathTables))
                .exprDotMethod(ref("md"), "setPathVariables", NameAndModule.makeArray(pathVariables))
                .exprDotMethod(ref("md"), "setPathContexts", NameAndModule.makeArray(pathContexts))
                .exprDotMethod(ref("md"), "setPathExpressions", NameAndModule.makeArray(pathExpressions))
                .exprDotMethod(ref("md"), "setPathIndexes", ModuleIndexMeta.makeArray(pathIndexes))
                .exprDotMethod(ref("md"), "setPathScripts", NameParamNumAndModule.makeArray(pathScripts))
                .exprDotMethod(ref("md"), "setPublicEventTypes", constant(publicEventTypes.toArray(new String[publicEventTypes.size()])))
                .exprDotMethod(ref("md"), "setPublicVariables", constant(publicVariables.toArray(new String[publicVariables.size()])))
                .methodReturn(ref("md"));
        return localMethod(method);
    }
}
