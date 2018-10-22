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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowLookupContext;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowLookupFieldDesc;

import java.lang.annotation.Annotation;
import java.util.List;

public class VirtualDataWindowLookupContextSPI extends VirtualDataWindowLookupContext {
    private int accessedByStatementSequenceNum;

    public VirtualDataWindowLookupContextSPI(String deploymentId, String statementName, int statementId, Annotation[] statementAnnotations, boolean isFireAndForget, String namedWindowName, List<VirtualDataWindowLookupFieldDesc> hashFields, List<VirtualDataWindowLookupFieldDesc> btreeFields, int accessedByStatementSequenceNum) {
        super(deploymentId, statementName, statementId, statementAnnotations, isFireAndForget, namedWindowName, hashFields, btreeFields);
        this.accessedByStatementSequenceNum = accessedByStatementSequenceNum;
    }

    public int getAccessedByStatementSequenceNum() {
        return accessedByStatementSequenceNum;
    }
}
