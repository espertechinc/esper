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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertTrue;

public class SupportExecUniqueRandomIntPointsInSquare {

    public static <L> void runAssertion(SupportQuadTreeToolUnique<L> tools) {
        assertTrue(tools.generator.unique());

        SupportQuadTreeConfig[] configs = new SupportQuadTreeConfig[] {
                new SupportQuadTreeConfig(0, 0, 1000, 1000, 4, 20),
                new SupportQuadTreeConfig(0, 0, 1000, 1000, 1000, 20),
                new SupportQuadTreeConfig(0, 0, 1000, 1000, 2, 50)
        };

        for (SupportQuadTreeConfig config : configs) {
            runAssertionPointsUnique(1000, config, tools);
        }
    }

    private static <L> void runAssertionPointsUnique(int numPoints, SupportQuadTreeConfig config, SupportQuadTreeToolUnique<L> tools) {
        Random random = new Random();
        L quadTree = tools.factory.make(config);
        List<SupportRectangleWithId> points = tools.generator.generate(random, numPoints, config.getX(), config.getY(), config.getWidth(), config.getHeight());

        // add
        for (SupportRectangleWithId p : points) {
            tools.adderUnique.addOrSet(quadTree, p);
        }

        // find all individually
        for (SupportRectangleWithId p : points) {
            Collection<Object> values = tools.querier.query(quadTree, p.getX(), p.getY(), 0.9, 0.9);
            assertTrue("Failed to find " + p.toString(), values != null && !values.isEmpty());
            assertEquals(1, values.size());
            assertEquals(p.getId(), values.iterator().next());
        }

        // get all content
        Collection<Object> all = tools.querier.query(quadTree, config.getX(), config.getY(), config.getWidth(), config.getHeight());
        assertEquals(points.size(), all.size());
        assertEquals(points.size(), new HashSet<>(all).size());
        for (Object value : all) {
            assertTrue(value instanceof String);
        }

        // remove all
        for (SupportRectangleWithId p : points) {
            tools.remover.removeOrDelete(quadTree, p);
        }

        Collection<Object> values = tools.querier.query(quadTree, config.getX(), config.getY(), config.getWidth(), config.getHeight());
        assertNull(values);
    }
}
