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

import com.espertech.esper.client.scopetest.EPAssertionUtil;

import java.util.*;

public class SupportQuadTreeUtil {

    public static String printPoint(double x, double y) {
        return "(" + x + "," + y + ")";
    }

    public static void assertPointsById(Collection<SupportPointWithId> points, Collection<Object> received, double x, double y, double width, double height) {
        BoundingBox boundingBox = new BoundingBox(x, y, x+width, y+height);
        List<String> expected = new ArrayList<>();
        for (SupportPointWithId p : points) {
            if (boundingBox.containsPoint(p.getX(), p.getY())) {
                expected.add(p.getId());
            }
        }
        if (received == null && expected.isEmpty()) {
            return;
        }
        EPAssertionUtil.assertEqualsAnyOrder(expected.toArray(), received.toArray());
    }

    public static QuadTreeNodeLeaf<Object> navigateLeaf(QuadTree<Object> tree, String directions) {
        return (QuadTreeNodeLeaf<Object>) navigate(tree, directions);
    }

    public static QuadTreeNodeLeaf<Object> navigateLeaf(QuadTreeNode<Object> node, String directions) {
        return (QuadTreeNodeLeaf<Object>) navigate(node, directions);
    }

    public static QuadTreeNodeBranch<Object> navigateBranch(QuadTree<Object> tree, String directions) {
        return (QuadTreeNodeBranch<Object>) navigate(tree, directions);
    }

    public static QuadTreeNode<Object> navigate(QuadTree<Object> tree, String directions) {
        return navigate(tree.getRoot(), directions);
    }

    public static QuadTreeNode<Object> navigate(QuadTreeNode<Object> current, String directions) {
        if (directions.isEmpty()) {
            return current;
        }
        String[] split = directions.split(",");
        for (int i = 0; i < split.length; i++) {
            QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch<Object>) current;
            if (split[i].equals("nw")) {
                current = branch.getNw();
            }
            else if (split[i].equals("ne")) {
                current = branch.getNe();
            }
            else if (split[i].equals("sw")) {
                current = branch.getSw();
            }
            else if (split[i].equals("se")) {
                current = branch.getSe();
            }
            else {
                throw new IllegalArgumentException("Invalid direction " + split[i]);
            }
        }
        return current;
    }

    public static void randomQuery(QuadTree<Object> quadTree, List<SupportPointWithId> points, Random random, double x, double y, double width, double height, SupportQuadTreeUtil.Querier querier) {
        double bbWidth = random.nextDouble() * width * 1.5;
        double bbHeight = random.nextDouble() * height * 1.5;
        double bbMinX = random.nextDouble() * width + x * 0.8;
        double bbMinY = random.nextDouble() * height + y * 0.8;
        Collection<Object> actual = querier.query(quadTree, bbMinX, bbMinY, bbWidth, bbHeight);
        SupportQuadTreeUtil.assertPointsById(points, actual, bbMinX, bbMinY, bbWidth, bbHeight);
    }

    public interface AdderUnique {
        void addOrSet(QuadTree<Object> tree, SupportPointWithId value);
    }

    public interface Remover {
        void removeOrDelete(QuadTree<Object> tree, SupportPointWithId value);
    }

    public interface Querier {
        Collection<Object> query(QuadTree<Object> tree, double x, double y, double width, double height);
    }

    public interface Generator {
        List<SupportPointWithId> generate(Random random, int numPoints, double x, double y, double width, double height);
    }

    public static class GeneratorUniqueDouble implements Generator {

        public final static GeneratorUniqueDouble INSTANCE = new GeneratorUniqueDouble();

        public List<SupportPointWithId> generate(Random random, int numPoints, double x, double y, double width, double height) {
            Map<XYPoint, SupportPointWithId> points = new HashMap<>();
            int pointNum = 0;
            while(points.size() < numPoints) {
                double px = x + width * random.nextDouble();
                double py = y + height * random.nextDouble();
                XYPoint point = new XYPoint(px, py);
                if (points.containsKey(point)) {
                    continue;
                }
                points.put(point, new SupportPointWithId("P" + pointNum, px, py));
                pointNum++;
            }
            return new LinkedList<>(points.values());
        }
    }
}
