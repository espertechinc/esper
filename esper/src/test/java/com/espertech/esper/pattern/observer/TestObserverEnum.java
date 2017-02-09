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
package com.espertech.esper.pattern.observer;

import junit.framework.TestCase;

public class TestObserverEnum extends TestCase {
    public void testForName() {
        ObserverEnum enumValue = ObserverEnum.forName(ObserverEnum.TIMER_INTERVAL.getNamespace(), ObserverEnum.TIMER_INTERVAL.getName());
        assertEquals(enumValue, ObserverEnum.TIMER_INTERVAL);

        enumValue = ObserverEnum.forName(ObserverEnum.TIMER_INTERVAL.getNamespace(), "dummy");
        assertNull(enumValue);

        enumValue = ObserverEnum.forName("dummy", ObserverEnum.TIMER_INTERVAL.getName());
        assertNull(enumValue);
    }
}
