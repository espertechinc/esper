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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

public interface EPServicesPath {
    PathRegistry<String, NamedWindowMetaData> getNamedWindowPathRegistry();
    PathRegistry<String, ContextMetaData> getContextPathRegistry();
    PathRegistry<String, ExpressionDeclItem> getExprDeclaredPathRegistry();
    PathRegistry<String, EventType> getEventTypePathRegistry();
    PathRegistry<NameAndParamNum, ExpressionScriptProvided> getScriptPathRegistry();
    PathRegistry<String, TableMetaData> getTablePathRegistry();
    PathRegistry<String, VariableMetaData> getVariablePathRegistry();
}
