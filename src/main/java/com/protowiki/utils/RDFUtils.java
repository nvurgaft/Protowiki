package com.protowiki.utils;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author Nick
 */
public class RDFUtils {

    /**
     * Splices the '^^' association tag from a string literal for instance this
     * text: "113230702^^http://www.w3.org/2001/XMLSchema#integer" would become
     * this text: "113230702" Notice that this method also trims the output
     * string!
     *
     * @param integerUri the uri text to splice
     * @return the uri text without the Literal type
     */
    public static String spliceLiteralType(String integerUri) {
        if (integerUri == null || integerUri.isEmpty()) {
            return null;
        }
        if (integerUri.contains("^^")) {
            return integerUri.substring(0, integerUri.indexOf("^^"));
        } else {
            return integerUri.trim();
        }
    }

    /**
     * Splices the language tag suffix from a text literal for instance this
     * text: "This is an author description.@jp" would become this text: "This
     * is an author description." Notice that this method also trims the output
     * string!
     *
     * @param text the text to splice
     * @return a language tag suffix spliced out version of the text
     */
    public static String spliceLiteralLaguageTag(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (text.contains("@")) {
            return text.substring(0, text.indexOf("@") - 1).trim();
        } else {
            return text.trim();
        }
    }

    /**
     * Escapes as string of text
     * @param text string to escape
     * @return escaped string, null if text string is null
     */
    public static String escapeTextLiteral(String text) {
        return StringEscapeUtils.escapeJava(text);
    }
    
    /**
     * "Normalizes a full name (as found in MARC 100 fields)
     * into a name that would be easily read and queried in wikipedia/dbPedia
     * Example: a name string such as "Doe, John"
     * would become "John Doe"
     * @param marcName the name string
     * @return the "normalized" name
     */
    public static String normalizeMARCName(String marcName) {
        if (marcName==null) {
            return null;
        }
        
        String[] fname = marcName.split(",");
        return (fname[1].trim() + " " + fname[0].trim());
    }
}