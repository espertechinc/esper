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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSetterBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.namedwindow.compile.NamedWindowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.NameParamNumAndModule;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeRegistry;
import com.espertech.esper.common.internal.type.NameAndModule;

import java.util.Collection;
import java.util.HashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class ModuleDependenciesCompileTime {
    private final Collection<NameAndModule> pathEventTypes = new HashSet<>();
    private final Collection<NameAndModule> pathNamedWindows = new HashSet<>();
    private final Collection<NameAndModule> pathTables = new HashSet<>();
    private final Collection<NameAndModule> pathVariables = new HashSet<>();
    private final Collection<NameAndModule> pathContexts = new HashSet<>();
    private final Collection<NameAndModule> pathExpressions = new HashSet<>();
    private final Collection<ModuleIndexMeta> pathIndexes = new HashSet<>();
    private final Collection<NameParamNumAndModule> pathScripts = new HashSet<>();
    private final Collection<NameAndModule> pathClasses = new HashSet<>();
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

    public void addPathClass(String className, String moduleName) {
        pathClasses.add(new NameAndModule(className, moduleName));
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

    public Collection<String> getPublicEventTypes() {
        return publicEventTypes;
    }

    public Collection<NameAndModule> getPathEventTypes() {
        return pathEventTypes;
    }

    public void make(CodegenMethod method, CodegenClassScope classScope) {
        CodegenSetterBuilder builder = new CodegenSetterBuilder(ModuleDependenciesRuntime.EPTYPE, ModuleDependenciesCompileTime.class, "md", classScope, method);
        builder.expressionDefaultChecked("pathEventTypes", NameAndModule.makeArrayNullIfEmpty(pathEventTypes))
            .expressionDefaultChecked("pathNamedWindows", NameAndModule.makeArrayNullIfEmpty(pathNamedWindows))
            .expressionDefaultChecked("pathTables", NameAndModule.makeArrayNullIfEmpty(pathTables))
            .expressionDefaultChecked("pathVariables", NameAndModule.makeArrayNullIfEmpty(pathVariables))
            .expressionDefaultChecked("pathContexts", NameAndModule.makeArrayNullIfEmpty(pathContexts))
            .expressionDefaultChecked("pathExpressions", NameAndModule.makeArrayNullIfEmpty(pathExpressions))
            .expressionDefaultChecked("pathIndexes", ModuleIndexMeta.makeArrayNullIfEmpty(pathIndexes))
            .expressionDefaultChecked("pathScripts", NameParamNumAndModule.makeArrayNullIfEmpty(pathScripts))
            .expressionDefaultChecked("pathClasses", NameAndModule.makeArrayNullIfEmpty(pathClasses))
            .expressionDefaultChecked("publicEventTypes", makeStringArrayNullIfEmpty(publicEventTypes))
            .expressionDefaultChecked("publicVariables", makeStringArrayNullIfEmpty(publicVariables));
        method.getBlock().methodReturn(builder.getRefName());
    }

    private CodegenExpression makeStringArrayNullIfEmpty(Collection<String> values) {
        return values.isEmpty() ? constantNull() : constant(values.toArray(new String[0]));
    }
}
