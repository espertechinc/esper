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
package com.espertech.esper.common.internal.epl.spatial.quadtree.core;

public class SupportQuadTreeToolNonUnique<L> {
    public SupportQuadTreeUtil.Factory<L> factory;
    public SupportQuadTreeUtil.Generator generator;
    public SupportQuadTreeUtil.AdderNonUnique<L> adderNonUnique;
    public SupportQuadTreeUtil.Remover<L> remover;
    public SupportQuadTreeUtil.Querier<L> querier;
    public boolean pointInsideChecking;

    public SupportQuadTreeToolNonUnique(SupportQuadTreeUtil.Factory<L> factory, SupportQuadTreeUtil.Generator generator, SupportQuadTreeUtil.AdderNonUnique<L> adderNonUnique, SupportQuadTreeUtil.Remover<L> remover, SupportQuadTreeUtil.Querier<L> querier, boolean pointInsideChecking) {
        this.factory = factory;
        this.generator = generator;
        this.adderNonUnique = adderNonUnique;
        this.remover = remover;
        this.querier = querier;
        this.pointInsideChecking = pointInsideChecking;
    }
}
