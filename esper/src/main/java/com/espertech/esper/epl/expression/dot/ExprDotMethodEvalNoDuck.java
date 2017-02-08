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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class ExprDotMethodEvalNoDuck implements ExprDotEval {
    private static final Logger log = LoggerFactory.getLogger(ExprDotMethodEvalNoDuck.class);

    protected final String statementName;
    protected final FastMethod method;
    private final ExprEvaluator[] parameters;

    public ExprDotMethodEvalNoDuck(String statementName, FastMethod method, ExprEvaluator[] parameters) {
        this.statementName = statementName;
        this.method = method;
        this.parameters = parameters;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitMethod(method.getName());
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = parameters[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            String message = JavaClassHelper.getMessageInvocationTarget(statementName, method.getJavaMethod(), target.getClass().getName(), args, e);
            log.error(message, e.getTargetException());
        }
        return null;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.fromMethod(method.getJavaMethod());
    }
}
