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

import com.espertech.esper.client.EPException;
import junit.framework.TestCase;

import java.io.FileNotFoundException;
import java.net.URL;

public class TestResourceLoader extends TestCase {
    private final static String TEST_RESOURCE = "regression/esper.test.readconfig.cfg.xml";

    public void testResolveResourceAsURL() throws Exception {
        URL url = ResourceLoader.getClasspathResourceAsURL("somefile", TEST_RESOURCE, Thread.currentThread().getContextClassLoader());
        assertNotNull(url);

        try {
            ResourceLoader.getClasspathResourceAsURL("somefile", "xxx", Thread.currentThread().getContextClassLoader());
            fail();
        } catch (FileNotFoundException ex) {
            // expected
        }
    }

    public void testClasspathOrURL() throws Exception {
        URL url = this.getClass().getClassLoader().getResource(TEST_RESOURCE);
        URL urlAfterResolve = ResourceLoader.resolveClassPathOrURLResource("a", url.toString(), Thread.currentThread().getContextClassLoader());
        assertEquals(url, urlAfterResolve);

        URL url3 = ResourceLoader.resolveClassPathOrURLResource("a", "file:///xxx/a.b", Thread.currentThread().getContextClassLoader());
        assertEquals("file:/xxx/a.b", url3.toString());

        try {
            ResourceLoader.resolveClassPathOrURLResource("a", "b", Thread.currentThread().getContextClassLoader());
            fail();
        } catch (FileNotFoundException ex) {
            // expected
        }
    }
}
