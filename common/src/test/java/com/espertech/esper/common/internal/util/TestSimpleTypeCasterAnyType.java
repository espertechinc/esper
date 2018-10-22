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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.supportunit.bean.ISupportA;
import com.espertech.esper.common.internal.supportunit.bean.ISupportABCImpl;
import com.espertech.esper.common.internal.supportunit.bean.ISupportBCImpl;
import junit.framework.TestCase;

public class TestSimpleTypeCasterAnyType extends TestCase {
    private SimpleTypeCasterAnyType caster;

    public void setUp() {
        caster = new SimpleTypeCasterAnyType(ISupportA.class);
    }

    public void testCast() {
        assertNull(caster.cast(new Object()));
        assertNull(caster.cast(new SupportBean()));
        assertNotNull(caster.cast(new ISupportABCImpl("", "", "", "")));
        assertNotNull(caster.cast(new ISupportABCImpl("", "", "", "")));
        assertNull(caster.cast(new ISupportBCImpl("", "", "")));
    }
}
