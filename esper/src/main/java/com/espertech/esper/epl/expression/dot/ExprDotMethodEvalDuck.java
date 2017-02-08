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
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ExprDotMethodEvalDuck implements ExprDotEval {
    private static final Logger log = LoggerFactory.getLogger(ExprDotMethodEvalDuck.class);

    private final String statementName;
    private final EngineImportService engineImportService;
    private final String methodName;
    private final Class[] parameterTypes;
    private final ExprEvaluator[] parameters;

    private Map<Class, FastMethod> cache;

    public ExprDotMethodEvalDuck(String statementName, EngineImportService engineImportService, String methodName, Class[] parameterTypes, ExprEvaluator[] parameters) {
        this.statementName = statementName;
        this.engineImportService = engineImportService;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
        cache = new HashMap<Class, FastMethod>();
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitMethod(methodName);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

        FastMethod method;
        if (cache.containsKey(target.getClass())) {
            method = cache.get(target.getClass());
        } else {
            method = getFastMethod(target.getClass());
            cache.put(target.getClass(), method);
        }

        if (method == null) {
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

    private FastMethod getFastMethod(Class clazz) {
        try {
            Method method = engineImportService.resolveMethod(clazz, methodName, parameterTypes, new boolean[parameterTypes.length], new boolean[parameterTypes.length]);
            FastClass declaringClass = FastClass.create(engineImportService.getFastClassClassLoader(method.getDeclaringClass()), method.getDeclaringClass());
            return declaringClass.getMethod(method);
        } catch (Exception e) {
            log.debug("Not resolved for class '" + clazz.getName() + "' method '" + methodName + "'");
        }
        return null;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.singleValue(Object.class);
    }
}
