/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.epl;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.ExprValidationContextFactory;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class SupportExprNodeUtil
{
    public static void validate(ExprNode node) throws ExprValidationException{
        node.validate(ExprValidationContextFactory.makeEmpty());
    }
}
