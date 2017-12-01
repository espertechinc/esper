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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportSingleRowDesc;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ContextControllerHashedGetterSingleRow implements EventPropertyGetter {
    private static final Logger log = LoggerFactory.getLogger(ContextControllerHashedGetterSingleRow.class);

    private final String statementName;
    private final FastMethod fastMethod;
    private final ExprEvaluator[] evaluators;
    private final int granularity;

    public ContextControllerHashedGetterSingleRow(String statementName, String functionName, Pair<Class, EngineImportSingleRowDesc> func, List<ExprNode> parameters, int granularity, EngineImportService engineImportService, EventType eventType, EventAdapterService eventAdapterService, int statementId, TableService tableService, String engineURI)
            throws ExprValidationException {
        ExprNodeUtilMethodDesc staticMethodDesc = ExprNodeUtilityRich.resolveMethodAllowWildcardAndStream(func.getFirst().getName(), null, func.getSecond().getMethodName(), parameters, engineImportService, eventAdapterService, statementId, true, eventType, new ExprNodeUtilResolveExceptionHandlerDefault(func.getSecond().getMethodName(), true), func.getSecond().getMethodName(), tableService, engineURI);
        this.statementName = statementName;
        this.evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(staticMethodDesc.getChildForges());
        this.granularity = granularity;
        this.fastMethod = staticMethodDesc.getFastMethod();
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        EventBean[] events = new EventBean[]{eventBean};

        Object[] parameters = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            parameters[i] = evaluators[i].evaluate(events, true, null);
        }

        try {
            Object result = fastMethod.invoke(null, parameters);
            if (result == null) {
                return 0;
            }
            int value = ((Number) result).intValue();
            if (value >= 0) {
                return value % granularity;
            }
            return -value % granularity;
        } catch (InvocationTargetException e) {
            String message = JavaClassHelper.getMessageInvocationTarget(statementName, fastMethod.getJavaMethod(), fastMethod.getDeclaringClass().getName(), parameters, e.getTargetException());
            log.error(message, e.getTargetException());
        }

        return 0;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return false;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}
