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

import com.espertech.esper.spatial.quadtree.core.SupportRandomIntPointsInSquareUnique;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.rowindex.SupportQuadTreeRowIndexUtil.ROWINDEX_ADDER;
import static com.espertech.esper.spatial.quadtree.rowindex.SupportQuadTreeRowIndexUtil.ROWINDEX_QUERIER;
import static com.espertech.esper.spatial.quadtree.rowindex.SupportQuadTreeRowIndexUtil.ROWINDEX_REMOVER;

public class TestQuadTreeRowIndexRandomIntPointsInSquareUnique extends TestCase {
    public void testRandomIntPoints() {
        SupportRandomIntPointsInSquareUnique.testRandomIntPoints(ROWINDEX_ADDER, ROWINDEX_REMOVER, ROWINDEX_QUERIER);
    }
}
