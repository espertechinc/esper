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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

public class VariableDeployTimeResolver {

    public static CodegenExpressionField makeVariableField(VariableMetaData variableMetaData, CodegenClassScope classScope, Class generator) {
        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        CodegenMethod variableInit = classScope.getPackageScope().getInitMethod().makeChildWithScope(Variable.class, generator, symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        variableInit.getBlock().methodReturn(VariableDeployTimeResolver.makeResolveVariable(variableMetaData, EPStatementInitServices.REF));
        return classScope.getPackageScope().addFieldUnshared(true, Variable.class, localMethod(variableInit, EPStatementInitServices.REF));
    }

    public static CodegenExpression makeResolveVariable(VariableMetaData variable, CodegenExpression initSvc) {
        return staticMethod(VariableDeployTimeResolver.class, "resolveVariable",
                constant(variable.getVariableName()),
                constant(variable.getVariableVisibility()),
                constant(variable.getVariableModuleName()),
                initSvc);
    }

    public static CodegenExpression makeResolveVariables(Collection<VariableMetaData> variables, CodegenExpression initSvc) {
        CodegenExpression[] expressions = new CodegenExpression[variables.size()];
        int count = 0;
        for (VariableMetaData variable : variables) {
            expressions[count++] = makeResolveVariable(variable, initSvc);
        }
        return newArrayWithInit(Variable.class, expressions);
    }

    public static Variable resolveVariable(String variableName,
                                           NameAccessModifier visibility,
                                           String optionalModuleName,
                                           EPStatementInitServices services) {
        String deploymentId = resolveDeploymentId(variableName, visibility, optionalModuleName, services);
        Variable variable = services.getVariableManagementService().getVariableMetaData(deploymentId, variableName);
        if (variable == null) {
            throw new EPException("Failed to resolve variable '" + variableName + "'");
        }
        return variable;
    }

    public static VariableReader resolveVariableReader(String variableName,
                                                       NameAccessModifier visibility,
                                                       String optionalModuleName,
                                                       String optionalContextName,
                                                       EPStatementInitServices services) {
        if (optionalContextName != null) {
            throw new IllegalArgumentException("Expected null context name");
        }
        String deploymentId = resolveDeploymentId(variableName, visibility, optionalModuleName, services);
        VariableReader reader = services.getVariableManagementService().getReader(deploymentId, variableName, DEFAULT_AGENT_INSTANCE_ID);
        if (reader == null) {
            throw new EPException("Failed to resolve variable '" + variableName + "'");
        }
        return reader;
    }

    public static Map<Integer, VariableReader> resolveVariableReaderPerCP(String variableName,
                                                                          NameAccessModifier visibility,
                                                                          String optionalModuleName,
                                                                          String optionalContextName,
                                                                          EPStatementInitServices services) {
        if (optionalContextName == null) {
            throw new IllegalArgumentException("No context name");
        }
        String deploymentId = resolveDeploymentId(variableName, visibility, optionalModuleName, services);
        Map<Integer, VariableReader> reader = services.getVariableManagementService().getReadersPerCP(deploymentId, variableName);
        if (reader == null) {
            throw new EPException("Failed to resolve variable '" + variableName + "'");
        }
        return reader;
    }

    private static String resolveDeploymentId(String variableName,
                                              NameAccessModifier visibility,
                                              String optionalModuleName,
                                              EPStatementInitServices services) {
        String deploymentId;
        if (visibility == NameAccessModifier.PRECONFIGURED) {
            deploymentId = null;
        } else if (visibility == NameAccessModifier.PRIVATE) {
            deploymentId = services.getDeploymentId();
        } else if (visibility == NameAccessModifier.PUBLIC) {
            deploymentId = services.getVariablePathRegistry().getDeploymentId(variableName, optionalModuleName);
            if (deploymentId == null) {
                throw new EPException("Failed to resolve path variable '" + variableName + "'");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized visibility " + visibility);
        }
        return deploymentId;
    }
}
