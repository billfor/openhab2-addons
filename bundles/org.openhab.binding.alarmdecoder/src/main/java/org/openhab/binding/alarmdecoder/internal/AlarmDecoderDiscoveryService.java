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
package org.openhab.binding.alarmdecoder.internal;

import static org.openhab.binding.alarmdecoder.internal.AlarmDecoderBindingConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.alarmdecoder.internal.handler.ADBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AlarmDecoderDiscoveryService} handles discovery of devices as they are identified by the bridge handler.
 * Requests from the framework to startScan() are ignored, since no active scanning is possible.
 *
 * @author Bob Adair - Initial contribution
 */
@NonNullByDefault
public class AlarmDecoderDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(AlarmDecoderDiscoveryService.class);

    private ADBridgeHandler bridgeHandler;
    private boolean discoveryEnabled = true; // TODO: remove
    private final Set<String> discoveredZoneSet = new HashSet<>();
    private final Set<Integer> discoveredRFZoneSet = new HashSet<>();
    // private final Set<Integer> discoveredKeypadSet = new HashSet<>();

    public AlarmDecoderDiscoveryService(ADBridgeHandler bridgeHandler) throws IllegalArgumentException {
        super(DISCOVERABLE_DEVICE_TYPE_UIDS, 0, false);
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void startScan() {
        // Ignore start scan requests
    }

    public void processZone(int address, int channel) {
        if (!discoveryEnabled) {
            return;
        }
        String token = String.format("%d-%d", address, channel);
        if (!discoveredZoneSet.contains(token)) {
            notifyDiscoveryOfZone(address, channel, token);
            discoveredZoneSet.add(token);
        }
    }

    public void processRFZone(int serial) {
        if (discoveryEnabled && !discoveredRFZoneSet.contains(serial)) {
            notifyDiscoveryOfRFZone(serial);
            discoveredRFZoneSet.add(serial);
        }
    }

    private void notifyDiscoveryOfZone(int address, int channel, String idString) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID uid = new ThingUID(THING_TYPE_ZONE, bridgeUID, idString);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_ADDRESS, address);
        properties.put(PROPERTY_CHANNEL, channel);
        properties.put("id", idString); // TODO: fix id property

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withProperties(properties)
                .withRepresentationProperty("id").build(); // TODO: fix id property
        thingDiscovered(result);
        logger.debug("Discovered Zone {}", uid);
    }

    private void notifyDiscoveryOfRFZone(Integer serial) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID uid = new ThingUID(THING_TYPE_RFZONE, bridgeUID, serial.toString());

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_SERIAL, serial);

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withProperties(properties)
                .withRepresentationProperty(PROPERTY_SERIAL).build();
        thingDiscovered(result);
        logger.debug("Discovered RF Zone{}", uid);
    }
}
