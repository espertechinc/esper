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

import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumerationGivenEvent;
import com.espertech.esper.epl.rettype.EPType;

public class ExprDotEnumerationSourceForProps extends ExprDotEnumerationSource {
    private final ExprEvaluatorEnumerationGivenEvent enumerationGivenEvent;

    public ExprDotEnumerationSourceForProps(ExprEvaluatorEnumeration enumeration, EPType returnType, Integer streamOfProviderIfApplicable, ExprEvaluatorEnumerationGivenEvent enumerationGivenEvent) {
        super(returnType, streamOfProviderIfApplicable, enumeration);
        this.enumerationGivenEvent = enumerationGivenEvent;
    }

    public ExprEvaluatorEnumerationGivenEvent getEnumerationGivenEvent() {
        return enumerationGivenEvent;
    }
}
