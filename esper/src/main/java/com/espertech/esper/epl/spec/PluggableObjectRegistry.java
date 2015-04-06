/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.spec;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.ConfigurationPlugInPatternObject;
import com.espertech.esper.client.ConfigurationPlugInView;
import com.espertech.esper.client.ConfigurationPlugInVirtualDataWindow;
import com.espertech.esper.collection.Pair;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface PluggableObjectRegistry
{
    public Pair<Class,PluggableObjectEntry> lookup(String nameSpace, String name);
}
