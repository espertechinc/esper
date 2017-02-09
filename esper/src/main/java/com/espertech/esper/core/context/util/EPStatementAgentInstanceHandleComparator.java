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
package com.espertech.esper.core.context.util;

import java.io.Serializable;
import java.util.Comparator;

public class EPStatementAgentInstanceHandleComparator implements Comparator<EPStatementAgentInstanceHandle>, Serializable {

    private static final long serialVersionUID = 8926266145763075051L;

    public final static EPStatementAgentInstanceHandleComparator INSTANCE = new EPStatementAgentInstanceHandleComparator();

    public int compare(EPStatementAgentInstanceHandle o1, EPStatementAgentInstanceHandle o2) {
        if (o1.getPriority() == o2.getPriority()) {
            if (o1 == o2 || o1.equals(o2)) {
                return 0;
            }
            if (o1.getStatementId() != o2.getStatementId()) {
                return compare(o1.getStatementId(), o2.getStatementId());
            }
            return o1.getAgentInstanceId() < o2.getAgentInstanceId() ? -1 : 1;
        } else {
            return o1.getPriority() > o2.getPriority() ? -1 : 1;
        }
    }

    public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
