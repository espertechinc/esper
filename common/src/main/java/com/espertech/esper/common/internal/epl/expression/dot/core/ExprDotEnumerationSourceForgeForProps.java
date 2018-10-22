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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationGivenEventForge;
import com.espertech.esper.common.internal.rettype.EPType;

public class ExprDotEnumerationSourceForgeForProps extends ExprDotEnumerationSourceForge {
    private final ExprEnumerationGivenEventForge enumerationGivenEvent;

    public ExprDotEnumerationSourceForgeForProps(ExprEnumerationForge enumeration, EPType returnType, Integer streamOfProviderIfApplicable, ExprEnumerationGivenEventForge enumerationGivenEvent) {
        super(returnType, streamOfProviderIfApplicable, enumeration);
        this.enumerationGivenEvent = enumerationGivenEvent;
    }

    public ExprEnumerationGivenEventForge getEnumerationGivenEvent() {
        return enumerationGivenEvent;
    }
}
