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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;

import java.util.LinkedHashMap;

public class FilterSpecCompilerArgs {

    public final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;
    public final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    public final StreamTypeService streamTypeService;
    public final ContextCompileTimeDescriptor contextDescriptor;
    public final StatementRawInfo statementRawInfo;
    public final StatementCompileTimeServices compileTimeServices;

    public FilterSpecCompilerArgs(LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, StreamTypeService streamTypeService, ContextCompileTimeDescriptor contextDescriptor, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        this.taggedEventTypes = taggedEventTypes;
        this.arrayEventTypes = arrayEventTypes;
        this.streamTypeService = streamTypeService;
        this.contextDescriptor = contextDescriptor;
        this.statementRawInfo = statementRawInfo;
        this.compileTimeServices = compileTimeServices;
    }
}
