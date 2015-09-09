/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

/**
 * Represents a simple Math (+/-/divide/*) in a filter expression tree.
 */
public class ExprConcatNode extends ExprNodeBase implements ExprEvaluator
{

    private ThreadLocal<StringBuffer> localBuffer = new ThreadLocal<StringBuffer>() {
        @Override
        protected StringBuffer initialValue() {
            return new StringBuffer();
        }
    };
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = 5811427566733004327L;

    /**
     * Ctor.
     */
    public ExprConcatNode()
    {
    }

    public ExprEvaluator getExprEvaluator()
    {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException
    {
        if (this.getChildNodes().length < 2)
        {
            throw new ExprValidationException("Concat node must have at least 2 parameters");
        }
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        for (int i = 0; i < evaluators.length; i++)
        {
            Class childType = evaluators[i].getType();
            String childTypeName = childType == null ? "null" : childType.getSimpleName();
            if (childType != String.class)
            {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        childTypeName +
                        "' to string is not allowed");
            }
        }
        return null;
    }

    public Class getType()
    {
        return String.class;
    }

    public boolean isConstantResult()
    {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qExprConcat(this);}
        final StringBuffer buffer = localBuffer.get();
        buffer.delete(0, buffer.length());
        for (ExprEvaluator child : evaluators)
        {
            String result = (String) child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null)
            {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aExprConcat(null);}
                return null;
            }
            buffer.append(result);
        }
        String result = buffer.toString();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aExprConcat(result);}
        return result;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        for (ExprNode child : this.getChildNodes())
        {
            writer.append(delimiter);
            child.toEPL(writer, getPrecedence());
            delimiter = "||";
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.CONCAT;
    }

    public boolean equalsNode(ExprNode node)
    {
        if (!(node instanceof ExprConcatNode))
        {
            return false;
        }

        return true;
    }
}
