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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.util.TypeWidener;

public class InternalEventRouterWriterArrayElement extends InternalEventRouterWriter {
    public final static EPTypeClass EPTYPE = new EPTypeClass(InternalEventRouterWriterArrayElement.class);

    private ExprEvaluator indexExpression;
    private ExprEvaluator rhsExpression;
    private TypeWidener typeWidener;
    private String propertyName;

    public ExprEvaluator getIndexExpression() {
        return indexExpression;
    }

    public void setIndexExpression(ExprEvaluator indexExpression) {
        this.indexExpression = indexExpression;
    }

    public ExprEvaluator getRhsExpression() {
        return rhsExpression;
    }

    public void setRhsExpression(ExprEvaluator rhsExpression) {
        this.rhsExpression = rhsExpression;
    }

    public TypeWidener getTypeWidener() {
        return typeWidener;
    }

    public void setTypeWidener(TypeWidener typeWidener) {
        this.typeWidener = typeWidener;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
