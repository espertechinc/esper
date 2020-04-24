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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompileHook;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SupportFilterSpecCompileHook implements FilterSpecCompileHook {
    private static List<SupportFilterSpecCompileEntry> entries;

    static {
        reset();
    }

    public static void reset() {
        entries = new ArrayList<>();
    }

    public static List<SupportFilterSpecCompileEntry> getEntries() {
        return entries;
    }

    public void filterSpec(EventType eventType, List<FilterSpecParamForge>[] spec) {
        entries.add(new SupportFilterSpecCompileEntry(eventType, spec));
    }

    public static FilterSpecParamForge assertSingleForTypeAndReset(String typeName) {
        SupportFilterSpecCompileEntry found = null;
        for (SupportFilterSpecCompileEntry entry : entries) {
            if (!entry.getEventType().getName().equals(typeName)) {
                continue;
            }
            if (found != null) {
                fail("Found multiple");
            }
            found = entry;
        }
        assertNotNull(found);
        reset();
        return found.getAssertSingle(typeName);
    }

    public static FilterSpecParamForge assertSingleAndReset(String typeName) {
        assertEquals(1, entries.size());
        SupportFilterSpecCompileEntry entry = entries.get(0);
        reset();
        return entry.getAssertSingle(typeName);
    }
}
