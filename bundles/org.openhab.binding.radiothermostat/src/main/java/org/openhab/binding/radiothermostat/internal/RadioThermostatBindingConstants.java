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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link RadioThermostatBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Bill Forsyth - Initial contribution
 */
@NonNullByDefault
public class RadioThermostatBindingConstants {

    private static final String BINDING_ID = "radiothermostat";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_RADIOTHERMOSTAT = new ThingTypeUID(BINDING_ID, "thermostat");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_RADIOTHERMOSTAT);

    // List or properties
    public static final String PROPERTY_API = "api";
    public static final String PROPERTY_FIRMWARE = "firmwareVersion";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_UUID = "uuid";
    public static final String PROPERTY_IP = "ip";

    // List of all Channel ids
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_FAN = "fan";
    public static final String CHANNEL_HOLD = "hold";
    public static final String CHANNEL_MODE = "mode";

    public static final String CHANNEL_HEATING_SETPOINT = "heatingSetpoint";
    public static final String CHANNEL_COOLING_SETPOINT = "coolingSetpoint";
    public static final String CHANNEL_SYSTEM_STATE = "systemState";

    public static final String CHANNEL_HEAT_RUNTIME_YESTERDAY = "heatRuntimeYesterday";
    public static final String CHANNEL_COOL_RUNTIME_YESTERDAY = "coolRuntimeYesterday";

    public static final String CHANNEL_FAN_NUMBER = "fanNumber";
    public static final String CHANNEL_MODE_NUMBER = "modeNumber";
    public static final String CHANNEL_FAN_STATE = "fanState";
    public static final String CHANNEL_MODE_STATE = "modeState";

    public static final String CHANNEL_LASTUPDATE = "lastupdate";
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_REBOOT = "reboot";
}
