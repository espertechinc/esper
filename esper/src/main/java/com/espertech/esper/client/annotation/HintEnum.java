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
package com.espertech.esper.client.annotation;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.annotation.AnnotationException;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Enumeration of hint values. Since hints may be a comma-separate list in a single @Hint annotation
 * they are listed as enumeration values here.
 */
public enum HintEnum {
    /**
     * For use with match_recognize, iterate-only matching.
     */
    ITERATE_ONLY("ITERATE_ONLY", false, false, false),

    /**
     * For use with group-by, disabled reclaim groups.
     */
    DISABLE_RECLAIM_GROUP("DISABLE_RECLAIM_GROUP", false, false, false),

    /**
     * For use with group-by and std:groupwin, reclaim groups for unbound streams based on time. The number of seconds after which a groups is reclaimed if inactive.
     */
    RECLAIM_GROUP_AGED("RECLAIM_GROUP_AGED", true, true, false),

    /**
     * For use with group-by and std:groupwin, reclaim groups for unbound streams based on time, this number is the frequency in seconds at which a sweep occurs for aged
     * groups, if not provided then the sweep frequency is the same number as the age.
     */
    RECLAIM_GROUP_FREQ("RECLAIM_GROUP_FREQ", true, true, false),

    /**
     * For use with create-named-window statements only, to indicate that statements that subquery the named window
     * use named window data structures (unless the subquery statement specifies below DISBABLE hint and as listed below).
     * <p>
     * By default and if this hint is not specified or subqueries specify a stream filter on a named window,
     * subqueries use statement-local data structures representing named window contents (table, index).
     * Such data structure is maintained by consuming the named window insert and remove stream.
     */
    ENABLE_WINDOW_SUBQUERY_INDEXSHARE("ENABLE_WINDOW_SUBQUERY_INDEXSHARE", false, false, false),

    /**
     * If ENABLE_WINDOW_SUBQUERY_INDEXSHARE is not specified for a named window (the default) then this instruction is ignored.
     * <p>
     * For use with statements that subquery a named window and that benefit from a statement-local data structure representing named window contents (table, index),
     * maintained through consuming the named window insert and remove stream.
     * </p>
     */
    DISABLE_WINDOW_SUBQUERY_INDEXSHARE("DISABLE_WINDOW_SUBQUERY_INDEXSHARE", false, false, false),

    /**
     * For use with subqueries and on-select, on-merge, on-update and on-delete to specify the query engine neither
     * build an implicit index nor use an existing index, always performing a full table scan.
     */
    SET_NOINDEX("SET_NOINDEX", false, false, false),

    /**
     * For use with join query plans to force a nested iteration plan.
     */
    FORCE_NESTED_ITER("FORCE_NESTED_ITER", false, false, false),

    /**
     * For use with join query plans to indicate preferance of the merge-join query plan.
     */
    PREFER_MERGE_JOIN("PREFER_MERGE_JOIN", false, false, false),

    /**
     * For use everywhere where indexes are used (subquery, joins, fire-and-forget, onl-select etc.), index hint.
     */
    INDEX("INDEX", false, false, true),

    /**
     * For use where query planning applies.
     */
    EXCLUDE_PLAN("EXCLUDE_PLAN", false, false, true),

    /**
     * For use everywhere where unique data window are used
     */
    DISABLE_UNIQUE_IMPLICIT_IDX("DISABLE_UNIQUE_IMPLICIT_IDX", false, false, false),

    /**
     * For use when filter expression optimization may widen the filter expression.
     */
    MAX_FILTER_WIDTH("MAX_FILTER_WIDTH", true, true, false),

    /**
     * For use everywhere where unique data window are used
     */
    DISABLE_WHEREEXPR_MOVETO_FILTER("DISABLE_WHEREEXPR_MOVETO_FILTER", false, false, false),

    /**
     * For use with output rate limiting.
     */
    ENABLE_OUTPUTLIMIT_OPT("ENABLE_OUTPUTLIMIT_OPT", false, false, false),

    /**
     * For use with output rate limiting.
     */
    DISABLE_OUTPUTLIMIT_OPT("DISABLE_OUTPUTLIMIT_OPT", false, false, false);

    private final String value;
    private final boolean acceptsParameters;
    private final boolean requiresParameters;
    private final boolean requiresParentheses;

