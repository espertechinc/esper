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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.internal.rettype.EPChainableType;

public class EnumForgeDesc {
    private final EPChainableType type;
    private final EnumForge forge;

    public EnumForgeDesc(EPChainableType type, EnumForge forge) {
        this.type = type;
        this.forge = forge;
    }

    public EPChainableType getType() {
        return type;
    }

    public EnumForge getForge() {
        return forge;
    }
}
