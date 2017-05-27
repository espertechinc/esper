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
package com.espertech.esper.supportregression.subscriber;

import com.espertech.esper.client.EPStatement;

import java.util.ArrayList;

public class SupportSubscriberRowByRowStaticWStatement {
    private static ArrayList<Object[]> indicate = new ArrayList<Object[]>();
    private static ArrayList<EPStatement> statements = new ArrayList<EPStatement>();

    public static void update(EPStatement statement, String theString, int intPrimitive) {
        indicate.add(new Object[]{theString, intPrimitive});
        statements.add(statement);
    }

    public static ArrayList<Object[]> getIndicate() {
        return indicate;
    }

    public static ArrayList<EPStatement> getStatements() {
        return statements;
    }

    public void reset() {
        indicate.clear();
        statements.clear();
    }
}
