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

import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.AdderNonUnique;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.Factory;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.Querier;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.Remover;

import static com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil.Generator;

public class SupportQuadTreeToolNonUnique<L> {
    public Factory<L> factory;
    public Generator generator;
    public AdderNonUnique<L> adderNonUnique;
    public Remover<L> remover;
    public Querier<L> querier;
    public boolean pointInsideChecking;

    public SupportQuadTreeToolNonUnique(Factory<L> factory, Generator generator, AdderNonUnique<L> adderNonUnique, Remover<L> remover, Querier<L> querier, boolean pointInsideChecking) {
        this.factory = factory;
        this.generator = generator;
        this.adderNonUnique = adderNonUnique;
        this.remover = remover;
        this.querier = querier;
        this.pointInsideChecking = pointInsideChecking;
    }
}
