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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.epl.datetime.reformatop.ReformatForge;

public abstract class DTLocalReformatForgeBase implements DTLocalForge {
    protected final ReformatForge reformatForge;

    protected DTLocalReformatForgeBase(ReformatForge reformatForge) {
        this.reformatForge = reformatForge;
    }
}
