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

import com.espertech.esper.spatial.quadtree.mxcif.SupportRectangleWithId;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class SupportExecRandomAddThenRemove {

    public static <L> void runAssertion(SupportQuadTreeToolUnique<L> tools) {
        SupportQuadTreeConfig[] configs = new SupportQuadTreeConfig[] {
                new SupportQuadTreeConfig(0, 0, 100, 100, 4, 20),
                new SupportQuadTreeConfig(50, 80, 20, 900, 4, 20),
                new SupportQuadTreeConfig(50, 80, 20, 900, 2, 80),
                new SupportQuadTreeConfig(50, 800000, 2000, 900, 1000, 80),
        };

        for (SupportQuadTreeConfig config : configs) {
            runAssertion(100, 100, config, tools);
        }
    }

    private static <L> void runAssertion(int numPoints, int numQueries, SupportQuadTreeConfig config, SupportQuadTreeToolUnique<L> tools) {

        L quadTree = tools.factory.make(config);

        // generate
        Random random = new Random();
        List<SupportRectangleWithId> rectangles = tools.generator.generate(random, numPoints, config.getX(), config.getY(), config.getWidth(), config.getHeight());

        // add
        for (SupportRectangleWithId rectangle : rectangles) {
            tools.adderUnique.addOrSet(quadTree, rectangle);
        }

        // query
        for (int i = 0; i < numQueries; i++) {
            SupportQuadTreeUtil.randomQuery(quadTree, rectangles, random, config.getX(), config.getY(), config.getWidth(), config.getHeight(), tools.querier, tools.pointInsideChecking);
        }

        // remove point-by-point
        while(!rectangles.isEmpty()) {
            int removeIndex = random.nextInt(rectangles.size());
            SupportRectangleWithId removed = rectangles.remove(removeIndex);
            tools.remover.removeOrDelete(quadTree, removed);

            for (int i = 0; i < numQueries; i++) {
                SupportQuadTreeUtil.randomQuery(quadTree, rectangles, random, config.getX(), config.getY(), config.getWidth(), config.getHeight(), tools.querier, tools.pointInsideChecking);
            }
        }
    }
}
