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
package com.espertech.esper.type;

import junit.framework.TestCase;

public class TestCronParameter extends TestCase {
    public void testFormat() {
        CronParameter cronParameter = new CronParameter(CronOperatorEnum.LASTDAY, 1);
        assertEquals("LASTDAY(day 1 month null)", cronParameter.formatted());
    }
}
