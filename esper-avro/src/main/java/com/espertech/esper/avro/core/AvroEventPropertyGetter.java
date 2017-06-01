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
package com.espertech.esper.avro.core;

import com.espertech.esper.event.EventPropertyGetterSPI;
import org.apache.avro.generic.GenericData;

public interface AvroEventPropertyGetter extends EventPropertyGetterSPI {
    Object getAvroFieldValue(GenericData.Record record);

    Object getAvroFragment(GenericData.Record record);

    boolean isExistsPropertyAvro(GenericData.Record record);
}
