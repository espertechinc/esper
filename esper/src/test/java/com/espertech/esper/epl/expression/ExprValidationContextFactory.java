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

package com.espertech.esper.epl.expression;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.epl.core.EngineImportServiceImpl;
import com.espertech.esper.epl.core.MethodResolutionServiceImpl;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.support.core.SupportExprEvaluatorContext;

import java.util.TimeZone;

public class ExprValidationContextFactory {
    public static ExprValidationContext makeEmpty() {
        return makeEmpty(ConfigurationEngineDefaults.ThreadingProfile.NORMAL);
    }

    public static ExprValidationContext makeEmpty(ConfigurationEngineDefaults.ThreadingProfile threadingProfile) {
        return new ExprValidationContext(null, new MethodResolutionServiceImpl(new EngineImportServiceImpl(false, false, false, false, null, TimeZone.getDefault(), threadingProfile), null), null, null, null, null, new SupportExprEvaluatorContext(null), null, null, null, null, null, false, false, false, false, null, false);
    }

    public static ExprValidationContext make(StreamTypeService streamTypeService) {
        return new ExprValidationContext(streamTypeService, null, null, null, null, null, new SupportExprEvaluatorContext(null), null, null, null, null, null, false, false, false, false, null, false);
    }
}
