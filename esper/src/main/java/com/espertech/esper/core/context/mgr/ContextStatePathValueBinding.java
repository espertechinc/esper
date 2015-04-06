/**************************************************************************************
 * Copyright (C) 2007 EsperTech Inc. All rights reserved.                             *
 * http://www.espertech.com                                                           *
 **************************************************************************************/
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.event.EventAdapterService;

public interface ContextStatePathValueBinding
{
    public Object byteArrayToObject(byte[] bytes, EventAdapterService eventAdapterService);
    public byte[] toByteArray(Object contextInfo);
}

