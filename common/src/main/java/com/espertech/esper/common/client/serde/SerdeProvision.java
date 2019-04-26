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

import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * For use with high-availability and scale-out only, this class provides information to the compiler how to
 * resolve the serializer and de-serializer (serde) at deployment-time.
 */
public abstract class SerdeProvision {
    /**
     * Convert to serde forge
     * @return serde forge
     */
    public abstract DataInputOutputSerdeForge toForge();
}
