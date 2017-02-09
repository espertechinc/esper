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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

@SuppressWarnings({"ALL"})
public class EPStatementFormatter {
    private final static String SYSTEM_NEWLINE = System.getProperty("line.separator");
    private final static String SPACE = " ";

    private final boolean isNewline;
    private final String newlineString;

    private String delimiter;

    public EPStatementFormatter() {
        isNewline = false;
        newlineString = " ";
    }

    public EPStatementFormatter(boolean newline) {
        isNewline = newline;
        this.newlineString = SYSTEM_NEWLINE;
    }

    public EPStatementFormatter(String newlineString) {
        isNewline = true;
        this.newlineString = newlineString;
    }

    public void beginContext(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginAnnotation(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginExpressionDecl(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginInsertInto(StringWriter writer, boolean topLevel) {
        writeDelimiter(writer, topLevel);
    }

    public void beginFromStream(StringWriter writer, boolean first) {
        writeDelimiter(writer, !first);
    }

    public void beginWhere(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginGroupBy(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginHaving(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginOutput(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginOrderBy(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginLimit(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginFor(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginOnTrigger(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginSelect(StringWriter writer, boolean topLevel) {
        if (topLevel) {
            writeDelimiter(writer, topLevel);
        }
        setDelimiter();
    }

    public void beginMerge(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginFrom(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginMergeWhere(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginMergeWhenMatched(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginMergeAction(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginOnSet(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginOnDelete(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginOnUpdate(StringWriter writer) {
        writeDelimiter(writer);
    }

    private void setDelimiter() {
        if (isNewline) {
            delimiter = newlineString;
        } else {
            delimiter = SPACE;
        }
    }

    private void writeDelimiter(StringWriter writer) {
        if (delimiter != null) {
            writer.write(delimiter);
        }
        setDelimiter();
    }

    private void writeDelimiter(StringWriter writer, boolean topLevel) {
        if (delimiter != null) {
            if (!topLevel) {
                writer.write(SPACE);
            } else {
                writer.write(delimiter);
            }
        }
        setDelimiter();
    }

    public void beginCreateDataFlow(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateVariable(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginUpdate(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateWindow(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateContext(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateSchema(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateIndex(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginDataFlowSchema(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginDataFlowOperator(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginDataFlowOperatorDetails(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void endDataFlowOperatorConfig(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void endDataFlowOperatorDetails(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateExpression(StringWriter writer) {
        writeDelimiter(writer);
    }

    public void beginCreateTable(StringWriter writer) {
        writeDelimiter(writer);
    }
}
