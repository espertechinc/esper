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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprWildcard;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluators;

public class StatViewAdditionalPropsForge {
    private final String[] additionalProps;
    private final ExprNode[] additionalEvals;
    private final Class[] additionalTypes;
    private final DataInputOutputSerdeForge[] additionalSerdes;

    public StatViewAdditionalPropsForge(String[] additionalProps, ExprNode[] additionalEvals, Class[] additionalTypes, DataInputOutputSerdeForge[] additionalSerdes) {
        this.additionalProps = additionalProps;
        this.additionalEvals = additionalEvals;
        this.additionalTypes = additionalTypes;
        this.additionalSerdes = additionalSerdes;
    }

    public String[] getAdditionalProps() {
        return additionalProps;
    }

    public ExprNode[] getAdditionalEvals() {
        return additionalEvals;
    }

    public Class[] getAdditionalTypes() {
        return additionalTypes;
    }

    public static StatViewAdditionalPropsForge make(ExprNode[] validated, int startIndex, EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) {
        if (validated.length <= startIndex) {
            return null;
        }

        List<String> additionalProps = new ArrayList<String>();
        List<ExprNode> lastValueForges = new ArrayList<>();
        List<Class> lastValueTypes = new ArrayList<>();
        List<DataInputOutputSerdeForge> lastSerdes = new ArrayList<>();
        boolean copyAllProperties = false;

        for (int i = startIndex; i < validated.length; i++) {

            if (validated[i] instanceof ExprWildcard) {
                copyAllProperties = true;
            } else {
                additionalProps.add(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validated[i]));
                Class evalType = validated[i].getForge().getEvaluationType();
                lastValueTypes.add(evalType);
                lastValueForges.add(validated[i]);
                lastSerdes.add(viewForgeEnv.getSerdeResolver().serdeForDerivedViewAddProp(evalType, viewForgeEnv.getStatementRawInfo()));
            }
        }

        if (copyAllProperties) {
            for (EventPropertyDescriptor propertyDescriptor : parentEventType.getPropertyDescriptors()) {
                if (propertyDescriptor.isFragment()) {
                    continue;
                }
                additionalProps.add(propertyDescriptor.getPropertyName());
                Class type = propertyDescriptor.getPropertyType();
                lastValueForges.add(new ExprIdentNodeImpl(parentEventType, propertyDescriptor.getPropertyName(), streamNumber));
                lastValueTypes.add(type);
                lastSerdes.add(viewForgeEnv.getSerdeResolver().serdeForDerivedViewAddProp(type, viewForgeEnv.getStatementRawInfo()));
            }
        }

        String[] addPropsArr = additionalProps.toArray(new String[additionalProps.size()]);
        ExprNode[] valueExprArr = lastValueForges.toArray(new ExprNode[lastValueForges.size()]);
        Class[] typeArr = lastValueTypes.toArray(new Class[lastValueTypes.size()]);
        DataInputOutputSerdeForge[] additionalForges = lastSerdes.toArray(new DataInputOutputSerdeForge[0]);
        return new StatViewAdditionalPropsForge(addPropsArr, valueExprArr, typeArr, additionalForges);
    }

    public static void addCheckDupProperties(Map<String, Object> target, StatViewAdditionalPropsForge addProps, ViewFieldEnum... builtin) {
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

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope) {
        return newInstance(StatViewAdditionalPropsEval.class, constant(additionalProps),
                codegenEvaluators(additionalEvals, method, this.getClass(), classScope), constant(additionalTypes),
                DataInputOutputSerdeForge.codegenArray(additionalSerdes, method, classScope, null));
    }
}
