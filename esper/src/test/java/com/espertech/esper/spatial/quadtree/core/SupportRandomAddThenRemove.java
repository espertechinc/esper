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
package com.espertech.esper.spatial.quadtree.core;

import java.util.*;

import static com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.randomQuery;

public class SupportRandomAddThenRemove {
    public static void runAssertion(int numPoints, int numQueries, double x, double y, double width, double height, int leafCapacity, int maxHeight,
                              SupportQuadTreeUtil.AdderUnique adder, SupportQuadTreeUtil.Remover remover, SupportQuadTreeUtil.Querier querier, SupportQuadTreeUtil.Generator generator) {

        QuadTree<Object> quadTree = QuadTreeFactory.make(x, y, width, height, leafCapacity, maxHeight);

        // generate
        Random random = new Random();
        List<SupportPointWithId> points = generator.generate(random, numPoints, x, y, width, height);

        // add
        for (SupportPointWithId point : points) {
            adder.addOrSet(quadTree, point);
        }

        // query
        for (int i = 0; i < numQueries; i++) {
            randomQuery(quadTree, points, random, x, y, width, height, querier);
        }

        // remove point-by-point
        while(!points.isEmpty()) {
            int removeIndex = random.nextInt(points.size());
            SupportPointWithId removed = points.remove(removeIndex);
            remover.removeOrDelete(quadTree, removed);

            for (int i = 0; i < numQueries; i++) {
                randomQuery(quadTree, points, random, x, y, width, height, querier);
            }
        }
    }
}
