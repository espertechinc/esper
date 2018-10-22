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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

public interface TableCompileTimeResolver extends CompileTimeResolver {
    TableMetaData resolve(String tableName);

    TableMetaData resolveTableFromEventType(EventType containedType);
}
