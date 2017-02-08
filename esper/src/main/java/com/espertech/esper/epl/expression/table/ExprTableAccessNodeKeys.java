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
package com.espertech.esper.epl.expression.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableMetadata;

import java.io.StringWriter;

public class ExprTableAccessNodeKeys extends ExprTableAccessNode implements ExprEvaluator {

    private static final long serialVersionUID = -1905494870160778124L;

    public ExprTableAccessNodeKeys(String tableName) {
        super(tableName);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer);
        writer.append(".keys()");
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    protected void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata) throws ExprValidationException {
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluate(eventsPerStream, isNewData, context);
    }

    public Class getType() {
        return Object[].class;
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        return true;
    }
}
