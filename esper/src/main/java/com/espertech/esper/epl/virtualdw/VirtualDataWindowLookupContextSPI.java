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

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.VirtualDataWindowLookupContext;
import com.espertech.esper.client.hook.VirtualDataWindowLookupFieldDesc;
import com.espertech.esper.epl.lookup.SubordPropPlan;

import java.lang.annotation.Annotation;
import java.util.List;

public class VirtualDataWindowLookupContextSPI extends VirtualDataWindowLookupContext {
    private SubordPropPlan joinDesc;
    private boolean forceTableScan;
    private EventType[] outerTypePerStream;
    private String accessedByStatementName;
    private int accessedByStatementSequenceNum;

    public VirtualDataWindowLookupContextSPI(String statementName, int statementId, Annotation[] statementAnnotations, boolean fireAndForget, String namedWindowName, List<VirtualDataWindowLookupFieldDesc> hashFields, List<VirtualDataWindowLookupFieldDesc> btreeFields, SubordPropPlan joinDesc, boolean forceTableScan, EventType[] outerTypePerStream, String accessedByStatementName, int accessedByStatementSequenceNum) {
        super(statementName, statementId, statementAnnotations, fireAndForget, namedWindowName, hashFields, btreeFields);
        this.joinDesc = joinDesc;
        this.forceTableScan = forceTableScan;
        this.outerTypePerStream = outerTypePerStream;
        this.accessedByStatementName = accessedByStatementName;
        this.accessedByStatementSequenceNum = accessedByStatementSequenceNum;
    }

    public SubordPropPlan getJoinDesc() {
        return joinDesc;
    }

    public boolean isForceTableScan() {
        return forceTableScan;
    }

    public EventType[] getOuterTypePerStream() {
        return outerTypePerStream;
    }

    public String getAccessedByStatementName() {
        return accessedByStatementName;
    }

    public int getAccessedByStatementSequenceNum() {
        return accessedByStatementSequenceNum;
    }
}
