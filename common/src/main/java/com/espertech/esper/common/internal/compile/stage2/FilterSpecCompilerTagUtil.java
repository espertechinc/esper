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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class FilterSpecCompilerTagUtil {
    public static LinkedHashSet<String> assignEventAsTagNumber(Set<String> priorAllTags, EvalForgeNode evalForgeNode) {
        LinkedHashSet<String> allTagNamesOrdered = new LinkedHashSet<>();
        Set<EvalForgeNode> filterFactoryNodes = EvalNodeUtil.recursiveGetChildNodes(evalForgeNode, StreamSpecCompiler.FilterForFilterFactoryNodes.INSTANCE);
        if (priorAllTags != null) {
            allTagNamesOrdered.addAll(priorAllTags);
        }
        for (EvalForgeNode filterNode : filterFactoryNodes) {
            EvalFilterForgeNode forge = (EvalFilterForgeNode) filterNode;
            int tagNumber;
            if (forge.getEventAsName() != null) {
                if (!allTagNamesOrdered.contains(forge.getEventAsName())) {
                    allTagNamesOrdered.add(forge.getEventAsName());
                    tagNumber = allTagNamesOrdered.size() - 1;
                } else {
                    tagNumber = findTagNumber(forge.getEventAsName(), allTagNamesOrdered);
                }
                forge.setEventAsTagNumber(tagNumber);
            }
        }
        return allTagNamesOrdered;
    }

    public static Set<String> getTagNumbers(EvalForgeNode evalForgeNode) {
        Set<String> tags = new HashSet<>();
        Set<EvalForgeNode> filterFactoryNodes = EvalNodeUtil.recursiveGetChildNodes(evalForgeNode, StreamSpecCompiler.FilterForFilterFactoryNodes.INSTANCE);
        for (EvalForgeNode filterNode : filterFactoryNodes) {
            EvalFilterForgeNode forge = (EvalFilterForgeNode) filterNode;
            if (forge.getEventAsName() != null) {
                tags.add(forge.getEventAsName());
            }
        }
        return tags;
    }

    public static int findTagNumber(String findTag, LinkedHashSet<String> allTagNamesOrdered) {
        int index = 0;
        for (String tag : allTagNamesOrdered) {
            if (findTag.equals(tag)) {
                return index;
            }
            index++;
        }
        throw new EPException("Failed to find tag '" + findTag + "' among known tags");
    }
}
