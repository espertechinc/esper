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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import org.junit.Assert;

public class SupportModelHelper {
    public static EPStatement createByCompileOrParse(EPServiceProvider epService, boolean soda, String epl) {
        return createByCompileOrParse(epService, soda, epl, null);
    }

    public static EPStatement createByCompileOrParse(EPServiceProvider epService, boolean soda, String epl, Object statementUserObject) {
        if (!soda) {
            return epService.getEPAdministrator().createEPL(epl, statementUserObject);
        }
        return compileCreate(epService, epl, statementUserObject);
    }

    public static EPStatement compileCreate(EPServiceProvider epService, String epl) {
        return compileCreate(epService, epl, null);
    }

    public static EPStatement compileCreate(EPServiceProvider epService, String epl, Object statementUserObject) {
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        Assert.assertEquals(epl, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model, null, statementUserObject);
        Assert.assertEquals(epl, stmt.getText());
        return stmt;
    }
}
