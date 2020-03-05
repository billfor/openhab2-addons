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
package org.openhab.binding.alarmdecoder.internal.handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.alarmdecoder.internal.config.SerialBridgeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * Handler responsible for communicating via a serial port with the Nu Tech Alarm Decoder device.
 * Based on and including code from the original OH1 alarmdecoder binding.
 *
 * @author Bernd Pfrommer - Initial contribution (OH1 version)
 * @author Bob Adair - Re-factored into OH2 binding
 */
@NonNullByDefault
public class SerialBridgeHandler extends ADBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SerialBridgeHandler.class);

    private @NonNullByDefault({}) SerialBridgeConfig config;

    /** name of serial device */
    private @Nullable String serialDeviceName = null;
    private @Nullable SerialPort serialPort = null;
    private int serialPortSpeed = 115200;

    public SerialBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing serial bridge handler");
        config = getConfigAs(SerialBridgeConfig.class);
        discovery = config.discovery;

        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "bridge configuration missing");

        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // moved from OH1 execute() method
        synchronized (this) {
            if (serialPort == null) {
                connect();
            }
        }

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });

        logger.trace("Finished initializing serial bridge handler");
    }

    @Override
    protected synchronized void connect() {
        try {
            disconnect(); // make sure we have disconnected
            if (this.serialDeviceName != null) {
                /*
                 * by default, RXTX searches only devices /dev/ttyS* and
                 * /dev/ttyUSB*, and will so not find symlinks. The
                 * setProperty() call below helps
                 */
                updateSerialProperties(serialDeviceName);
                CommPortIdentifier ci = CommPortIdentifier.getPortIdentifier(serialDeviceName);
                CommPort cp = ci.open("openhabalarmdecoder", 10000);
                if (cp == null) {
                    throw new IllegalStateException("cannot open serial port!");
                }
                if (cp instanceof SerialPort) {
                    serialPort = (SerialPort) cp;
                } else {
                    throw new IllegalStateException("unknown port type");
                }
                serialPort.setSerialPortParams(serialPortSpeed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
                serialPort.disableReceiveFraming();
                serialPort.disableReceiveThreshold();
                reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
                logger.info("connected to serial port: {}", serialDeviceName);
                panelReadyReceived = false;
                startMsgReader();
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.debug("device name not configured");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "device name not configured");
            }
        } catch (PortInUseException e) {
            logger.warn("cannot open serial port: {}, it is already in use", serialDeviceName);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (UnsupportedCommOperationException | NoSuchPortException | IllegalStateException | IOException e) {
            logger.debug("error connecting to serial port", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    protected synchronized void disconnect() {
        stopMsgReader();
        if (serialPort != null) {
            serialPort.close();
            serialPort = null;
        }
    }

    private void updateSerialProperties(@Nullable String devName) {
        /*
         * By default, RXTX searches only devices /dev/ttyS* and
         * /dev/ttyUSB*, and will therefore not find devices that
         * have been symlinked. Adding them however is tricky, see below.
         */

        // first go through the port identifiers to find any that are not in
        // "gnu.io.rxtx.SerialPorts"

        if (devName == null) {
            return;
        }
        ArrayList<String> allPorts = new ArrayList<String>();
        @SuppressWarnings("rawtypes")
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
            if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                allPorts.add(id.getName());
            }
        }
        logger.trace("ports found from identifiers: {}", StringUtils.join(allPorts, ":"));

        // now add our port so it's in the list
        if (!allPorts.contains(devName)) {
            allPorts.add(devName);
        }

        // add any that are already in "gnu.io.rxtx.SerialPorts"
        // so we don't accidentally overwrite some of those ports

        String ports = System.getProperty("gnu.io.rxtx.SerialPorts");
        if (ports != null) {
            ArrayList<String> propPorts = new ArrayList<String>(Arrays.asList(ports.split(":")));
            for (String p : propPorts) {
                if (!allPorts.contains(p)) {
                    allPorts.add(p);
                }
            }
        }
        String finalPorts = StringUtils.join(allPorts, ":");
        logger.trace("final port list: {}", finalPorts);

        // Finally overwrite the "gnu.io.rxtx.SerialPorts" System property.

        // Note: calling setProperty() is not threadsafe. All bindings run in
        // the same address space, System.setProperty() is globally visible
        // to all bindings.
        // This means if multiple bindings use the serial port there is a
        // race condition where two bindings could be changing the properties
        // at the same time.

        System.setProperty("gnu.io.rxtx.SerialPorts", finalPorts);
    }
}
