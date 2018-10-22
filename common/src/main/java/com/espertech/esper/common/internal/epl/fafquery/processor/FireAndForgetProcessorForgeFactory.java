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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.TableQueryStreamSpec;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

public class FireAndForgetProcessorForgeFactory {
    public static FireAndForgetProcessorForge validateResolveProcessor(StreamSpecCompiled streamSpec) {
        if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowMetaData nwdetail = ((NamedWindowConsumerStreamSpec) streamSpec).getNamedWindow();
            return new FireAndForgetProcessorNamedWindowForge(nwdetail);
        }
        TableQueryStreamSpec tableSpec = (TableQueryStreamSpec) streamSpec;
        return new FireAndForgetProcessorTableForge(tableSpec.getTable());
    }
}
