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
package com.espertech.esper.epl.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.VirtualDataWindowLookupFieldDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableOrganization;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class VirtualDWEventTable implements EventTable {
    private final boolean unique;
    private final List<VirtualDataWindowLookupFieldDesc> hashAccess;
    private final List<VirtualDataWindowLookupFieldDesc> btreeAccess;
    private final EventTableOrganization organization;

    public VirtualDWEventTable(boolean unique, List<VirtualDataWindowLookupFieldDesc> hashAccess, List<VirtualDataWindowLookupFieldDesc> btreeAccess,
                               EventTableOrganization organization) {
        this.unique = unique;
        this.hashAccess = Collections.unmodifiableList(hashAccess);
        this.btreeAccess = Collections.unmodifiableList(btreeAccess);
        this.organization = organization;
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        add(newData, exprEvaluatorContext);
        remove(oldData, exprEvaluatorContext);
    }

    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void add(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void remove(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public Iterator<EventBean> iterator() {
        return Collections.<EventBean>emptyList().iterator();
    }

    public boolean isEmpty() {
        return true;
    }

    public void clear() {
    }

    public void destroy() {
    }

    public String toQueryPlan() {
        return "(external event table)";
    }

    public List<VirtualDataWindowLookupFieldDesc> getHashAccess() {
        return hashAccess;
    }

    public List<VirtualDataWindowLookupFieldDesc> getBtreeAccess() {
        return btreeAccess;
    }

    public boolean isUnique() {
        return unique;
    }

    public Integer getNumberOfEvents() {
        return null;
    }

    public int getNumKeys() {
        return 0;
    }

    public Object getIndex() {
        return null;
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }

    public Class getProviderClass() {
        return VirtualDWEventTable.class;
    }
}
