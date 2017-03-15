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
package com.espertech.esperio.csv;

import com.espertech.esper.client.EPException;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A source that processes a CSV file and returns CSV records
 * from that file.
 */
public class CSVReader {
    private static final Logger log = LoggerFactory.getLogger(CSVReader.class);

    private boolean looping;
    private boolean isUsingTitleRow;
    private final CSVSource source;

    private final List<String> values = new ArrayList<String>();
    private boolean isClosed = false;
    private boolean atEOF = false;
    private boolean isReset = true;


    /**
     * Ctor.
     *
     * @param adapterInputSource - the source of the CSV file
     * @throws EPException in case of errors in reading the CSV file
     */
    public CSVReader(AdapterInputSource adapterInputSource) throws EPException {
        if (adapterInputSource == null) {
            throw new NullPointerException("AdapterInputSource cannot be null");
        }
        this.source = new CSVSource(adapterInputSource);
    }

    /**
     * Close the source and release the input source.
     *
     * @throws EPException in case of error in closing resources
     */
    public void close() throws EPException {
        if (isClosed) {
            throw new EPException("Calling close() on an already closed CSVReader");
        }
        try {
            isClosed = true;
            source.close();
        } catch (IOException e) {
            throw new EPException(e);
        }
    }

    /**
     * Get the next record from the CSV file.
     *
     * @return a string array containing the values of the record
     * @throws EOFException in case no more records can be read (end-of-file has been reached and isLooping is false)
     * @throws EPException  in case of error in reading the CSV file
     */
    public String[] getNextRecord() throws EOFException, EPException {
        try {
            String[] result = getNextValidRecord();

            if (atEOF && result == null) {
                throw new EOFException("In reading CSV file, reached end-of-file and not looping to the beginning");
            }

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".getNextRecord record==" + Arrays.asList(result));
            }
            return result;
        } catch (EOFException e) {
            throw e;
        } catch (IOException e) {
            throw new EPException(e);
        }
    }

    /**
     * Set the isUsingTitleRow value.
     *
     * @param isUsingTitleRow - true if the CSV file contains a valid title row
     */
    public void setIsUsingTitleRow(boolean isUsingTitleRow) {
        this.isUsingTitleRow = isUsingTitleRow;
    }

    /**
     * Set the looping value.
     *
     * @param looping - true if processing should start over from the beginning after the end of the CSV file is reached
     */
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    /**
     * Reset the source to the beginning of the file.
     *
     * @throws EPException in case of errors in resetting the source
     */
    public void reset() {
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".reset");
            }
            source.reset();
            atEOF = false;
            if (isUsingTitleRow) {
                // Ignore the title row
                getNextRecord();
            }
            isReset = true;
        } catch (IOException e) {
            throw new EPException(e);
        }
    }

    /**
     * Return and set to false the isReset value, which is set to
     * true whenever the CSVReader is reset.
     *
     * @return isReset
     */
    public boolean getAndClearIsReset() {
        boolean result = isReset;
        isReset = false;
        return result;
    }

    /**
     * Return true if this CSVReader supports the reset() method.
     *
     * @return true if the underlying AdapterInputSource is resettable
     */
    public boolean isResettable() {
        return source.isResettable();
    }

    private String[] getNextValidRecord() throws IOException {
        String[] result = null;

        // Search for a valid record to the end of the CSV file
        result = getNoCommentNoWhitespace();

        // If haven't found a valid record and at the end of the
        // file and looping, search from the beginning of the file
        if (result == null && atEOF && looping) {
            reset();
            result = getNoCommentNoWhitespace();
        }

        return result;
    }

    private String[] getNoCommentNoWhitespace() throws IOException {
        String[] result = null;
        // This loop serves to filter out commented lines and
        //lines that contain only whitespace
        while (result == null && !atEOF) {
            skipCommentedLines();
            result = getNewValues();
        }
        return result;
    }

    private String[] getNewValues() throws IOException {
        values.clear();
        boolean doConsume = true;

        while (true) {
            String value = matchValue();

            if (atComma(doConsume)) {
                addNonFinalValue(value);
                continue;
            } else if (atNewline(doConsume) || atEOF(doConsume)) {
                addFinalValue(value);
                break;
            } else {
                throw unexpectedCharacterException((char) source.read());
            }
        }

        // All values empty means that this line was just whitespace
        return values.isEmpty() ? null : values.toArray(new String[0]);
    }

    private void addNonFinalValue(String value) {
        // Represent empty values as empty strings
        value = (value == null) ? "" : value;
        values.add(value);
    }

    private void addFinalValue(String value) {
        // Add this value only if it is nonempty or if it is the
        // last value of a nonempty record.
        if (value != null) {
            values.add(value);
        } else {
            if (!values.isEmpty()) {
                values.add("");
            }
        }
    }

    private boolean atNewline(boolean doConsume) throws IOException {
        return atWinNewline(doConsume) || atChar('\n', doConsume) || atChar('\r', doConsume);
    }

    private boolean atWinNewline(boolean doConsume) throws IOException {
        markReader(2, doConsume);

        char firstChar = (char) source.read();
        char secondChar = (char) source.read();
        boolean result = firstChar == '\r' && secondChar == '\n';

        resetReader(doConsume, result);
        return result;
    }

    private boolean atChar(char character, boolean doConsume) throws IOException {
        markReader(1, doConsume);

        char firstChar = (char) source.read();
        boolean result = firstChar == character;

        resetReader(doConsume, result);
        return result;
    }

    private void resetReader(boolean doConsume, boolean result) throws IOException {
        // Reset the source unless in consuming mode and the
        // matched character was what was expected
        if (!(doConsume && result)) {
            source.resetToMark();
        }
    }

    private void markReader(int markLimit, boolean doConsume) throws IOException {
        source.mark(markLimit);
    }

    private boolean atEOF(boolean doConsume) throws IOException {
        markReader(1, doConsume);

        int value = source.read();
        atEOF = value == -1;

        resetReader(doConsume, atEOF);
        return atEOF;
    }

    private boolean atComma(boolean doConsume) throws IOException {
        return atChar(',', doConsume);
    }

    private String matchValue() throws IOException {
        consumeWhiteSpace();

        String value = matchQuotedValue();
        if (value == null) {
            value = matchUnquotedValue();
        }

        consumeWhiteSpace();
        return value;
    }

    private String matchQuotedValue() throws IOException {
        // Enclosing quotes and quotes used to escape other quotes
        // are discarded

        boolean doConsume = true;
        if (!atChar('"', doConsume)) {
            // This isn't a quoted value
            return null;
        }

        StringBuffer value = new StringBuffer();
        while (true) {
            char currentChar = (char) source.read();

            if (currentChar == '"' && !atChar('"', doConsume)) {
                // Single quote ends the value
                break;
            }

            value.append(currentChar);
        }

        return value.toString();
    }

    private String matchUnquotedValue() throws IOException {
        boolean doConsume = false;
        StringBuffer value = new StringBuffer();
        int trailingSpaces = 0;

        while (true) {
            // Break on newline or comma without consuming
            if (atNewline(doConsume) || atEOF(doConsume) || atComma(doConsume)) {
                break;
            }

            // Unquoted values cannot contain quotes
            if (atChar('"', doConsume)) {
                if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                    log.debug(".matchUnquotedValue matched unexpected double-quote while matching " + value);
                    log.debug(".matchUnquotedValue values==" + values);
                }
                throw unexpectedCharacterException('"');
            }

            char currentChar = (char) source.read();

            // Update the count of trailing spaces
            trailingSpaces = (isWhiteSpace(currentChar)) ?
                    trailingSpaces + 1 : 0;

            value.append(currentChar);
        }

        // Remove the trailing spaces
        int end = value.length();
        value.delete(end - trailingSpaces, end);

        // An empty string means that this value was just whitespace,
        // so nothing was matched
        return value.length() == 0 ? null : value.toString();
    }

    private void consumeWhiteSpace() throws IOException {
        while (true) {
            source.mark(1);
            char currentChar = (char) source.read();

            if (!isWhiteSpace(currentChar)) {
                source.resetToMark();
                break;
            }
        }
    }

    private boolean isWhiteSpace(char currentChar) {
        return currentChar == ' ' || currentChar == '\t';
    }

    private EPException unexpectedCharacterException(char unexpected) {
        return new EPException("Encountered unexpected character " + unexpected);
    }

    private void skipCommentedLines() throws IOException {
        boolean doConsume = false;
        while (true) {
            if (atEOF && looping) {
                reset();
            }
            if (atChar('#', doConsume)) {
                consumeLine();
            } else {
                break;
            }
        }
    }

    private void consumeLine() throws IOException {
        boolean doConsume = true;
        while (!atEOF(doConsume) && !atNewline(doConsume)) {
            // Discard input
            source.read();
        }
    }
}
