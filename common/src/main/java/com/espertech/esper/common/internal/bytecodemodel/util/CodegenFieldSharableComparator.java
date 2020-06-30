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
package com.espertech.esper.common.internal.bytecodemodel.util;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class CodegenFieldSharableComparator implements CodegenFieldSharable {
    private final CodegenSharableSerdeName name;
    private final EPType[] types;
    private final boolean isSortUsingCollator;
    private final boolean[] descending;

    public CodegenFieldSharableComparator(CodegenSharableSerdeName name, EPType[] types, boolean isSortUsingCollator, boolean[] descending) {
        this.name = name;
        this.types = types;
        this.isSortUsingCollator = isSortUsingCollator;
        this.descending = descending;
    }

    public EPTypeClass type() {
        return EPTypePremade.COMPARATOR.getEPType();
    }

    public CodegenExpression initCtorScoped() {
        return staticMethod(ExprNodeUtilityMake.class, name.methodName, constant(types), constant(isSortUsingCollator), constant(descending));
    }

    public enum CodegenSharableSerdeName {
        COMPARATORHASHABLEMULTIKEYS("getComparatorHashableMultiKeys"),
        COMPARATOROBJECTARRAYNONHASHABLE("getComparatorObjectArrayNonHashable");

        private final String methodName;

        CodegenSharableSerdeName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }
    }

}
