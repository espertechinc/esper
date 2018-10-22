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
package com.espertech.esper.common.internal.epl.table.update;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataEntry;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperNoCopy;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TableUpdateStrategyFactory {
    public static void validateNewUniqueIndex(String[] tableUpdatedProperties, IndexedPropDesc[] hashIndexedProps) {
        for (IndexedPropDesc prop : hashIndexedProps) {
            for (String col : tableUpdatedProperties) {
                if (prop.getIndexPropName().equals(col)) {
                    throw new EPException("Create-index adds a unique key on columns that are updated by one or more on-merge statements");
                }
            }
        }
    }

    public static void validateTableUpdateOnMerge(TableMetaData tableMetadata, String[] updatedProperties)
            throws ExprValidationException {
        IndexUpdateDesc desc = getAffectedIndexes(tableMetadata, updatedProperties);
        if (desc.affectedIndexNames != null && desc.uniqueIndexUpdated) {
            throw new ExprValidationException("On-merge statements may not update unique keys of tables");
        }
    }

    public static TableUpdateStrategy validateGetTableUpdateStrategy(TableMetaData tableMetadata, EventBeanUpdateHelperNoCopy updateHelper, boolean isOnMerge)
            throws ExprValidationException {
        IndexUpdateDesc desc = getAffectedIndexes(tableMetadata, updateHelper.getUpdatedProperties());

        // with affected indexes and with uniqueness : careful updates, may need to rollback
        if (desc.affectedIndexNames != null && desc.uniqueIndexUpdated) {
            if (isOnMerge) {
                throw new ExprValidationException("On-merge statements may not update unique keys of tables");
            }
            return new TableUpdateStrategyWUniqueConstraint(updateHelper, desc.affectedIndexNames);
        }
        // with affected indexes and without uniqueness : update indexes without unique key violation and rollback
        if (desc.affectedIndexNames != null) {
            return new TableUpdateStrategyIndexNonUnique(updateHelper, desc.affectedIndexNames);
        }
        // no affected indexes, the fasted means of updating
        return new TableUpdateStrategyNonIndex(updateHelper);
    }

    private static IndexUpdateDesc getAffectedIndexes(TableMetaData tableMetadata, String[] updatedProperties) {
        Set<String> affectedIndexNames = null;
        boolean uniqueIndexUpdated = false;

        for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> index :
                tableMetadata.getIndexMetadata().getIndexes().entrySet()) {

            for (String updatedProperty : updatedProperties) {
                boolean match = determineUpdatesIndex(updatedProperty, index.getKey());
                if (match) {
                    if (affectedIndexNames == null) {
                        affectedIndexNames = new LinkedHashSet<String>();
                    }
                    affectedIndexNames.add(index.getValue().getOptionalIndexName());
                    uniqueIndexUpdated |= index.getKey().isUnique();
                }
            }
        }

        return new IndexUpdateDesc(affectedIndexNames, uniqueIndexUpdated);
    }

    private static boolean determineUpdatesIndex(String updatedProperty, IndexMultiKey key) {
        for (IndexedPropDesc prop : key.getHashIndexedProps()) {
            if (prop.getIndexPropName().equals(updatedProperty)) {
                return true;
            }
        }
        for (IndexedPropDesc prop : key.getRangeIndexedProps()) {
            if (prop.getIndexPropName().equals(updatedProperty)) {
                return true;
            }
        }
        return false;
    }

    private static class IndexUpdateDesc {
        private final Set<String> affectedIndexNames;
        private final boolean uniqueIndexUpdated;

        public IndexUpdateDesc(Set<String> affectedIndexNames, boolean uniqueIndexUpdated) {
            this.affectedIndexNames = affectedIndexNames;
            this.uniqueIndexUpdated = uniqueIndexUpdated;
        }

        public Set<String> getAffectedIndexNames() {
            return affectedIndexNames;
        }

        public boolean isUniqueIndexUpdated() {
            return uniqueIndexUpdated;
        }
    }
}
