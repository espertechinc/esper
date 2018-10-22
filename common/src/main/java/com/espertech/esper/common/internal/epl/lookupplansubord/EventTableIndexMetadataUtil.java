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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventTableIndexMetadataUtil {
    public static String[][] getUniqueness(EventTableIndexMetadata indexMetadata, String[] optionalViewUniqueness) {
        List<String[]> unique = null;

        Set<IndexMultiKey> indexDescriptors = indexMetadata.getIndexes().keySet();
        for (IndexMultiKey index : indexDescriptors) {
            if (!index.isUnique()) {
                continue;
            }
            String[] uniqueKeys = IndexedPropDesc.getIndexProperties(index.getHashIndexedProps());
            if (unique == null) {
                unique = new ArrayList<>();
            }
            unique.add(uniqueKeys);
        }
        if (optionalViewUniqueness != null) {
            if (unique == null) {
                unique = new ArrayList<>();
            }
            unique.add(optionalViewUniqueness);
        }
        if (unique == null) {
            return null;
        }
        return unique.toArray(new String[unique.size()][]);
    }
}
