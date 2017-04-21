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
package com.espertech.esper.epl.join.plan;

import junit.framework.TestCase;

public class TestQueryPlanIndex extends TestCase {
    private QueryPlanIndex indexSpec;

    public void setUp() {
        QueryPlanIndexItem itemOne = new QueryPlanIndexItem(new String[]{"p01", "p02"}, null, null, null, false, null);
        QueryPlanIndexItem itemTwo = new QueryPlanIndexItem(new String[]{"p21"}, new Class[0], null, null, false, null);
        QueryPlanIndexItem itemThree = new QueryPlanIndexItem(new String[0], new Class[0], null, null, false, null);
        indexSpec = QueryPlanIndex.makeIndex(itemOne, itemTwo, itemThree);
    }

    public void testInvalidUse() {
        try {
            new QueryPlanIndex(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testGetIndexNum() {
        assertNotNull(indexSpec.getIndexNum(new String[]{"p01", "p02"}, null));
        assertNotNull(indexSpec.getIndexNum(new String[]{"p21"}, null));
        assertNotNull(indexSpec.getIndexNum(new String[0], null));

        assertNull(indexSpec.getIndexNum(new String[]{"YY", "XX"}, null));
    }

    public void testAddIndex() {
        String indexNum = indexSpec.addIndex(new String[]{"a", "b"}, null);
        assertNotNull(indexNum);
        assertEquals(indexNum, indexSpec.getIndexNum(new String[]{"a", "b"}, null).getFirst().getName());
    }
}
