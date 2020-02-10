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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Superclass for all Alarm Decoder protocol message types
 *
 * @author Bob Adair - Initial contribution
 */
@NonNullByDefault
public abstract class ADMessage {

    /** string containing the original unparsed message */
    public String message;

    public ADMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return new String(message);
    }

    protected List<String> splitMsg(String msg) {
        List<String> l = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\,\"]+|\"[^\"]*\"");
        Matcher regexMatcher = regex.matcher(msg);
        while (regexMatcher.find()) {
            l.add(regexMatcher.group());
        }
        return l;
    }
}
