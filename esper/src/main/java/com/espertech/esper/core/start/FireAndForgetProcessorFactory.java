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
package com.espertech.esper.core.start;

import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.epl.spec.StreamSpecCompiled;
import com.espertech.esper.epl.spec.TableQueryStreamSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadata;

public class FireAndForgetProcessorFactory {
    public static FireAndForgetProcessor validateResolveProcessor(StreamSpecCompiled streamSpec, EPServicesContext services)
            throws ExprValidationException {
        // resolve processor
        String processorName;
        if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
            processorName = namedSpec.getWindowName();
        } else {
            TableQueryStreamSpec tableSpec = (TableQueryStreamSpec) streamSpec;
            processorName = tableSpec.getTableName();
        }

        // get processor instance
        TableMetadata tableMetadata = services.getTableService().getTableMetadata(processorName);
        if (tableMetadata != null) {
            return new FireAndForgetProcessorTable(services.getTableService(), tableMetadata);
        } else {
            NamedWindowProcessor nwprocessor = services.getNamedWindowMgmtService().getProcessor(processorName);
            if (nwprocessor == null) {
                throw new ExprValidationException("A table or named window by name '" + processorName + "' does not exist");
            }
            return new FireAndForgetProcessorNamedWindow(nwprocessor);
        }
    }
}
