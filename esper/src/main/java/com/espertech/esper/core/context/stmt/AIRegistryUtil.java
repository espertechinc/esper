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
package com.espertech.esper.core.context.stmt;

import com.espertech.esper.collection.ArrayWrap;

public class AIRegistryUtil {
    public static void checkExpand(int serviceId, ArrayWrap services) {
        if (serviceId > services.getArray().length - 1) {
            int delta = serviceId - services.getArray().length + 1;
            services.expand(delta);
        }
    }
}
