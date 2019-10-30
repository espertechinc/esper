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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;

public interface ExprEnumerationForgeProvider {
    /**
     * Returns the enumeration forge provider, or null if not applicable
     * @return forge provider
     * @param streamTypeService stream type service
     * @param contextDescriptor context descriptor
     */
    ExprEnumerationForgeDesc getEnumerationForge(StreamTypeService streamTypeService, ContextCompileTimeDescriptor contextDescriptor);
}
