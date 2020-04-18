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

package org.openhab.binding.radiothermostat.internal.data;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link RadioThermostatTstat} is responsible for parsing
 * and controlling main thermostat data.
 *
 * @author Bill Forsyth - Initial contribution
 */
public class RadioThermostatTstat {
    public enum Tmode {
        @SerializedName("0")
        off(0),
        @SerializedName("1")
        heat(1),
        @SerializedName("2")
        cool(2),
        @SerializedName("3")
        auto(3);

        private int mode;

        Tmode(int mode) {
            this.mode = mode;
        }

        public int mode() {
            return mode;
        }

        /*
         * public static Tmode fromInt(int mode) {
         * for (Tmode sm : values()) {
         * if (sm.mode == mode) {
         * return sm;
         * }
         * }
         * throw (new IllegalArgumentException("Invalid system mode " + mode));
         * }
         */

    }

    public enum Fmode {
        @SerializedName("0")
        auto,
        @SerializedName("1")
        circulate,
        @SerializedName("2")
        on
    };

    float temp;
    Float t_heat;
    Float t_cool;
    int hold; // ct50 representation of bool is a numeric 0 or 1.
    int tstate; // readonly. actual state
    int fstate; // readonly. actual state
    Tmode tmode;
    Fmode fmode;

    public RadioThermostatTstat() {
        super();
    }

    // we're not setting anything. this status is r/o. so this is just for reference.
    /*
     * public RadioThermostatTstat(float temp, Float t_heat, Float t_cool, int hold, Fmode fmode, Tmode tmode, int
     * tstate,
     * int fstate) {
     * super();
     * this.temp = temp;
     * this.t_heat = t_heat;
     * this.t_cool = t_cool;
     * this.hold = hold;
     * this.fmode = fmode;
     * this.tmode = tmode;
     * this.tstate = tstate;
     * this.fstate = fstate;
     * }
     */

    public float getTemp() {
        return temp;
    }

    public Float getHeatingSetpoint() {
        return t_heat;
    }

    public Float getCoolingSetpoint() {
        return t_cool;
    }

    public int getHold() {
        return hold;
    }

    public String getFan() {
        return fmode.name();
    }

    public int getFanNumber() {
        return fmode.ordinal();
    }

    public int getTstate() {
        return tstate;
    }

    public int getFstate() {
        return fstate;
    }

    public String getMode() {
        return tmode.name();
    }

    public int getModeNumber() {
        return tmode.ordinal();
    }

}
