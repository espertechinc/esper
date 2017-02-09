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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.epl.join.table.EventTable;

import java.util.List;

public interface StatementAgentInstancePostLoadIndexVisitor {

    public void visit(EventTable[][] repositories);

    public void visit(List<EventTable> tables);

    public void visit(EventTable index);
}
