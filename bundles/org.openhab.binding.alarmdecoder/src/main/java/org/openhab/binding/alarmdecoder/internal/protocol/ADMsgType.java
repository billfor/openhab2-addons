/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.alarmdecoder.internal.protocol;

import java.util.HashMap;

import org.eclipse.jdt.annotation.Nullable;

/**
 * The various message types that come from the ad2usb/ad2pi interface
 *
 * @author Bernd Pfrommer - Initial contribution
 * @author Bob Adair - Removed methods unused in OH2 binding
 */
public enum ADMsgType {
    EXP, // zone expander message
    KPM, // keypad message
    LRR, // long range radio message
    REL, // relay message
    RFX, // wireless message
    VER, // version message
    INVALID; // invalid message

    // TODO: Clean up

    // /**
    // *
    // * @return true if it is a valid message type
    // */
    // public boolean isValid() {
    // return (this != INVALID);
    // }
    //
    // /**
    // * Determine the message type from a 3-letter string
    // *
    // * @param s the 3-letter string
    // * @return the message type (potentially INVALID)
    // */
    // public static ADMsgType fromString(String s) {
    // ADMsgType mt = strToType.get(s);
    // if (mt == null) {
    // return strToType.get("INVALID");
    // }
    // return mt;
    // }
    //
    // /**
    // * Test if 3-letter string is a message type
    // *
    // * @param s string to test
    // * @return true if string is a valid message type
    // */
    // public static boolean isValid(String s) {
    // return (fromString(s).isValid());
    // }
    //
    // /**
    // * Test if string contains a valid message type
    // *
    // * @param s string to test
    // * @return true if string contains a valid message type
    // */
    // public static boolean containsValidMsgType(String s) {
    // for (String t : strToType.keySet()) {
    // if (s.contains(t)) {
    // return true;
    // }
    // }
    // return (false);
    // }

    // /** hash map from string to type */
    // private static HashMap<String, ADMsgType> strToType;
    //
    // static {
    // strToType = new HashMap<String, ADMsgType>();
    // strToType.put("KPM", KPM);
    // strToType.put("RFX", RFX);
    // strToType.put("EXP", EXP);
    // strToType.put("REL", REL);
    // strToType.put("LRR", LRR);
    // strToType.put("VER", VER);
    // strToType.put("INVALID", INVALID);
    // }

    /** hash map from protocol message heading to type */
    private static HashMap<String, @Nullable ADMsgType> startToMsgType = new HashMap<>();

    static {
        startToMsgType.put("!REL", ADMsgType.REL);
        startToMsgType.put("!SER", ADMsgType.INVALID);
        startToMsgType.put("!RFX", ADMsgType.RFX);
        startToMsgType.put("!EXP", ADMsgType.EXP);
        startToMsgType.put("!LRR", ADMsgType.LRR);
        startToMsgType.put("!VER", ADMsgType.VER);
    }

    /**
     * Extract message type from message. Relies on static map startToMsgType.
     *
     * @param s message string
     * @return message type
     */
    public static ADMsgType getMsgType(@Nullable String s) {
        if (s == null || s.length() < 4) {
            return ADMsgType.INVALID;
        }
        if (s.startsWith("[")) {
            return ADMsgType.KPM;
        }
        ADMsgType mt = startToMsgType.get(s.substring(0, 4));
        if (mt == null) {
            mt = ADMsgType.INVALID;
        }
        return mt;
    }

}
