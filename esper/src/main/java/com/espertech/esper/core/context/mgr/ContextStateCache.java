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
package com.espertech.esper.core.context.mgr;

import java.util.TreeMap;

public interface ContextStateCache {

    public ContextStatePathValueBinding getBinding(Object bindingInfo);

    public void addContextPath(String contextName, int level, int parentPath, int subPath, Integer optionalContextPartitionId, Object additionalInfo, ContextStatePathValueBinding binding);

    public void updateContextPath(String contextName, ContextStatePathKey key, ContextStatePathValue value);

    public void removeContextParentPath(String contextName, int level, int parentPath);

    public void removeContextPath(String contextName, int level, int parentPath, int subPath);

    public void removeContext(String contextName);

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getContextPaths(String contextName);
}
