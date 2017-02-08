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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.util.AuditCallback;
import com.espertech.esper.util.AuditContext;

import java.util.ArrayList;
import java.util.List;

public class SupportAuditCallback implements AuditCallback {
    private List<AuditContext> audits = new ArrayList<AuditContext>();

    public void audit(AuditContext auditContext) {
        audits.add(auditContext);
        System.out.println("Received one");
    }

    public List<AuditContext> getAudits() {
        return audits;
    }
}
