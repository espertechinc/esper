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
package com.espertech.esper.spatial.quadtree.rowindex;

import com.espertech.esper.spatial.quadtree.core.SupportPointWithId;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.spatial.quadtree.core.SupportRandomAddThenRemove.runAssertion;
import static com.espertech.esper.spatial.quadtree.rowindex.SupportQuadTreeRowIndexUtil.*;

public class TestQuadTreeRowIndexRandomAddThenRemove extends TestCase {
    private final static SupportQuadTreeUtil.Generator GENERATOR_NONUNIQUE = (random, numPoints, x, y, width, height) -> {
        List<SupportPointWithId> points = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double px = random.nextDouble() * width + x;
            double py = random.nextDouble() * height + y;
            points.add(new SupportPointWithId("P" + i, px, py));
        }
        return points;
    };

    public void testRun() {
        runAssertion(100, 100, 0, 0, 100, 100, 4, 20, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, GENERATOR_NONUNIQUE);
        runAssertion(100, 100, 50, 80, 20, 900, 4, 20, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, GENERATOR_NONUNIQUE);
        runAssertion(100, 100, 50, 80, 20, 900, 2, 80, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, GENERATOR_NONUNIQUE);
        runAssertion(1000, 100, 50, 800000, 2000, 900, 1000, 80, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, GENERATOR_NONUNIQUE);

        runAssertion(100, 100, 0, 0, 100, 100, 4, 20, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
        runAssertion(100, 100, 50, 80, 20, 900, 4, 20, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
        runAssertion(100, 100, 50, 80, 20, 900, 2, 80, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
        runAssertion(1000, 100, 50, 800000, 2000, 900, 1000, 80, ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER, SupportQuadTreeUtil.GeneratorUniqueDouble.INSTANCE);
    }
}
