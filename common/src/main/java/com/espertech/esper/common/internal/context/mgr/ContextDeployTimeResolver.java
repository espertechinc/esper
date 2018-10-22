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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.util.StringValue;

public class ContextDeployTimeResolver {
    public static String resolveContextDeploymentId(String contextModuleName, NameAccessModifier contextVisibility, String contextName, String myDeploymentId, PathRegistry<String, ContextMetaData> pathContextRegistry) {
        boolean protectedVisibility = contextVisibility == NameAccessModifier.PRIVATE;
        String contextDeploymentId;
        if (protectedVisibility) {
            contextDeploymentId = myDeploymentId;
        } else {
            contextDeploymentId = pathContextRegistry.getDeploymentId(contextName, contextModuleName);
        }
        if (contextDeploymentId == null) {
            throw failedToFind(contextModuleName, contextVisibility, contextName);
        }
        return contextDeploymentId;
    }

    public static EPException failedToFind(String contextModuleName, NameAccessModifier visibility, String contextName) {
        boolean protectedVisibility = visibility == NameAccessModifier.PRIVATE;
        String message = "Failed find to context '" + contextName + "'";
        if (!protectedVisibility) {
            message += " module name '" + StringValue.unnamedWhenNullOrEmpty(contextModuleName) + "'";
        }
        return new EPException(message);
    }
}
