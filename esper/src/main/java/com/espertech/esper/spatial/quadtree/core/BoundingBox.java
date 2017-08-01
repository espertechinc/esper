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

public class BoundingBox {
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public BoundingBox(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public boolean containsPoint(double x, double y) {
        return x >= minX && y >= minY && x < maxX && y < maxY;
    }

    public boolean intersectsBoxIncludingEnd(double x, double y, double width, double height) {
        return intersectsBoxIncludingEnd(minX, minY, maxX, maxY, x, y, width, height);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param minX min-x
     * @param minY min-y
     * @param maxX max-x
     * @param maxY max-y
     * @param otherX x
     * @param otherY y
     * @param otherWidth w
     * @param otherHeight h
     * @return flag
     */
    public static boolean intersectsBoxIncludingEnd(double minX, double minY, double maxX, double maxY, double otherX, double otherY, double otherWidth, double otherHeight) {
        double otherMaxX = otherX + otherWidth;
        double otherMaxY = otherY + otherHeight;
        if (maxX < otherX) return false; // a is left of b
        if (minX > otherMaxX) return false; // a is right of b
        if (maxY < otherY) return false; // a is above b
        if (minY > otherMaxY) return false; // a is below b
        return true; // boxes overlap
    }

    public static boolean containsPoint(double x, double y, double width, double height, double px, double py) {
        if (px >= x + width) return false;
        if (px < x) return false;
        if (py >= y + height) return false;
        if (py < y) return false;
        return true;
    }

    public String toString() {
        return "{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                '}';
    }

    public QuadrantEnum getQuadrant(double x, double y) {
        double deltaX = x - minX;
        double deltaY = y - minY;
        double halfWidth = (maxX - minX) / 2;
        double halfHeight = (maxY - minY) / 2;
        if (deltaX < halfWidth) {
            return deltaY < halfHeight ? QuadrantEnum.NW : QuadrantEnum.SW;
        }
        return deltaY < halfHeight ? QuadrantEnum.NE : QuadrantEnum.SE;
    }

    public QuadrantAppliesEnum getQuadrantApplies(double x, double y, double w, double h) {
        double deltaX = x - minX;
        double deltaY = y - minY;
        double halfWidth = (maxX - minX) / 2;
        double halfHeight = (maxY - minY) / 2;
        double midX = minX + halfWidth;
        double midY = minY + halfHeight;
        if (deltaX < halfWidth) {
            if (deltaY < halfHeight) {
                // x,y is NW world
                if (x + w < minX || y + h < minY) {
                    return QuadrantAppliesEnum.NONE;
                }
                if (x + w >= midX || y + h >= midY) {
                    return QuadrantAppliesEnum.SOME;
                }
                return QuadrantAppliesEnum.NW;
            } else {
                if (y > maxY || x + w < minX) {
                    return QuadrantAppliesEnum.NONE;
                }
                if (x + w >= midX || y <= midY) {
                    return QuadrantAppliesEnum.SOME;
                }
                return QuadrantAppliesEnum.SW;
            }
        }
        if (deltaY < halfHeight) {
            // x,y is NE world
            if (x > maxX || y + h < minY) {
                return QuadrantAppliesEnum.NONE;
            }
            if (x <= midX || y + h >= midY) {
                return QuadrantAppliesEnum.SOME;
            }
            return QuadrantAppliesEnum.NE;
        } else {
            if (x > maxX || y > maxY) {
                return QuadrantAppliesEnum.NONE;
            }
            if (x <= midX || y <= midY) {
                return QuadrantAppliesEnum.SOME;
            }
            return QuadrantAppliesEnum.SE;
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingBox that = (BoundingBox) o;

        if (Double.compare(that.minX, minX) != 0) return false;
        if (Double.compare(that.minY, minY) != 0) return false;
        if (Double.compare(that.maxX, maxX) != 0) return false;
        return Double.compare(that.maxY, maxY) == 0;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(minX);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minY);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxX);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxY);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public BoundingBox[] subdivide() {
        double w = (maxX - minX) / 2d;
        double h = (maxY - minY) / 2d;

        BoundingBox bbNW = new BoundingBox(minX, minY, minX + w, minY + h);
        BoundingBox bbNE = new BoundingBox(minX + w, minY, maxX, minY + h);
        BoundingBox bbSW = new BoundingBox(minX, minY + h, minX + w, maxY);
        BoundingBox bbSE = new BoundingBox(minX + w, minY + h, maxX, maxY);
        return new BoundingBox[]{bbNW, bbNE, bbSW, bbSE};
    }

    public BoundingBoxNode treeForDepth(int depth) {
        BoundingBoxNode[] quadrants = new BoundingBoxNode[4];
        if (depth > 0) {
            BoundingBox[] subs = subdivide();
            quadrants[0] = subs[0].treeForDepth(depth - 1);
            quadrants[1] = subs[1].treeForDepth(depth - 1);
            quadrants[2] = subs[2].treeForDepth(depth - 1);
            quadrants[3] = subs[3].treeForDepth(depth - 1);
        }
        return new BoundingBoxNode(this, quadrants[0], quadrants[1], quadrants[2], quadrants[3]);
    }

    public static BoundingBox from(double x, double y, double width, double height) {
        return new BoundingBox(x, y, x + width, y + height);
    }

    public BoundingBoxNode treeForPath(String[] path) {
        return treeForPath(path, 0);
    }

    private BoundingBoxNode treeForPath(String[] path, int offset) {
        BoundingBoxNode[] quadrants = new BoundingBoxNode[4];
        if (offset < path.length) {
            BoundingBox[] subs = subdivide();
            String q = path[offset];
            if (q.equals("nw")) {
                quadrants[0] = subs[0].treeForPath(path, offset + 1);
            }
            if (q.equals("ne")) {
                quadrants[1] = subs[1].treeForPath(path, offset + 1);
            }
            if (q.equals("sw")) {
                quadrants[2] = subs[2].treeForPath(path, offset + 1);
            }
            if (q.equals("se")) {
                quadrants[3] = subs[3].treeForPath(path, offset + 1);
            }
        }
        return new BoundingBoxNode(this, quadrants[0], quadrants[1], quadrants[2], quadrants[3]);
    }

    public static class BoundingBoxNode {
        public final BoundingBox bb;
        public final BoundingBoxNode nw;
        public final BoundingBoxNode ne;
        public final BoundingBoxNode sw;
        public final BoundingBoxNode se;

        public BoundingBoxNode(BoundingBox bb, BoundingBoxNode nw, BoundingBoxNode ne, BoundingBoxNode sw, BoundingBoxNode se) {
            this.bb = bb;
            this.nw = nw;
            this.ne = ne;
            this.sw = sw;
            this.se = se;
        }

        public BoundingBoxNode getQuadrant(QuadrantEnum q) {
            if (q == QuadrantEnum.NW) {
                return nw;
            } else if (q == QuadrantEnum.NE) {
                return ne;
            } else if (q == QuadrantEnum.SW) {
                return sw;
            } else {
                return se;
            }
        }
    }
}
