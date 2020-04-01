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
package com.espertech.esper.common.internal.epl.updatehelper;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

public class EventBeanUpdateItemForgeWExpressions {
    private final CodegenExpression rhsExpression;
    private final EventBeanUpdateItemArrayExpressions optionalArrayExpressions;

    public EventBeanUpdateItemForgeWExpressions(CodegenExpression rhsExpression, EventBeanUpdateItemArrayExpressions optionalArrayExpressions) {
        this.rhsExpression = rhsExpression;
        this.optionalArrayExpressions = optionalArrayExpressions;
    }

    public CodegenExpression getRhsExpression() {
        return rhsExpression;
    }

    public EventBeanUpdateItemArrayExpressions getOptionalArrayExpressions() {
        return optionalArrayExpressions;
    }
}