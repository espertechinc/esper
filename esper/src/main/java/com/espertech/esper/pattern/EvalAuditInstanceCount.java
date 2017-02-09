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
package com.espertech.esper.pattern;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.util.AuditPath;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class EvalAuditInstanceCount {

    private final Map<EvalFactoryNode, Integer> counts;

    public EvalAuditInstanceCount() {
        counts = new HashMap<EvalFactoryNode, Integer>();
    }

    public void decreaseRefCount(EvalFactoryNode evalNode, EvalAuditStateNode current, String patternExpr, String statementName, String engineURI) {
        Integer count = counts.get(evalNode);
        if (count == null) {
            return;
        }
        count--;
        if (count <= 0) {
            counts.remove(evalNode);
            print(current, patternExpr, engineURI, statementName, false, 0);
            return;
        }
        counts.put(evalNode, count);
        print(current, patternExpr, engineURI, statementName, false, count);


    }

    public void increaseRefCount(EvalFactoryNode evalNode, EvalAuditStateNode current, String patternExpr, String statementName, String engineURI) {
        Integer count = counts.get(evalNode);
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        counts.put(evalNode, count);
        print(current, patternExpr, engineURI, statementName, true, count);
    }

    private static void print(EvalAuditStateNode current, String patternExpression, String engineURI, String statementName, boolean added, int count) {
        if (!AuditPath.isInfoEnabled()) {
            return;
        }

        StringWriter writer = new StringWriter();
        EvalAuditStateNode.writePatternExpr(current, patternExpression, writer);

        if (added) {
            writer.write(" increased to " + count);
        } else {
            writer.write(" decreased to " + count);
        }

        AuditPath.auditLog(engineURI, statementName, AuditEnum.PATTERNINSTANCES, writer.toString());
    }
}
