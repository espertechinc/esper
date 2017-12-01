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
package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.view.ViewFieldEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatViewAdditionalProps {
    private final String[] additionalProps;
    private final ExprEvaluator[] additionalEvals;
    private final Class[] additionalTypes;

    private StatViewAdditionalProps(String[] additionalProps, ExprEvaluator[] additionalEvals, Class[] additionalTypes) {
        this.additionalProps = additionalProps;
        this.additionalEvals = additionalEvals;
        this.additionalTypes = additionalTypes;
    }

    public String[] getAdditionalProps() {
        return additionalProps;
    }

    public ExprEvaluator[] getAdditionalEvals() {
        return additionalEvals;
    }

    public Class[] getAdditionalTypes() {
        return additionalTypes;
    }

    public static StatViewAdditionalProps make(ExprNode[] validated, int startIndex, EventType parentEventType, EngineImportService engineImportService, String statementName) {
        if (validated.length <= startIndex) {
            return null;
        }

        List<String> additionalProps = new ArrayList<String>();
        List<ExprEvaluator> lastValueEvals = new ArrayList<>();
        List<Class> lastValueTypes = new ArrayList<>();
        boolean copyAllProperties = false;

        for (int i = startIndex; i < validated.length; i++) {

            if (validated[i] instanceof ExprWildcard) {
                copyAllProperties = true;
            }

            additionalProps.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validated[i]));
            lastValueTypes.add(validated[i].getForge().getEvaluationType());
            ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(validated[i].getForge(), engineImportService, StatViewAdditionalProps.class, false, statementName);
            lastValueEvals.add(evaluator);
        }

        if (copyAllProperties) {
            for (EventPropertyDescriptor propertyDescriptor : parentEventType.getPropertyDescriptors()) {
                if (propertyDescriptor.isFragment()) {
                    continue;
                }
                additionalProps.add(propertyDescriptor.getPropertyName());
                final EventPropertyGetter getter = parentEventType.getGetter(propertyDescriptor.getPropertyName());
                final Class type = propertyDescriptor.getPropertyType();
                ExprEvaluator exprEvaluator = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                        return getter.get(eventsPerStream[0]);
                    }
                };
                lastValueEvals.add(exprEvaluator);
                lastValueTypes.add(type);
            }
        }

        String[] addPropsArr = additionalProps.toArray(new String[additionalProps.size()]);
        ExprEvaluator[] valueExprArr = lastValueEvals.toArray(new ExprEvaluator[lastValueEvals.size()]);
        Class[] typeArr = lastValueTypes.toArray(new Class[lastValueTypes.size()]);
        return new StatViewAdditionalProps(addPropsArr, valueExprArr, typeArr);
    }

    public void addProperties(Map<String, Object> newDataMap, Object[] lastValuesEventNew) {
        if (lastValuesEventNew != null) {
            for (int i = 0; i < additionalProps.length; i++) {
                newDataMap.put(additionalProps[i], lastValuesEventNew[i]);
            }
        }
    }

    public static void addCheckDupProperties(Map<String, Object> target, StatViewAdditionalProps addProps, ViewFieldEnum... builtin) {
        if (addProps == null) {
            return;
        }

        for (int i = 0; i < addProps.getAdditionalProps().length; i++) {
            String name = addProps.getAdditionalProps()[i];
            for (int j = 0; j < builtin.length; j++) {
                if (name.toLowerCase(Locale.ENGLISH).equals(builtin[j].getName().toLowerCase(Locale.ENGLISH))) {
                    throw new IllegalArgumentException("The property by name '" + name + "' overlaps the property name that the view provides");
                }
            }
            target.put(name, addProps.additionalTypes[i]);
        }
    }
}
