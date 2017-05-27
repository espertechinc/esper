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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.supportregression.bean.SupportBean_S0;

import java.util.ArrayList;
import java.util.List;

public class SupportStaticMethodInvocations {
    private static List<String> invocations = new ArrayList<String>();

    public static int getInvocationSizeReset() {
        int size = invocations.size();
        invocations.clear();
        return size;
    }

    public static SupportBean_S0 fetchObjectLog(String fetchId, int passThroughNumber) {
        invocations.add(fetchId);
        return new SupportBean_S0(passThroughNumber, "|" + fetchId + "|");
    }
}
