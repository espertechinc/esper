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
package com.espertech.esper.util;

/**
 * Copyright 2010 The Apache Software Foundation
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Original file: org/apache/hadoop/hbase/util/MurmurHash.java
 */

/**
 * Original file: org/apache/hadoop/hbase/util/MurmurHash.java
 */

/**
 * This is a very fast, non-cryptographic hash suitable for general hash-based
 * lookup.  See http://murmurhash.googlepages.com/ for more details.
 *
 * <p>The C version of MurmurHash 2.0 found at that site was ported
 * to Java by Andrzej Bialecki (ab at getopt org).</p>
 */
public class MurmurHash {

    public static int hash(byte[] data, int offset, int length, int seed) {
        int m = 0x5bd1e995;
        int r = 24;

        int h = seed ^ length;

        int len4 = length >> 2;

        for (int i = 0; i < len4; i++) {
            int i4 = (i << 2) + offset;
            int k = data[i4 + 3];
            k = k << 8;
            k = k | (data[i4 + 2] & 0xff);
            k = k << 8;
            k = k | (data[i4 + 1] & 0xff);
            k = k << 8;
            //noinspection PointlessArithmeticExpression
            k = k | (data[i4 + 0] & 0xff);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        // avoid calculating modulo
        int lenM = len4 << 2;
        int left = length - lenM;
        int iM = lenM + offset;

        if (left != 0) {
            if (left >= 3) {
                h ^= data[iM + 2] << 16;
            }
            if (left >= 2) {
                h ^= data[iM + 1] << 8;
            }
            if (left >= 1) {
                h ^= data[iM];
            }

            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }
}