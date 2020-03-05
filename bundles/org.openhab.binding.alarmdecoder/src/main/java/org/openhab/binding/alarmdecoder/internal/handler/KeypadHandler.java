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

import static org.openhab.binding.alarmdecoder.internal.AlarmDecoderBindingConstants.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.alarmdecoder.internal.config.KeypadConfig;
import org.openhab.binding.alarmdecoder.internal.protocol.ADCommand;
import org.openhab.binding.alarmdecoder.internal.protocol.KeypadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link KeypadHandler} is responsible for handling handling wired zones (i.e. REL & EXP messages).
 *
 * @author Bob Adair - Initial contribution
 * @author Bill Forsyth - Initial contribution
 */
@NonNullByDefault
public class KeypadHandler extends ADThingHandler {

    private final Logger logger = LoggerFactory.getLogger(KeypadHandler.class);

    private @NonNullByDefault({}) KeypadConfig config;
    private boolean singleAddress;
    private Pattern validCommandPattern = Pattern.compile(ADCommand.KEYPAD_COMMAND_REGEX);

    public KeypadHandler(Thing thing) {
        super(thing);
    }

    /**
     * Returns true if this handler is responsible for the supplied address mask.
     * This is true is this handler's address mask is 0 (all), the supplied address mask is 0 (all), or if any bits in
     * this handler's address mask match bits set in the supplied address mask.
     */
    public Boolean responsibleFor(final int addressMask) {
        if (config.addressMask != null
                && (((config.addressMask & addressMask) != 0) || config.addressMask.equals(0) || addressMask == 0)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(KeypadConfig.class);

        if (config.addressMask == null || config.addressMask < 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }
        singleAddress = Integer.bitCount(config.addressMask) == 1;
        logger.debug("Keypad handler initializing for address mask {}", config.addressMask);

        initDeviceState();

        logger.trace("Keypad handler finished initializing");
    }

    @Override
    protected void initDeviceState() {
        logger.trace("Initializing device state for Keypad address mask {}", config.addressMask);
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge configured");
        } else if (bridge.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            initChannelState();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    @Override
    public void initChannelState() {
        // TODO: init channels to UNDEF
    }

    @Override
    public void notifyPanelReady() {
        // Do nothing
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_KP_COMMAND)) {
            if (command instanceof StringType) {
                String cmd = ((StringType) command).toString();
                if (cmd.length() > 0) {
                    if (!config.sendCommands) {
                        logger.info(
                                "Sending keypad commands is disabled. Enable using the sendCommands keypad parameter.");
                        return;
                    }

                    // check that received command is valid
                    Matcher matcher = validCommandPattern.matcher(cmd);
                    if (!matcher.matches()) {
                        logger.info("Invalid characters in command. Ignoring command: {}", cmd);
                        return;
                    }

                    // TODO: Replace A-H in command string with special keys 1-8

                    if (singleAddress) {
                        sendCommand(ADCommand.addressedMessage(config.addressMask, cmd)); // send from keypad address
                    } else {
                        sendCommand(new ADCommand(cmd)); // send from AD address
                    }
                }
            }
        }
    }

    public void handleUpdate(KeypadMessage kpm) {
        // TODO: Update channels only if linked?

        logger.trace("Keypad handler for address mask {} received update: {}", config.addressMask, kpm);
        updateState(CHANNEL_KP_ZONE, new DecimalType(kpm.getZone()));
        updateState(CHANNEL_KP_TEXT, new StringType(kpm.alphaMessage));

        updateState(CHANNEL_KP_READY, (kpm.getStatus(KeypadMessage.BIT_READY)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_ARMEDAWAY, (kpm.getStatus(KeypadMessage.BIT_ARMEDAWAY)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_ARMEDHOME, (kpm.getStatus(KeypadMessage.BIT_ARMEDHOME)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_BACKLIGHT, (kpm.getStatus(KeypadMessage.BIT_BACKLIGHT)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_PRORGAM, (kpm.getStatus(KeypadMessage.BIT_PRORGAM)) ? OnOffType.ON : OnOffType.OFF);

        updateState(CHANNEL_KP_BEEPS, new DecimalType(kpm.nbeeps));

        updateState(CHANNEL_KP_BYPASSED, (kpm.getStatus(KeypadMessage.BIT_BYPASSED)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_ACPOWER, (kpm.getStatus(KeypadMessage.BIT_ACPOWER)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_CHIME, (kpm.getStatus(KeypadMessage.BIT_CHIME)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_ALARMOCCURRED,
                (kpm.getStatus(KeypadMessage.BIT_ALARMOCCURRED)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_ALARM, (kpm.getStatus(KeypadMessage.BIT_ALARM)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_LOWBAT, (kpm.getStatus(KeypadMessage.BIT_LOWBAT)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_DELAYOFF, (kpm.getStatus(KeypadMessage.BIT_DELAYOFF)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_FIRE, (kpm.getStatus(KeypadMessage.BIT_FIRE)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_SYSFAULT, (kpm.getStatus(KeypadMessage.BIT_SYSFAULT)) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_KP_PERIMETER, (kpm.getStatus(KeypadMessage.BIT_PERIMETER)) ? OnOffType.ON : OnOffType.OFF);

        firstUpdateReceived = true;
    }
}
