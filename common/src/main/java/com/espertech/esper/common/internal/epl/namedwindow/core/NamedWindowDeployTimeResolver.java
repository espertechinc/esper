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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class NamedWindowDeployTimeResolver {
    public static CodegenExpression makeResolveNamedWindow(NamedWindowMetaData namedWindow, CodegenExpression initSvc) {
        return staticMethod(NamedWindowDeployTimeResolver.class, "resolveNamedWindow",
                constant(namedWindow.getEventType().getName()),
                constant(namedWindow.getEventType().getMetadata().getAccessModifier()),
                constant(namedWindow.getNamedWindowModuleName()),
                initSvc);
    }

    public static NamedWindow resolveNamedWindow(String namedWindowName,
                                                 NameAccessModifier visibility,
                                                 String optionalModuleName,
                                                 EPStatementInitServices services) {
        String deploymentId = resolveDeploymentId(namedWindowName, visibility, optionalModuleName, services);
        NamedWindow namedWindow = services.getNamedWindowManagementService().getNamedWindow(deploymentId, namedWindowName);
        if (namedWindow == null) {
            throw new EPException("Failed to resolve named window '" + namedWindowName + "'");
        }
        return namedWindow;
    }

    private static String resolveDeploymentId(String tableName,
                                              NameAccessModifier visibility,
                                              String optionalModuleName,
                                              EPStatementInitServices services) {
        String deploymentId;
        if (visibility == NameAccessModifier.PRIVATE) {
            deploymentId = services.getDeploymentId();
        } else if (visibility == NameAccessModifier.PUBLIC || visibility == NameAccessModifier.PROTECTED) {
            deploymentId = services.getNamedWindowPathRegistry().getDeploymentId(tableName, optionalModuleName);
            if (deploymentId == null) {
                throw new EPException("Failed to resolve path named window '" + tableName + "'");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized visibility " + visibility);
        }
        return deploymentId;
    }
}
