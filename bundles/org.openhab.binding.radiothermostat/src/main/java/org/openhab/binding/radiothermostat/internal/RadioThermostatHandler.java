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

import static org.eclipse.smarthome.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.binding.radiothermostat.internal.RadioThermostatBindingConstants.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.openhab.binding.radiothermostat.internal.data.RadioThermostatTstat;
import org.openhab.binding.radiothermostat.internal.data.RadioThermostatTstatDatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link RadioThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bill Forsyth - Initial contribution
 *         based on the Venstar binding by William Welliver and Dan Cunningham
 */
@NonNullByDefault
public class RadioThermostatHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(RadioThermostatHandler.class);
    private int refresh;

    private RadioThermostatTstat tstat = new RadioThermostatTstat();
    private RadioThermostatTstatDatalog datalog = new RadioThermostatTstatDatalog();

    private static final int TIMEOUT_SECONDS = 3;
    private static final int RETRY_ATTEMPTS = 2;

    private static final int UPDATE_AFTER_INIT_SECONDS = 20;
    private static final int INITIAL_UPDATE_DELAY = 30;
    private static final int HISTORY_CHECK_GRACE_PERIOD = 600;

    private @Nullable RadioThermostatConfiguration config;
    // TODO: put daily runtime GET in job, and maybe the property update
    private @Nullable Future<?> updatesTask, historyTask, propertyTask = null;

    private @Nullable String baseURL;
    private final HttpClient httpClient;
    private final Gson gson;

    private Unit<Temperature> unitSystem = ImperialUnits.FAHRENHEIT; // CT50 seems to allways report in F
                                                                     // C setting is for display.

    public RadioThermostatHandler(Thing thing) {
        super(thing);
        httpClient = new HttpClient();
        gson = new GsonBuilder().create();
        logger.trace("Radiothermostat handler for thing {}", getThing().getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int value;
        logger.warn("Received command {} for thing '{}' on channel {}", command, thing.getUID().getAsString(),
                channelUID.getId());

        if (getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("Thermostat is NOT ONLINE and is not responding to commands");
            return;
        }

        if (command instanceof RefreshType) {
            logger.debug("Refresh command requested for {}", channelUID);
            if (tstat.getTemp() != 0) {
                startUpdatesTask(0); // ignore if we have not ever received update. Since the single update gets
                                     // everything.
            } else {
                logger.trace("Ignoring initial refresh.");
                startUpdatesTask(UPDATE_AFTER_INIT_SECONDS);
            }
        } else {
            // stopUpdateTasks();
            if (channelUID.getId().equals(CHANNEL_HEATING_SETPOINT)) {
                QuantityType<Temperature> quantity = commandToQuantityType(command, unitSystem);
                value = quantityToRoundedTemperature(quantity, unitSystem).intValue();
                updateState(CHANNEL_HEATING_SETPOINT, new QuantityType<Temperature>(value, unitSystem));
                logger.debug("Setting heating setpoint to {}", value);
                postData("t_heat", value);
            } else if (channelUID.getId().equals(CHANNEL_COOLING_SETPOINT)) {
                QuantityType<Temperature> quantity = commandToQuantityType(command, unitSystem);
                value = quantityToRoundedTemperature(quantity, unitSystem).intValue();
                updateState(CHANNEL_COOLING_SETPOINT, new QuantityType<Temperature>(value, unitSystem));
                logger.debug("Setting cooling setpoint to {}", value);
                postData("t_cool", value);
            } else if (channelUID.getId().equals(CHANNEL_MODE)) {
                value = RadioThermostatTstat.Tmode.valueOf(((StringType) command).toString()).ordinal();
                logger.debug("Set mode to {}", value);
                postData("tmode", value);
            } else if (channelUID.getId().equals(CHANNEL_HOLD)) {
                value = (((OnOffType) command).equals(OnOffType.ON)) ? 1 : 0;
                logger.debug("Set hold to {}", value);
                postData("hold", value);
            } else if (channelUID.getId().equals(CHANNEL_MODE_NUMBER)) {
                logger.debug("update mode");
                value = ((DecimalType) command).intValue();
                logger.debug("Set mode to {}", value);
                updateState(CHANNEL_MODE_NUMBER, (DecimalType) command);
                postData("tmode", value);
            } else if (channelUID.getId().equals(CHANNEL_FAN)) {
                value = RadioThermostatTstat.Fmode.valueOf(((StringType) command).toString()).ordinal();
                logger.debug("Set fmode to {}", value);
                postData("fmode", value);
            } else if (channelUID.getId().equals(CHANNEL_COMMAND)) {
                String jsonString = ((StringType) command).toString();
                logger.debug("Sending command: {}", jsonString);
                postData(jsonString, -1);
            } else if (channelUID.getId().equals(CHANNEL_REBOOT)) {
                logger.debug("Rebooting");
                postData("reboot", 0);
            } else {
                logger.debug("Can not handle command '{}'", command);
            }
        }

        // startUpdatesTask(UPDATE_AFTER_COMMAND_SECONDS);
    }

    private void postData(String jsonElement, int value) {
        String requestString;
        String json;

        if (jsonElement == "reboot") {
            requestString = baseURL + "/sys/command";
        } else {
            requestString = baseURL + "/tstat";
        }

        if (value == -1) {
            json = jsonElement;
        } else {
            if (jsonElement == "reboot") {
                json = "{\"command\":\"reboot\"}";
            } else {
                json = "{\"" + jsonElement + "\":" + String.valueOf(value) + "}";
            }
        }

        for (int i = 0; i < RETRY_ATTEMPTS; i++) {
            try {
                Request request = httpClient.newRequest(requestString)
                        .content(new StringContentProvider(json), "application/json")
                        .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).method(HttpMethod.POST);
                logger.trace("sendRequest: sending {} json {} value {}", request.getURI(), jsonElement, value);
                ContentResponse response = request.send();
                logger.trace("Response code {}", response.getStatus());
                if (response.getStatus() != 200) {
                    logger.debug("Error communicating with thermostat. Error Code: " + response.getStatus());
                    goOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Bad response code: " + response.getStatus());
                    return;
                } else {
                    // String content = response.getContentAsString();
                    JsonObject jsonObject = new JsonParser().parse(response.getContentAsString()).getAsJsonObject();
                    logger.trace("sendRequest: response {}", jsonObject);
                    return;
                }
            } catch (TimeoutException e) {
                logger.debug("Timeout", e);
                // goOffline(ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (InterruptedException | ExecutionException e) {
                logger.debug("Unable to fetch info data", e);
                goOffline(ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                return;
            } catch (JsonSyntaxException e) {
                logger.info("Invalid json sent: ", e);
                return;
            }
        }
        goOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Timed out after retries.");
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        stopUpdateTasks();

        config = getConfigAs(RadioThermostatConfiguration.class);
        logger.debug("ip = {}, refresh = {}", config.ip, config.refresh);

        try {
            if (!httpClient.isStarted()) {
                httpClient.start();
            }
            refresh = config.refresh;
        } catch (Exception e) {
            logger.debug("Could not connect to URL  {}", baseURL, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }

        if (config.ip == null) {
            config.ip = getThing().getProperties().get(PROPERTY_IP); // it must have been autodiscovered
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Hostname/IP address or discovery must be set");
        }

        if (config.refresh < 60) {
            config.refresh = 60;
        }

        baseURL = "http://" + config.ip;

        try {
            Map<String, String> properties = editProperties();
            String name, fw, api, model, uuid;
            JsonObject content;
            content = new JsonParser().parse(getData("/sys/name")).getAsJsonObject();
            name = content.get("name").getAsString();
            logger.debug("My name {}", name);

            content = new JsonParser().parse(getData("/sys")).getAsJsonObject();
            fw = content.get("fw_version").getAsString();
            api = content.get("api_version").getAsString();
            uuid = content.get("uuid").getAsString();

            content = new JsonParser().parse(getData("/tstat/model")).getAsJsonObject();
            model = content.get("model").getAsString();

            properties.put(PROPERTY_NAME, name);
            properties.put(PROPERTY_UUID, uuid);
            properties.put(PROPERTY_FIRMWARE, fw);
            properties.put(PROPERTY_API, api);
            properties.put(PROPERTY_MODEL, model);
            properties.put(PROPERTY_IP, config.ip);

            logger.debug("Set properties name: {}, fw: {}, api: {}, model: {}", name, fw, api, model);

            updateProperties(properties);
            updateStatus(ThingStatus.ONLINE);
        } catch (RadioThermostatCommunicationException | JsonSyntaxException e) {
            logger.debug("Error fetching init data {}", baseURL, e);
            updateStatus(ThingStatus.UNKNOWN);
        }
        startUpdatesTask(UPDATE_AFTER_INIT_SECONDS);
        logger.debug("Finished initializing!");
    }

    private String getData(String path) throws RadioThermostatCommunicationException {
        String content;
        for (int i = 0; i < RETRY_ATTEMPTS; i++) {
            try {
                String requestString = baseURL + path;
                logger.trace("Requesting {}", requestString);

                ContentResponse response = httpClient.newRequest(requestString)
                        .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
                logger.trace("Response code {}", response.getStatus());
                if (response.getStatus() != 200) {
                    throw new RadioThermostatCommunicationException(
                            "Error communicating with thermostat. Error Code: " + response.getStatus());
                }
                content = response.getContentAsString();
                logger.trace("sendRequest: response {}", content);
                return content;
            } catch (InterruptedException | ExecutionException e) {
                throw new RadioThermostatCommunicationException(e);
            } catch (TimeoutException e) {
                logger.debug("Timeout", e);
            }
        }
        throw new RadioThermostatCommunicationException("All retries timed out.");
    }

    private void updateData() {
        try {
            Future<?> localUpdatesTask = updatesTask;
            String response = getData("/tstat");
            if (!isFutureValid(localUpdatesTask)) {
                return;
            }

            tstat = gson.fromJson(response, RadioThermostatTstat.class);
            logger.debug("Got mode {} and fan {}", tstat.getMode(), tstat.getFan());

            updateState(CHANNEL_TEMPERATURE, new QuantityType<Temperature>(tstat.getTemp(), unitSystem));
            updateState(CHANNEL_HEATING_SETPOINT,
                    (tstat.getHeatingSetpoint() != null)
                            ? new QuantityType<Temperature>(tstat.getHeatingSetpoint(), unitSystem)
                            : UnDefType.UNDEF);
            updateState(CHANNEL_COOLING_SETPOINT,
                    (tstat.getCoolingSetpoint() != null)
                            ? new QuantityType<Temperature>(tstat.getCoolingSetpoint(), unitSystem)
                            : UnDefType.UNDEF);
            updateState(CHANNEL_HOLD, (tstat.getHold() == 1) ? OnOffType.ON : OnOffType.OFF);
            updateState(CHANNEL_FAN, new StringType(tstat.getFan()));
            updateState(CHANNEL_FAN_NUMBER, new DecimalType(tstat.getFanNumber()));
            updateState(CHANNEL_MODE, new StringType(tstat.getMode()));
            updateState(CHANNEL_MODE_NUMBER, new DecimalType(tstat.getModeNumber()));

            updateState(CHANNEL_FAN_STATE, new DecimalType(tstat.getFstate()));
            updateState(CHANNEL_MODE_STATE, new DecimalType(tstat.getTstate()));

            updateState(CHANNEL_LASTUPDATE, new DateTimeType());

            // Only get the runtime history once a day, inline with the other calls so we can sleep, otherwise
            // thermostat can timeout.
            DateTime startOfDay = DateTime.now().withTimeAtStartOfDay().plusSeconds(HISTORY_CHECK_GRACE_PERIOD);
            if (new Interval(startOfDay, startOfDay.plusSeconds(refresh)).containsNow()) {
                logger.trace("Fetch runlogs");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.trace("Sleep interrupted: {}", e.getMessage());
            }

            response = getData("/tstat/datalog");
            logger.debug("datalog {}", response);

            datalog = gson.fromJson(response, RadioThermostatTstatDatalog.class);
            int cool_usage = datalog.getYesterday().getCool().getUsage();
            int heat_usage = datalog.getYesterday().getHeat().getUsage();
            logger.debug("usage cool: {} heat: {}", cool_usage, heat_usage);
            // updateState(CHANNEL_STATUS_HOURS, new QuantityType<>(info.getStatus().getHours(), SmartHomeUnits.HOUR));

            updateState(CHANNEL_HEAT_RUNTIME_YESTERDAY, new QuantityType<>(heat_usage, SmartHomeUnits.MINUTE));
            updateState(CHANNEL_COOL_RUNTIME_YESTERDAY, new QuantityType<>(cool_usage, SmartHomeUnits.MINUTE));

            goOnline();
        } catch (RadioThermostatCommunicationException | JsonSyntaxException e) {
            logger.debug("Unable to fetch data", e);
            goOffline(ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private synchronized void startUpdatesTask(int initialDelay) {
        stopUpdateTasks();
        updatesTask = scheduler.scheduleWithFixedDelay(this::updateData, initialDelay, refresh, TimeUnit.SECONDS);
    }

    @SuppressWarnings("null")
    private void stopUpdateTasks() {
        Future<?> localUpdatesTask = updatesTask;
        if (isFutureValid(localUpdatesTask)) {
            localUpdatesTask.cancel(false);
        }
    }

    private boolean isFutureValid(@Nullable Future<?> future) {
        return future != null && !future.isCancelled();
    }

    protected void goOnline() {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    protected void goOffline(ThingStatusDetail detail, String reason) {
        if (getThing().getStatus() != ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, detail, reason);
        }
    }

    @Override
    public void dispose() {
        stopUpdateTasks();
        if (httpClient.isStarted()) {
            try {
                httpClient.stop();
            } catch (Exception e) {
                logger.debug("Could not stop HttpClient", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <U extends Quantity<U>> QuantityType<U> commandToQuantityType(Command command, Unit<U> defaultUnit) {
        if (command instanceof QuantityType) {
            return (QuantityType<U>) command;
        }
        return new QuantityType<U>(new BigDecimal(command.toString()), defaultUnit);
    }

    protected DecimalType commandToDecimalType(Command command) {
        if (command instanceof DecimalType) {
            return (DecimalType) command;
        }
        return new DecimalType(new BigDecimal(command.toString()));
    }

    private BigDecimal quantityToRoundedTemperature(QuantityType<Temperature> quantity, Unit<Temperature> unit)
            throws IllegalArgumentException {
        QuantityType<Temperature> temparatureQuantity = quantity.toUnit(unit);
        if (temparatureQuantity == null) {
            return quantity.toBigDecimal();
        }

        BigDecimal value = temparatureQuantity.toBigDecimal();
        BigDecimal increment = CELSIUS == unit ? new BigDecimal("0.5") : new BigDecimal("1");
        BigDecimal divisor = value.divide(increment, 0, RoundingMode.HALF_UP);
        return divisor.multiply(increment);
    }

    @SuppressWarnings("serial")
    private class RadioThermostatCommunicationException extends Exception {
        public RadioThermostatCommunicationException(Exception e) {
            super(e);
        }

        public RadioThermostatCommunicationException(String message) {
            super(message);
        }
    }

    /**
     * Returns a copy of the properties map, that can be modified. The method {@link
     * BaseThingHandler#updateProperties(Map<String, String> properties)} must be called to persist the properties.
     *
     * @return copy of the thing properties (not null)
     */
    @Override
    protected Map<String, String> editProperties() {
        Map<String, String> properties = this.thing.getProperties();
        return new HashMap<>(properties);
    }

}
