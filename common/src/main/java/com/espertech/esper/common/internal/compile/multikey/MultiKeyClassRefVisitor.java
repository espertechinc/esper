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
package com.espertech.esper.common.internal.compile.multikey;

public interface MultiKeyClassRefVisitor<T> {
    T visit(MultiKeyClassRefUUIDBased mk);
    T visit(MultiKeyClassRefPredetermined mk);
    T visit(MultiKeyClassRefEmpty mk);
    T visit(MultiKeyClassRefWSerde mk);
}
