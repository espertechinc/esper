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
package com.espertech.esper.support;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactoryDefault;
import com.espertech.esper.epl.core.engineimport.EngineImportServiceImpl;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.time.TimeAbacusMilliseconds;

import java.util.TimeZone;

public class SupportExprValidationContextFactory {
    public static ExprValidationContext makeEmpty() {
        return makeEmpty(ConfigurationEngineDefaults.ThreadingProfile.NORMAL);
    }

    public static ExprValidationContext makeEmpty(ConfigurationEngineDefaults.ThreadingProfile threadingProfile) {
        ConfigurationEngineDefaults.ByteCodeGeneration codegenSettings = new ConfigurationEngineDefaults.ByteCodeGeneration();
        codegenSettings.setEnablePropertyGetter(false);
        codegenSettings.setEnableExpression(false);
        return new ExprValidationContext(null, new EngineImportServiceImpl(false, false, false, false, null, TimeZone.getDefault(), TimeAbacusMilliseconds.INSTANCE, threadingProfile, null, AggregationFactoryFactoryDefault.INSTANCE, codegenSettings, "default", null), null, null, null, null, null, new SupportExprEvaluatorContext(null), null, null, 1, null, null, false, false, false, false, null, false);
    }

    public static ExprValidationContext make(StreamTypeService streamTypeService) {
        return new ExprValidationContext(streamTypeService, null, null, null, null, null, null, new SupportExprEvaluatorContext(null), null, null, -1, null, null, false, false, false, false, null, false);
    }
}
