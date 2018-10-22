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
package com.espertech.esper.common.internal.epl.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;

/**
 * Marker interface for compile-time resolvers
 */
public interface CompileTimeResolver {
    static <T> T validateAmbiguous(T local, T path, T preconfigured, PathRegistryObjectType objectType, String name) {
        if (path != null && preconfigured != null) {
            throw new EPException("The " + objectType.getName() + " by name '" + name + "' is ambiguous as it exists in both the path space and the preconfigured space");
        }
        if (local != null) {
            if (path != null || preconfigured != null) {
                // This should not happen as any create-XXX has checked whether if it already exists; handle it anyway
                throw new EPException("The " + objectType.getName() + " by name '" + name + "' is ambiguous as it exists in both the local space and the path or preconfigured space");
            }
            return local;
        }
        return path != null ? path : preconfigured;
    }

    static EPException makePathAmbiguous(PathRegistryObjectType objectType, String name, PathException e) {
        return new EPException("The " + objectType.getName() + " by name '" + name + "' is ambiguous as it exists for multiple modules: " + e.getMessage(), e);
    }
}
