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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.alarmdecoder.internal.config.IPBridgeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler responsible for communicating via TCP with the Nu Tech Alarm Decoder device.
 * Based on and including code from the original OH1 alarmdecoder binding.
 *
 * @author Bernd Pfrommer - Initial contribution (OH1 version)
 * @author Bob Adair - Re-factored into OH2 binding
 */
@NonNullByDefault
public class IPBridgeHandler extends ADBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(IPBridgeHandler.class);

    private @NonNullByDefault({}) IPBridgeConfig config;

    /** hostname for the alarmdecoder process */
    private @Nullable String tcpHostName = null;
    /** port for the alarmdecoder process */
    private int tcpPort = -1;
    private @Nullable Socket socket = null;

    public IPBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing IP bridge handler");
        config = getConfigAs(IPBridgeConfig.class);
        discovery = config.discovery;

        if (config.hostname == null || config.tcpPort == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "ipAddress/tcpPort parameters not supplied");
            return;
        }
        tcpHostName = config.hostname;
        tcpPort = config.tcpPort;

        // set the thing status to UNKNOWN temporarily and let the background connect task decide the real status.
        // we set this up-front to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        scheduler.submit(this::connect); // start the async connect task

        logger.trace("Finished initializing IP bridge handler");
    }

    @Override
    protected synchronized void connect() {
        boolean connectionSuccess = false;

        try {
            disconnect(); // make sure we have disconnected
            if (tcpHostName != null && tcpPort > 0 && tcpPort < 65536) {
                socket = new Socket(tcpHostName, tcpPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                logger.debug("connected to {}:{}", tcpHostName, tcpPort);
                panelReadyReceived = false;
                startMsgReader();
                updateStatus(ThingStatus.ONLINE);
                connectionSuccess = true;
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "invalid ipAddress/tcpPort configured");
            }
        } catch (UnknownHostException e) {
            logger.debug("unknown hostname: {}", tcpHostName);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "unknown host");
            disconnect();
        } catch (IOException e) {
            logger.debug("cannot open connection to {}:{} error: {}", tcpHostName, tcpPort, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            disconnect();
            scheduleConnectRetry(config.reconnect); // Possibly a retryable error. Try again later.
        }

        // Start connection check job
        if (connectionSuccess) {
            connectionCheckJob = scheduler.scheduleWithFixedDelay(this::connectionCheck, config.heartbeat,
                    config.heartbeat, TimeUnit.MINUTES);
        }
    }

    protected synchronized void connectionCheck() {
        logger.trace("Connection check job started");
        // TODO: Implement connection check
        // Move to superclass?
        // connectionCheckReconnectJob = scheduler.schedule(command, delay, unit);
    }

    @Override
    protected synchronized void disconnect() {
        // stop scheduled connection check and retry jobs
        if (connectRetryJob != null) {
            // use cancel(false) so we don't kill ourselves when connect retry job calls disconnect()
            connectRetryJob.cancel(false);
            connectRetryJob = null;
        }
        if (connectionCheckJob != null) {
            connectionCheckJob.cancel(true);
            connectionCheckJob = null;
        }
        if (connectionCheckReconnectJob != null) {
            // use cancel(false) so we don't kill ourselves when reconnect job calls disconnect()
            connectionCheckReconnectJob.cancel(false);
            connectionCheckReconnectJob = null;
        }

        stopMsgReader();

        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            logger.debug("error closing reader/writer: {}", e.getMessage());
        }
        writer = null;
        reader = null;

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("error closing socket: {}", e.getMessage());
            }
        }
        socket = null;
    }
}
