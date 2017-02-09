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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.IndentWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

/**
 * Interface for an execution node that looks up events and builds a result set contributing to an overall
 * join result set.
 */
public abstract class ExecNode {
    /**
     * Process single event using the prefill events to compile lookup results.
     *
     * @param lookupEvent          - event to look up for or query for
     * @param prefillPath          - set of events currently in the example tuple to serve
     *                             as a prototype for result rows.
     * @param result               is the list of tuples to add a result row to
     * @param exprEvaluatorContext context for expression evaluation
     */
    public abstract void process(EventBean lookupEvent, EventBean[] prefillPath, Collection<EventBean[]> result, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Output the execution strategy.
     *
     * @param writer to output to
     */
    public abstract void print(IndentWriter writer);

    /**
     * Print in readable format the execution strategy.
     *
     * @param execNode - execution node to print
     * @return readable text with execution nodes constructed for actual streams
     */
    public static String print(ExecNode execNode) {
        StringWriter buf = new StringWriter();
        PrintWriter printer = new PrintWriter(buf);
        IndentWriter indentWriter = new IndentWriter(printer, 4, 2);
        execNode.print(indentWriter);

        return buf.toString();
    }


}
