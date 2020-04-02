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
    public static final ThingTypeUID THING_TYPE_RADIOTHERMOSTAT = new ThingTypeUID(BINDING_ID, "radiothermostat");

    // List of all Channel ids
    public final static String CHANNEL_TEMPERATURE = "temperature";
    public final static String CHANNEL_FAN = "fan";
    public final static String CHANNEL_HOLD = "hold";
    public final static String CHANNEL_MODE = "mode";

    public final static String CHANNEL_HEATING_SETPOINT = "heatingSetpoint";
    public final static String CHANNEL_COOLING_SETPOINT = "coolingSetpoint";
    public final static String CHANNEL_SYSTEM_STATE = "systemState";

    public final static String CHANNEL_HEAT_RUNTIME_TODAY = "heatRuntimeToday";
    public final static String CHANNEL_HEAT_RUNTIME_YESTERDAY = "heatRuntimeYesterday";
    public final static String CHANNEL_COOL_RUNTIME_TODAY = "coolRuntimeToday";
    public final static String CHANNEL_COOL_RUNTIME_YESTERDAY = "coolRuntimeYesterday";

    public final static String CHANNEL_FAN_NUMBER = "fanNumber";
    public final static String CHANNEL_MODE_NUMBER = "modeNumber";
    public final static String CHANNEL_FAN_STATE = "fanState";
    public final static String CHANNEL_MODE_STATE = "modeState";

    public final static String REFRESH_INVALID = "refresh-invalid";
    public final static String EMPTY_INVALID = "empty-invalid";
}
