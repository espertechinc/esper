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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.support.SupportExprValidationContextFactory;

public class SupportOuterJoinDescFactory {
    public static OuterJoinDesc makeDesc(String propOne, String streamOne, String propTwo, String streamTwo, OuterJoinType type) throws Exception {
        ExprIdentNode identNodeOne = new ExprIdentNodeImpl(propOne, streamOne);
        ExprIdentNode identNodeTwo = new ExprIdentNodeImpl(propTwo, streamTwo);

        ExprValidationContext context = SupportExprValidationContextFactory.make(new SupportStreamTypeSvc3Stream());
        identNodeOne.validate(context);
        identNodeTwo.validate(context);
        OuterJoinDesc desc = new OuterJoinDesc(type, identNodeOne, identNodeTwo, null, null);

        return desc;
    }
}
