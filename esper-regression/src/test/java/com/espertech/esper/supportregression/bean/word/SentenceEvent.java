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
package com.espertech.esper.supportregression.bean.word;

public class SentenceEvent {
    private final String sentence;

    public SentenceEvent(String sentence) {
        this.sentence = sentence;
    }

    public WordEvent[] getWords() {
        String[] split = sentence.split(" ");
        WordEvent[] words = new WordEvent[split.length];
        for (int i = 0; i < split.length; i++) {
            words[i] = new WordEvent(split[i]);
        }
        return words;
    }
}

