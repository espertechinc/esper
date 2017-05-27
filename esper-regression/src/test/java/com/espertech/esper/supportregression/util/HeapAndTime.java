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
package com.espertech.esper.supportregression.util;

public class HeapAndTime {

    private final long thenHeapSize = Runtime.getRuntime().totalMemory();
    private final long thenHeapMaxSize = Runtime.getRuntime().maxMemory();
    private final long thenHeapFreeSize = Runtime.getRuntime().freeMemory();
    private final long thenMsec = System.currentTimeMillis();
    private final long thenNano = System.nanoTime();

    public HeapAndTime() {
    }

    public String report() {
        long deltaHeapSize = Runtime.getRuntime().totalMemory() - thenHeapSize;
        long deltaHeapMaxSize = Runtime.getRuntime().maxMemory() - thenHeapMaxSize;
        long deltaHeapFreeSize = Runtime.getRuntime().freeMemory() - thenHeapFreeSize;
        long deltaMsec = System.currentTimeMillis() - thenMsec;
        long deltaNano = System.nanoTime() - thenNano;

        return "Delta: Sec " + deltaMsec / 1000d +
                "  SecHres " + deltaNano / 1000000000d +
                "  MaxMB " + deltaHeapMaxSize / 1024d / 1024d +
                "  FreeMB " + deltaHeapFreeSize / 1024d / 1024d +
                "  SizeMB " + deltaHeapSize +
                "  (heap max " + thenHeapMaxSize / 1024d / 1024d + ")";
    }
}
