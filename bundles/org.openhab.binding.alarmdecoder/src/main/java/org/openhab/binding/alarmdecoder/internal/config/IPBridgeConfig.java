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
package org.openhab.binding.alarmdecoder.internal.config;

import org.openhab.binding.alarmdecoder.internal.handler.IPBridgeHandler;

/**
 * The {@link IPBridgeConfig} class contains fields mapping thing configuration parameters for {@link IPBridgeHandler}.
 *
 * @author Bob Adair - Initial contribution
 */
public class IPBridgeConfig {
    public String hostname;
    public int tcpPort = 10000;
    public boolean discovery = false;
    public int reconnect = 2;
    public int timeout = 5;
}
