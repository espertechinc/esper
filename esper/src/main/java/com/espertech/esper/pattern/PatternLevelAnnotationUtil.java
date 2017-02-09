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
package com.espertech.esper.pattern;

import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.epl.spec.PatternStreamSpecRaw;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class PatternLevelAnnotationUtil {

    private final static String DISCARDPARTIALSONMATCH = "DiscardPartialsOnMatch";
    private final static String SUPPRESSOVERLAPPINGMATCHES = "SuppressOverlappingMatches";

    public static AnnotationPart[] annotationsFromSpec(PatternStreamSpecRaw pattern) {
        Deque<AnnotationPart> parts = null;

        if (pattern.isDiscardPartialsOnMatch()) {
            parts = new ArrayDeque<AnnotationPart>();
            parts.add(new AnnotationPart(DISCARDPARTIALSONMATCH));
        }

        if (pattern.isSuppressSameEventMatches()) {
            if (parts == null) {
                parts = new ArrayDeque<AnnotationPart>();
            }
            parts.add(new AnnotationPart(SUPPRESSOVERLAPPINGMATCHES));
        }

        if (parts == null) {
            return null;
        }
        return parts.toArray(new AnnotationPart[parts.size()]);
    }

    public static PatternLevelAnnotationFlags annotationsToSpec(AnnotationPart[] parts) {
        PatternLevelAnnotationFlags flags = new PatternLevelAnnotationFlags();
        if (parts == null) {
            return flags;
        }
        for (AnnotationPart part : parts) {
            validateSetFlags(flags, part.getName());
        }
        return flags;
    }

    public static void validateSetFlags(PatternLevelAnnotationFlags flags, String annotation) {
        if (annotation.toLowerCase(Locale.ENGLISH).equals(DISCARDPARTIALSONMATCH.toLowerCase(Locale.ENGLISH))) {
            flags.setDiscardPartialsOnMatch(true);
        } else if (annotation.toLowerCase(Locale.ENGLISH).equals(SUPPRESSOVERLAPPINGMATCHES.toLowerCase(Locale.ENGLISH))) {
            flags.setSuppressSameEventMatches(true);
        } else {
            throw new IllegalArgumentException("Unrecognized pattern-level annotation '" + annotation + "'");
        }
    }
}
