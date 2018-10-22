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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanNodeForgeVisitor;
import com.espertech.esper.common.internal.util.IndentWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;

/**
 * Specification node for a query execution plan to be extended by specific execution specification nodes.
 */
public abstract class QueryPlanNodeForge implements CodegenMakeable<SAIFFInitializeSymbol> {

    public abstract void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes);

    public abstract void accept(QueryPlanNodeForgeVisitor visitor);

    /**
     * Print a long readable format of the query node to the supplied PrintWriter.
     *
     * @param writer is the indentation writer to print to
     */
    protected abstract void print(IndentWriter writer);

    /**
     * Print in readable format the execution plan spec.
     *
     * @param planNodeSpecs - plans to print
     * @return readable text with plans
     */
    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "StringContatenationInLoop"})
    public static String print(QueryPlanNodeForge[] planNodeSpecs) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("QueryPlanNode[]\n");

        for (int i = 0; i < planNodeSpecs.length; i++) {
            buffer.append("  node spec " + i + " :\n");

            StringWriter buf = new StringWriter();
            PrintWriter printer = new PrintWriter(buf);
            IndentWriter indentWriter = new IndentWriter(printer, 4, 2);

            if (planNodeSpecs[i] != null) {
                planNodeSpecs[i].print(indentWriter);
            } else {
                indentWriter.println("no plan (historical)");
            }

            buffer.append(buf.toString());
        }

        return buffer.toString();
    }
}