    private HintEnum(String value, boolean acceptsParameters, boolean requiresParameters, boolean requiresParentheses) {
        this.value = value.toUpperCase(Locale.ENGLISH);
        this.acceptsParameters = acceptsParameters;
        if (acceptsParameters) {
            this.requiresParameters = true;
        } else {
            this.requiresParameters = requiresParameters;
        }
        this.requiresParentheses = requiresParentheses;
    }

    /**
     * Returns the constant.
     *
     * @return constant
     */
    public String getValue() {
        return value;
    }

    /**
     * True if the hint accepts params.
     *
     * @return indicator
     */
    public boolean isAcceptsParameters() {
        return acceptsParameters;
    }

    /**
     * True if the hint requires params.
     *
     * @return indicator
     */
    public boolean isRequiresParameters() {
        return requiresParameters;
    }

    /**
     * Check if the hint is present in the annotations provided.
     *
     * @param annotations the annotations to inspect
     * @return indicator
     */
    public Hint getHint(Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }

        for (Annotation annotation : annotations) {
            if (!(annotation instanceof Hint)) {
                continue;
            }

            Hint hintAnnotation = (Hint) annotation;
            try {
                Map<HintEnum, List<String>> setOfHints = HintEnum.validateGetListed(hintAnnotation);
                if (setOfHints.containsKey(this)) {
                    return hintAnnotation;
                }
            } catch (AnnotationException e) {
                throw new EPException("Invalid hint: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Validate a hint annotation ensuring it contains only recognized hints.
     *
     * @param annotation to validate
     * @return validated hint enums and their parameter list
     * @throws AnnotationException if an invalid text was found
     */
    public static Map<HintEnum, List<String>> validateGetListed(Annotation annotation) throws AnnotationException {
        if (!(annotation instanceof Hint)) {
            return Collections.emptyMap();
        }

        Hint hint = (Hint) annotation;
        String hintValueCaseNeutral = hint.value().trim();
        String hintValueUppercase = hintValueCaseNeutral.toUpperCase(Locale.ENGLISH);

        for (HintEnum val : HintEnum.values()) {
            if (val.getValue().equals(hintValueUppercase) && !val.requiresParentheses) {
                validateParameters(val, hint.value().trim());
                List<String> parameters;
                if (val.acceptsParameters) {
                    String assignment = getAssignedValue(hint.value().trim(), val.value);
                    if (assignment == null) {
                        parameters = Collections.emptyList();
                    } else {
                        parameters = Collections.singletonList(assignment);
                    }
                } else {
                    parameters = Collections.emptyList();
                }
                return Collections.singletonMap(val, parameters);
            }
        }

        String[] hints = splitCommaUnlessInParen(hint.value());
        Map<HintEnum, List<String>> listed = new HashMap<HintEnum, List<String>>();
        for (int i = 0; i < hints.length; i++) {
            String hintValUppercase = hints[i].trim().toUpperCase(Locale.ENGLISH);
            String hintValNeutralcase = hints[i].trim();
            HintEnum found = null;
            String parameter = null;

            for (HintEnum val : HintEnum.values()) {
                if (val.getValue().equals(hintValUppercase) && !val.requiresParentheses) {
                    found = val;
                    parameter = getAssignedValue(hint.value().trim(), val.value);
                    break;
                }

                if (val.requiresParentheses) {
                    int indexOpen = hintValUppercase.indexOf('(');
                    int indexClosed = hintValUppercase.lastIndexOf(')');
                    if (indexOpen != -1) {
                        String hintNameNoParen = hintValUppercase.substring(0, indexOpen);
                        if (val.getValue().equals(hintNameNoParen)) {
                            if (indexClosed == -1 || indexClosed < indexOpen) {
                                throw new AnnotationException("Hint '" + val + "' mismatches parentheses");
                            }
                            if (indexClosed != hintValUppercase.length() - 1) {
                                throw new AnnotationException("Hint '" + val + "' has additional text after parentheses");
                            }
                            found = val;
                            parameter = hintValNeutralcase.substring(indexOpen + 1, indexClosed);
                            break;
                        }
                    }
                    if (hintValUppercase.equals(val.getValue()) && indexOpen == -1) {
                        throw new AnnotationException("Hint '" + val + "' requires additional parameters in parentheses");
                    }
                }

                if (hintValUppercase.indexOf('=') != -1) {
                    String hintName = hintValUppercase.substring(0, hintValUppercase.indexOf('='));
                    if (val.getValue().equals(hintName.trim().toUpperCase(Locale.ENGLISH))) {
                        found = val;
                        parameter = getAssignedValue(hint.value().trim(), val.value);
                        break;
                    }
                }
            }

            if (found == null) {
                String hintName = hints[i].trim();
                if (hintName.indexOf('=') != -1) {
                    hintName = hintName.substring(0, hintName.indexOf('='));
                }
                throw new AnnotationException("Hint annotation value '" + hintName.trim() + "' is not one of the known values");
            } else {
                if (!found.requiresParentheses) {
                    validateParameters(found, hintValUppercase);
                }
                List<String> existing = listed.get(found);
                if (existing == null) {
                    existing = new ArrayList<String>();
                    listed.put(found, existing);
                }
                if (parameter != null) {
                    existing.add(parameter);
                }
            }
        }
        return listed;
    }

    private static void validateParameters(HintEnum val, String hintVal) throws AnnotationException {
        if (val.isRequiresParameters()) {
            if (hintVal.indexOf('=') == -1) {
                throw new AnnotationException("Hint '" + val + "' requires a parameter value");
            }
        }
        if (!val.isAcceptsParameters()) {
            if (hintVal.indexOf('=') != -1) {
                throw new AnnotationException("Hint '" + val + "' does not accept a parameter value");
            }
        }
    }

    /**
     * Returns hint value.
     *
     * @param annotation to look for
     * @return hint assigned first value provided
     */
    public String getHintAssignedValue(Hint annotation) {
        try {
            Map<HintEnum, List<String>> hintValues = validateGetListed(annotation);
            if (hintValues == null || !hintValues.containsKey(this)) {
                return null;
            }
            return hintValues.get(this).get(0);
        } catch (AnnotationException ex) {
            throw new EPException("Failed to interpret hint annotation: " + ex.getMessage(), ex);
        }
    }

    /**
     * Returns all values assigned for a given hint, if any
     *
     * @param annotations to be interogated
     * @return hint assigned values or null if none found
     */
    public List<String> getHintAssignedValues(Annotation[] annotations) {
        List<String> allHints = null;
        try {
            for (Annotation annotation : annotations) {
                Map<HintEnum, List<String>> hintValues = validateGetListed(annotation);
                if (hintValues == null || !hintValues.containsKey(this)) {
                    continue;
                }
                if (allHints == null) {
                    allHints = hintValues.get(this);
                } else {
                    allHints.addAll(hintValues.get(this));
                }
            }
        } catch (AnnotationException ex) {
            throw new EPException("Failed to interpret hint annotation: " + ex.getMessage(), ex);
        }
        return allHints;
    }

    private static String getAssignedValue(String value, String enumValue) {

        String valMixed = value.trim();
        String val = valMixed.toUpperCase(Locale.ENGLISH);

        if (!val.contains(",")) {
            if (val.indexOf('=') == -1) {
                return null;
            }

            String hintName = val.substring(0, val.indexOf('='));
            if (!hintName.equals(enumValue)) {
                return null;
            }
            return valMixed.substring(val.indexOf('=') + 1, val.length());
        }

        String[] hints = valMixed.split(",");
        for (String hint : hints) {
            int indexOfEquals = hint.indexOf('=');
            if (indexOfEquals == -1) {
                continue;
            }

            val = hint.substring(0, indexOfEquals).trim().toUpperCase(Locale.ENGLISH);
            if (!val.equals(enumValue)) {
                continue;
            }

            String strValue = hint.substring(indexOfEquals + 1).trim();
            if (strValue.length() == 0) {
                return null;
            }
            return strValue;
        }
        return null;
    }

    /**
     * Split a line of comma-separated values allowing parenthesis.
     *
     * @param line to split
     * @return parameters
     */
    public static String[] splitCommaUnlessInParen(String line) {
        int nestingLevelParen = 0;

        int lastComma = -1;
        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '(') {
                nestingLevelParen++;
            }
            if (c == ')') {
                if (nestingLevelParen == 0) {
                    throw new RuntimeException("Close parenthesis ')' found but none open");
                }
                nestingLevelParen--;
            }
            if (c == ',' && nestingLevelParen == 0) {
                String part = line.substring(lastComma + 1, i);
                if (part.trim().length() > 0) {
                    parts.add(part);
                }
                lastComma = i;
            }
        }
        String lastPart = line.substring(lastComma + 1);
        if (lastPart.trim().length() > 0) {
            parts.add(lastPart);
        }
        return parts.toArray(new String[parts.size()]);
    }
}