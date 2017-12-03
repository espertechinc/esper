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
package com.espertech.esper.spatial.quadtree.mxcifrowindex;

import com.espertech.esper.type.XYWHRectangle;

import java.util.Collection;
import java.util.LinkedList;

public class XYWHRectangleMultiType extends XYWHRectangle {
    private Object multityped;

    public XYWHRectangleMultiType(double x, double y, double w, double h, Object multityped) {
        super(x, y, w, h);
        this.multityped = multityped;
    }

    public Object getMultityped() {
        return multityped;
    }

    public void setMultityped(Object multityped) {
        this.multityped = multityped;
    }

    public int count() {
        if (multityped instanceof Collection) {
            return ((Collection) multityped).size();
        }
        return 1;
    }

    public void addSingleValue(Object value) {
        if (multityped == null) {
            multityped = value;
            return;
        }
        if (multityped instanceof Collection) {
            ((Collection) multityped).add(value);
            return;
        }
        Collection<Object> coll = new LinkedList<>();
        coll.add(multityped);
        coll.add(value);
        multityped = coll;
    }

    public void addMultiType(XYWHRectangleMultiType other) {
        if (other.getX() != x || other.getY() != y) {
            throw new IllegalArgumentException("Coordinate mismatch");
        }
        if (!(other.multityped instanceof Collection)) {
            addSingleValue(other.getMultityped());
            return;
        }

        Collection otherCollection = (Collection) other.multityped;
        if (multityped instanceof Collection) {
            ((Collection) multityped).addAll(otherCollection);
            return;
        }
        Collection<Object> coll = new LinkedList<>();
        coll.add(multityped);
        coll.addAll(otherCollection);
        multityped = coll;
    }

    public void collectInto(Collection<Object> result) {
        if (!(multityped instanceof Collection)) {
            result.add(multityped);
            return;
        }
        result.addAll((Collection) multityped);
    }

    public boolean remove(Object value) {
        if (multityped == null) {
            return false;
        }
        if (multityped.equals(value)) {
            multityped = null;
            return true;
        }
        if (multityped instanceof Collection) {
            return ((Collection) multityped).remove(value);
        }
        return false;
    }

    public boolean isEmpty() {
        return multityped == null || multityped instanceof Collection && ((Collection) multityped).isEmpty();
    }

    public String toString() {
        return "XYWHRectangleMultiType{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + w +
                ", h=" + h +
                ", multityped=" + multityped +
                '}';
    }
}
