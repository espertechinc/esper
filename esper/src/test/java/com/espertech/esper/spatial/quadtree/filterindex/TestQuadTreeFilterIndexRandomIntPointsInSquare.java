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

import com.espertech.esper.spatial.quadtree.core.SupportRandomIntPointsInSquareUnique;
import junit.framework.TestCase;

import static com.espertech.esper.spatial.quadtree.filterindex.SupportQuadTreeFilterIndexUtil.*;

public class TestQuadTreeFilterIndexRandomIntPointsInSquare extends TestCase {
    public void testRandomIntPoints() {
        SupportRandomIntPointsInSquareUnique.testRandomIntPoints(FILTERINDEX_ADDER, FILTERINDEX_REMOVER, FILTERINDEX_QUERIER);
    }
}
