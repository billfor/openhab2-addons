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

package org.openhab.binding.radiothermostat.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RadioThermostatData {
    private static Logger logger = LoggerFactory.getLogger(RadioThermostatData.class);

    public double temp;

    // public int temp;
    // public String name;

    private RadioThermostatData() {
    }

    public static RadioThermostatData parse(String response) {
        logger.debug("Parsing string: \"{}\"", response);
        /* parse json string */
        JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
        RadioThermostatData info = new RadioThermostatData();
        info.temp = jsonObject.get("temp").getAsDouble();
        // info.door = jsonObject.get("door").getAsInt();
        // info.name = jsonObject.get("name").getAsString();
        return info;
    }

}
