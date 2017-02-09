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
package com.espertech.esper.util;

import junit.framework.TestCase;

import java.util.UUID;

public class TestUuidGenerator extends TestCase {
    public void testGenerate() {
        String uuid = UuidGenerator.generate();
        System.out.println(uuid + " length " + uuid.length());

        String newuuid = UUID.randomUUID().toString();
        System.out.println(newuuid + " length " + newuuid.length());
    }
}
