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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.hint.IndexHintInstruction;
import com.espertech.esper.epl.join.hint.IndexHintInstructionBust;
import com.espertech.esper.epl.join.hint.IndexHintInstructionExplicit;
import com.espertech.esper.epl.join.hint.IndexHintInstructionIndexName;
import com.espertech.esper.epl.spec.CreateIndexItem;
import com.espertech.esper.epl.spec.CreateIndexType;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class EventTableIndexUtil {
    private static final Logger log = LoggerFactory.getLogger(EventTableIndexUtil.class);
    private final static IndexComparatorShortestPath INDEX_COMPARATOR_INSTANCE = new IndexComparatorShortestPath();

    public static EventTableCreateIndexDesc validateCompileExplicitIndex(boolean unique, List<CreateIndexItem> columns, EventType eventType)
            throws ExprValidationException {
        List<IndexedPropDesc> hashProps = new ArrayList<IndexedPropDesc>();
        List<IndexedPropDesc> btreeProps = new ArrayList<IndexedPropDesc>();

        Set<String> indexed = new HashSet<String>();
        for (CreateIndexItem columnDesc : columns) {
            String columnName = columnDesc.getName();

            Class type = JavaClassHelper.getBoxedType(eventType.getPropertyType(columnName));
            if (type == null) {
                throw new ExprValidationException("Property named '" + columnName + "' not found");
            }
            if (!indexed.add(columnName)) {
                throw new ExprValidationException("Property named '" + columnName + "' has been declared more then once");
            }

            IndexedPropDesc desc = new IndexedPropDesc(columnName, type);
            if (columnDesc.getType() == CreateIndexType.HASH) {
                hashProps.add(desc);
            } else {
                btreeProps.add(desc);
            }
        }

        if (unique && !btreeProps.isEmpty()) {
            throw new ExprValidationException("Combination of unique index with btree (range) is not supported");
        }
        return new EventTableCreateIndexDesc(hashProps, btreeProps, unique);
    }

    public static IndexMultiKey findIndexConsiderTyping(Map<IndexMultiKey, EventTableIndexMetadataEntry> tableIndexesRefCount,
                                                        List<IndexedPropDesc> hashProps,
                                                        List<IndexedPropDesc> btreeProps,
                                                        List<IndexHintInstruction> optionalIndexHintInstructions) {

        if (hashProps.isEmpty() && btreeProps.isEmpty()) {
            throw new IllegalArgumentException("Invalid zero element list for hash and btree columns");
        }

        Map<IndexMultiKey, EventTableIndexRepositoryEntry> indexCandidates = (Map<IndexMultiKey, EventTableIndexRepositoryEntry>) EventTableIndexUtil.findCandidates(tableIndexesRefCount, hashProps, btreeProps);

        // if there are hints, follow these
        if (optionalIndexHintInstructions != null) {
            IndexMultiKey found = EventTableIndexUtil.findByIndexHint(indexCandidates, optionalIndexHintInstructions);
            if (found != null) {
                return found;
            }
        }

        // Get an existing table, if any, matching the exact requirement, prefer unique
        IndexMultiKey indexPropKeyMatch = EventTableIndexUtil.findExactMatchNameAndType(tableIndexesRefCount.keySet(), true, hashProps, btreeProps);
        if (indexPropKeyMatch == null) {
            indexPropKeyMatch = EventTableIndexUtil.findExactMatchNameAndType(tableIndexesRefCount.keySet(), false, hashProps, btreeProps);
        }
        if (indexPropKeyMatch != null) {
            return indexPropKeyMatch;
        }

        if (indexCandidates.isEmpty()) {
            return null;
        }

        return getBestCandidate((Map<IndexMultiKey, EventTableIndexEntryBase>) (Map) indexCandidates).getFirst();
    }

    public static Pair<IndexMultiKey, EventTableIndexEntryBase> findIndexBestAvailable(Map<IndexMultiKey, ? extends EventTableIndexEntryBase> tablesAvailable,
                                                                                       Set<String> keyPropertyNames,
                                                                                       Set<String> rangePropertyNames,
                                                                                       List<IndexHintInstruction> optionalIndexHintInstructions) {
        if (keyPropertyNames.isEmpty() && rangePropertyNames.isEmpty()) {
            return null;
        }

        // determine candidates
        List<IndexedPropDesc> hashProps = new ArrayList<IndexedPropDesc>();
        for (String keyPropertyName : keyPropertyNames) {
            hashProps.add(new IndexedPropDesc(keyPropertyName, null));
        }
        List<IndexedPropDesc> rangeProps = new ArrayList<IndexedPropDesc>();
        for (String rangePropertyName : rangePropertyNames) {
            rangeProps.add(new IndexedPropDesc(rangePropertyName, null));
        }
        Map<IndexMultiKey, EventTableIndexEntryBase> indexCandidates = (Map<IndexMultiKey, EventTableIndexEntryBase>) EventTableIndexUtil.findCandidates(tablesAvailable, hashProps, rangeProps);

        // handle hint
        if (optionalIndexHintInstructions != null) {
            IndexMultiKey found = EventTableIndexUtil.findByIndexHint(indexCandidates, optionalIndexHintInstructions);
            if (found != null) {
                return getPair(tablesAvailable, found);
            }
        }

        // no candidates
        if (indexCandidates == null || indexCandidates.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("No index found.");
            }
            return null;
        }

        return getBestCandidate(indexCandidates);
    }

    private static Pair<IndexMultiKey, EventTableIndexEntryBase> getBestCandidate(Map<IndexMultiKey, EventTableIndexEntryBase> indexCandidates) {
        // take the table that has a unique index
        List<IndexMultiKey> indexes = new ArrayList<IndexMultiKey>();
        for (Map.Entry<IndexMultiKey, EventTableIndexEntryBase> entry : indexCandidates.entrySet()) {
            if (entry.getKey().isUnique()) {
                indexes.add(entry.getKey());
            }
        }
        if (!indexes.isEmpty()) {
            Collections.sort(indexes, INDEX_COMPARATOR_INSTANCE);
            return getPair(indexCandidates, indexes.get(0));
        }

        // take the best available table
        indexes.clear();
        indexes.addAll(indexCandidates.keySet());
        if (indexes.size() > 1) {
            Collections.sort(indexes, INDEX_COMPARATOR_INSTANCE);
        }
        return getPair(indexCandidates, indexes.get(0));
    }

    public static IndexMultiKey findByIndexHint(Map<IndexMultiKey, ? extends EventTableIndexEntryBase> indexCandidates, List<IndexHintInstruction> instructions) {
        for (IndexHintInstruction instruction : instructions) {
            if (instruction instanceof IndexHintInstructionIndexName) {
                String indexName = ((IndexHintInstructionIndexName) instruction).getIndexName();
                IndexMultiKey found = findExplicitIndexByName(indexCandidates, indexName);
                if (found != null) {
                    return found;
                }
            }
            if (instruction instanceof IndexHintInstructionExplicit) {
                IndexMultiKey found = findExplicitIndexAnyName(indexCandidates);
                if (found != null) {
                    return found;
                }
            }
            if (instruction instanceof IndexHintInstructionBust) {
                throw new EPException("Failed to plan index access, index hint busted out");
            }
        }
        return null;
    }

    public static IndexMultiKey findExactMatchNameAndType(Set<IndexMultiKey> indexMultiKeys, boolean unique, List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps) {
        for (IndexMultiKey existing : indexMultiKeys) {
            if (isExactMatch(existing, unique, hashProps, btreeProps)) {
                return existing;
            }
        }
        return null;
    }

    private static Map<IndexMultiKey, ? extends EventTableIndexEntryBase> findCandidates(Map<IndexMultiKey, ? extends EventTableIndexEntryBase> indexes, List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps) {
        Map<IndexMultiKey, EventTableIndexEntryBase> indexCandidates = new HashMap<IndexMultiKey, EventTableIndexEntryBase>();
        for (Map.Entry<IndexMultiKey, ? extends EventTableIndexEntryBase> entry : indexes.entrySet()) {
            boolean matches = indexMatchesProvided(entry.getKey(), hashProps, btreeProps);
            if (matches) {
                indexCandidates.put(entry.getKey(), entry.getValue());
            }
        }
        return indexCandidates;
    }

    private static IndexMultiKey findExplicitIndexByName(Map<IndexMultiKey, ? extends EventTableIndexEntryBase> indexCandidates, String name) {
        for (Map.Entry<IndexMultiKey, ? extends EventTableIndexEntryBase> entry : indexCandidates.entrySet()) {
            if (entry.getValue().getOptionalIndexName() != null && entry.getValue().getOptionalIndexName().equals(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static IndexMultiKey findExplicitIndexAnyName(Map<IndexMultiKey, ? extends EventTableIndexEntryBase> indexCandidates) {
        for (Map.Entry<IndexMultiKey, ? extends EventTableIndexEntryBase> entry : indexCandidates.entrySet()) {
            if (entry.getValue().getOptionalIndexName() != null) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean indexHashIsProvided(IndexedPropDesc hashPropIndexed, List<IndexedPropDesc> hashPropsProvided) {
        for (IndexedPropDesc hashPropProvided : hashPropsProvided) {
            boolean nameMatch = hashPropProvided.getIndexPropName().equals(hashPropIndexed.getIndexPropName());
            boolean typeMatch = true;
            if (hashPropProvided.getCoercionType() != null && !JavaClassHelper.isSubclassOrImplementsInterface(JavaClassHelper.getBoxedType(hashPropProvided.getCoercionType()), JavaClassHelper.getBoxedType(hashPropIndexed.getCoercionType()))) {
                typeMatch = false;
            }
            if (nameMatch && typeMatch) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExactMatch(IndexMultiKey existing, boolean unique, List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps) {
        if (existing.isUnique() != unique) {
            return false;
        }
        boolean keyPropCompare = IndexedPropDesc.compare(Arrays.asList(existing.getHashIndexedProps()), hashProps);
        return keyPropCompare && IndexedPropDesc.compare(Arrays.asList(existing.getRangeIndexedProps()), btreeProps);
    }

    private static boolean indexMatchesProvided(IndexMultiKey indexDesc, List<IndexedPropDesc> hashPropsProvided, List<IndexedPropDesc> rangePropsProvided) {
        IndexedPropDesc[] hashPropIndexedList = indexDesc.getHashIndexedProps();
        for (IndexedPropDesc hashPropIndexed : hashPropIndexedList) {
            boolean foundHashProp = indexHashIsProvided(hashPropIndexed, hashPropsProvided);
            if (!foundHashProp) {
                return false;
            }
        }

        IndexedPropDesc[] rangePropIndexedList = indexDesc.getRangeIndexedProps();
        for (IndexedPropDesc rangePropIndexed : rangePropIndexedList) {
            boolean foundRangeProp = indexHashIsProvided(rangePropIndexed, rangePropsProvided);
            if (!foundRangeProp) {
                return false;
            }
        }

        return true;
    }

    private static Pair<IndexMultiKey, EventTableIndexEntryBase> getPair(Map<IndexMultiKey, ? extends EventTableIndexEntryBase> tableIndexesRefCount, IndexMultiKey indexMultiKey) {
        EventTableIndexEntryBase indexFound = tableIndexesRefCount.get(indexMultiKey);
        return new Pair<IndexMultiKey, EventTableIndexEntryBase>(indexMultiKey, indexFound);
    }

    private static class IndexComparatorShortestPath implements Comparator<IndexMultiKey>, Serializable {
        private static final long serialVersionUID = -2214412607714095566L;

        public int compare(IndexMultiKey o1, IndexMultiKey o2) {
            String[] indexedProps1 = IndexedPropDesc.getIndexProperties(o1.getHashIndexedProps());
            String[] indexedProps2 = IndexedPropDesc.getIndexProperties(o2.getHashIndexedProps());
            if (indexedProps1.length > indexedProps2.length) {
                return 1;  // sort desc by count columns
            }
            if (indexedProps1.length == indexedProps2.length) {
                return 0;
            }
            return -1;
        }
    }
}
