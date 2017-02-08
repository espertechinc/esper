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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.filter.FilterSpecCompiled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContextDetailInitiatedTerminated implements ContextDetail {

    private static final long serialVersionUID = 800736876398383226L;
    private ContextDetailCondition start;
    private ContextDetailCondition end;
    private boolean overlapping;
    private ExprNode[] distinctExpressions;

    public ContextDetailInitiatedTerminated(ContextDetailCondition start, ContextDetailCondition end, boolean overlapping, ExprNode[] distinctExpressions) {
        this.start = start;
        this.end = end;
        this.overlapping = overlapping;
        this.distinctExpressions = distinctExpressions;
    }

    public ContextDetailCondition getStart() {
        return start;
    }

    public ContextDetailCondition getEnd() {
        return end;
    }

    public void setStart(ContextDetailCondition start) {
        this.start = start;
    }

    public void setEnd(ContextDetailCondition end) {
        this.end = end;
    }

    public boolean isOverlapping() {
        return overlapping;
    }

    public List<FilterSpecCompiled> getContextDetailFilterSpecs() {
        List<FilterSpecCompiled> startFS = start.getFilterSpecIfAny();
        List<FilterSpecCompiled> endFS = end.getFilterSpecIfAny();
        if (startFS == null && endFS == null) {
            return Collections.emptyList();
        }
        List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>(2);
        if (startFS != null) {
            filters.addAll(startFS);
        }
        if (endFS != null) {
            filters.addAll(endFS);
        }
        return filters;
    }

    public ExprNode[] getDistinctExpressions() {
        return distinctExpressions;
    }
}
