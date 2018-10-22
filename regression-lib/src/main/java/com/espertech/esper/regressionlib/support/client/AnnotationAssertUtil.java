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
package com.espertech.esper.regressionlib.support.client;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class AnnotationAssertUtil {
    public static Annotation[] sortAlpha(Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }
        ArrayList<Annotation> sorted = new ArrayList<>();
        sorted.addAll(Arrays.asList(annotations));
        Collections.sort(sorted, new Comparator<Annotation>() {
            public int compare(Annotation o1, Annotation o2) {
                return o1.annotationType().getSimpleName().compareTo(o2.annotationType().getSimpleName());
            }
        });
        return sorted.toArray(new Annotation[sorted.size()]);
    }
}
