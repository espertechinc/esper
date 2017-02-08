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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.enummethod.dot.ExprDotStaticMethodWrap;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class ExprDotEvalStaticMethod implements ExprEvaluator, EventPropertyGetter
{
    private static final Logger log = LoggerFactory.getLogger(ExprDotEvalStaticMethod.class);

    private final String statementName;
    private final String classOrPropertyName;
	private final FastMethod staticMethod;
    private final ExprEvaluator[] childEvals;
    private final boolean isConstantParameters;
    private final ExprDotEval[] chainEval;
    private final ExprDotStaticMethodWrap resultWrapLambda;
    private final boolean rethrowExceptions;
    private final Object targetObject;

    private boolean isCachedResult;
    private Object cachedResult;

    public ExprDotEvalStaticMethod(String statementName,
                                   String classOrPropertyName,
                                   FastMethod staticMethod,
                                   ExprEvaluator[] childEvals,
                                   boolean constantParameters,
                                   ExprDotStaticMethodWrap resultWrapLambda,
                                   ExprDotEval[] chainEval,
                                   boolean rethrowExceptions,
                                   Object targetObject)
    {
        this.statementName = statementName;
        this.classOrPropertyName = classOrPropertyName;
        this.staticMethod = staticMethod;
        this.childEvals = childEvals;
        this.targetObject = targetObject;
        if (chainEval.length > 0) {
            isConstantParameters = false;
        }
        else {
            this.isConstantParameters = constantParameters;
        }
        this.resultWrapLambda = resultWrapLambda;
        this.chainEval = chainEval;
        this.rethrowExceptions = rethrowExceptions;
    }

    public Class getType()
    {
        if (chainEval.length == 0) {
            return staticMethod.getReturnType();
        }
        else {
            return EPTypeHelper.getNormalizedClass(chainEval[chainEval.length - 1].getTypeInfo());
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext)
	{
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qExprPlugInSingleRow(staticMethod.getJavaMethod());}
        if ((isConstantParameters) && (isCachedResult))
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aExprPlugInSingleRow(cachedResult);}
            return cachedResult;
        }

		Object[] args = new Object[childEvals.length];
		for(int i = 0; i < args.length; i++)
		{
			args[i] = childEvals[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
		}

		// The method is static so the object it is invoked on
		// can be null
		try
		{
            Object result = staticMethod.invoke(targetObject, args);

            result = ExprDotNodeUtility.evaluateChainWithWrap(resultWrapLambda, result, null, staticMethod.getReturnType(), chainEval, eventsPerStream, isNewData, exprEvaluatorContext);

            if (isConstantParameters)
            {
                cachedResult = result;
                isCachedResult = true;
            }

            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aExprPlugInSingleRow(result);}
            return result;
		}
		catch (InvocationTargetException e)
		{
            String message = JavaClassHelper.getMessageInvocationTarget(statementName, staticMethod.getJavaMethod(), classOrPropertyName, args, e);
            log.error(message, e.getTargetException());
            if (rethrowExceptions) {
                throw new EPException(message, e.getTargetException());
            }
		}
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aExprPlugInSingleRow(null);}
        return null;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object[] args = new Object[childEvals.length];
        for(int i = 0; i < args.length; i++)
        {
            args[i] = childEvals[i].evaluate(new EventBean[] {eventBean}, false, null);
        }

        // The method is static so the object it is invoked on
        // can be null
        try
        {
            return staticMethod.invoke(targetObject, args);
        }
        catch (InvocationTargetException e)
        {
            String message = JavaClassHelper.getMessageInvocationTarget(statementName, staticMethod.getJavaMethod(), classOrPropertyName, args, e);
            log.error(message, e.getTargetException());
            if (rethrowExceptions) {
                throw new EPException(message, e.getTargetException());
            }
        }
        return null;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return false;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}
