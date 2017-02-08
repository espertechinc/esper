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

import java.net.URL;

import com.espertech.esper.client.EPException;

public class TestResourceLoader extends TestCase
{
    private final static String TEST_RESOURCE = "regression/esper.test.readconfig.cfg.xml";

    public void testResolveResourceAsURL()
    {
        URL url = ResourceLoader.getClasspathResourceAsURL("somefile", TEST_RESOURCE, Thread.currentThread().getContextClassLoader());
        assertNotNull(url);

        try
        {
            ResourceLoader.getClasspathResourceAsURL("somefile", "xxx", Thread.currentThread().getContextClassLoader());
            fail();
        }
        catch (EPException ex)
        {
            // expected
        }
    }

    public void testClasspathOrURL()
    {
        URL url = this.getClass().getClassLoader().getResource(TEST_RESOURCE);
        URL urlAfterResolve = ResourceLoader.resolveClassPathOrURLResource("a", url.toString(), Thread.currentThread().getContextClassLoader());
        assertEquals(url, urlAfterResolve);

        URL url3 = ResourceLoader.resolveClassPathOrURLResource("a", "file:///xxx/a.b", Thread.currentThread().getContextClassLoader());
        assertEquals("file:/xxx/a.b", url3.toString());

        try
        {
            ResourceLoader.resolveClassPathOrURLResource("a", "b", Thread.currentThread().getContextClassLoader());
            fail();
        }
        catch (EPException ex)
        {
            // expected
        }
    }
}
