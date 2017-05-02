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
package com.espertech.esper.spatial.quadtree.filterindex;

import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.core.SupportRandomAddThenRemove.runAssertion;
import static com.espertech.esper.spatial.quadtree.filterindex.SupportQuadTreeFilterIndexUtil.FILTERINDEX_ADDER;
import static com.espertech.esper.spatial.quadtree.filterindex.SupportQuadTreeFilterIndexUtil.FILTERINDEX_QUERIER;
import static com.espertech.esper.spatial.quadtree.filterindex.SupportQuadTreeFilterIndexUtil.FILTERINDEX_REMOVER;

public class TestQuadTreeFilterIndexRandomAddThenRemove extends TestCase {

    public void testRun() {
        runAssertion(100, 100, 0, 0, 100, 100, 4, 20, FILTERINDEX_ADDER, FILTERINDEX_REMOVER, FILTERINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
        runAssertion(100, 100, 50, 80, 20, 900, 4, 20, FILTERINDEX_ADDER, FILTERINDEX_REMOVER, FILTERINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
        runAssertion(100, 100, 50, 80, 20, 900, 2, 80, FILTERINDEX_ADDER, FILTERINDEX_REMOVER, FILTERINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
        runAssertion(1000, 100, 50, 800000, 2000, 900, 1000, 80, FILTERINDEX_ADDER, FILTERINDEX_REMOVER, FILTERINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
    }
}
