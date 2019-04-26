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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

/**
 * Information that the serde applies to a filter.
 */
public class SerdeProviderAdditionalInfoFilter extends SerdeProviderAdditionalInfo {
    /**
     * Ctor
     * @param raw statement information
     */
    public SerdeProviderAdditionalInfoFilter(StatementRawInfo raw) {
        super(raw);
    }
}
