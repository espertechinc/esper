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
package com.espertech.esper.common.internal.epl.subselect;

public class SubselectUtil {
    public static String getStreamName(String optionalStreamName, int subselectNumber) {
        String subexpressionStreamName = optionalStreamName;
        if (subexpressionStreamName == null) {
            subexpressionStreamName = "$subselect_" + subselectNumber;
        }
        return subexpressionStreamName;
    }
}
