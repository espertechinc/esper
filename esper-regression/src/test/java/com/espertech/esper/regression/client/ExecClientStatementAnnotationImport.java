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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.client.MyAnnotationValueEnum;
import com.espertech.esper.supportregression.client.MyAnnotationValueEnumTwo;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

public class ExecClientStatementAnnotationImport implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addAnnotationImport(SupportEnum.class);
        configuration.addAnnotationImport(MyAnnotationValueEnum.class);
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // init-time import
        epService.getEPAdministrator().createEPL("@MyAnnotationValueEnum(supportEnum = SupportEnum.ENUM_VALUE_1) " +
                "select * from SupportBean");

        // try invalid annotation not yet imported
        String epl = "@MyAnnotationValueEnumTwo(supportEnum = SupportEnum.ENUM_VALUE_1) select * from SupportBean";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Failed to process statement annotations: Failed to resolve @-annotation");

        // runtime import
        epService.getEPAdministrator().getConfiguration().addAnnotationImport(MyAnnotationValueEnumTwo.class.getName());
        epService.getEPAdministrator().createEPL(epl);

        // try invalid use : these are annotation-specific imports of an annotation and an enum
        SupportMessageAssertUtil.tryInvalid(epService, "select * from MyAnnotationValueEnumTwo",
                "Failed to resolve event type: Event type or class named");
        SupportMessageAssertUtil.tryInvalid(epService, "select SupportEnum.ENUM_VALUE_1 from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'SupportEnum.ENUM_VALUE_1'");

        epService.getEPAdministrator().destroyAllStatements();
    }
}
