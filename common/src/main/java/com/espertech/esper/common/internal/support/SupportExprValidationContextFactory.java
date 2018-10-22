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
package com.espertech.esper.common.internal.support;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.client.util.ThreadingProfile;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContextBuilder;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;

public class SupportExprValidationContextFactory {
    public static ExprValidationContext makeEmpty() {
        return make(new StreamTypeServiceImpl(false));
    }

    public static ExprValidationContext make(StreamTypeService streamTypeService) {
        ModuleCompileTimeServices moduleServices = new ModuleCompileTimeServices();
        moduleServices.setConfiguration(new Configuration());
        moduleServices.setClasspathImportServiceCompileTime(SupportClasspathImport.INSTANCE);
        StatementCompileTimeServices services = new StatementCompileTimeServices(1, moduleServices);
        StatementRawInfo raw = new StatementRawInfo(1, "abc", null, StatementType.SELECT, null, null, null, null);
        return new ExprValidationContextBuilder(streamTypeService, raw, services).build();
    }

    public static ExprValidationContext makeEmpty(ThreadingProfile threadingProfile) {
        return makeEmpty();
    }
}
