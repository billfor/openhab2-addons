# alarmdecoder Binding

The [Alarm Decoder](http://www.alarmdecoder.com) from Nu Tech Software Solutions is a hardware adapter that interfaces with Ademco/Honeywell and DSC alarm panels.
It acts essentially like a keypad, reading and writing messages on the serial bus that connects keypads with the main panel.

There are several versions of the adapter available: 

* *ad2pi* - A board that plugs into a Raspberry Pi and so offers network-based TCP connectivity
* *ad2serial* - Attached via a serial port
* *ad2usb* - Attached via USB

This binding allows openHAB to access the status of wired or wireless contacts and motion detectors connected to supported alarm panels, as well as the state of attached keypads and messages send to attached LRR devices.
Support is also available for sending keypad commands, including special/programmable keys if they are supported by your panel.

## Supported Things

The binding supports the following thing types:

* **ipbridge** - Supports TCP connection to the AD.
* **serialbridge** - Supports serial/USB connection to the AD.
* **keypad** - Reports keypad status and optionally sends keypad messages.
* **zone** - Reports status from zone expanders and relay expanders, and also from built-in zones via emulation.
* **rfzone** - Reports status from RF zones
* **lrr** - Reports messages sent to Long Range Radio (LRR) or emulated LRR device.

## Discovery

Background discovery is currently supported for **zone** and **rfzone** things.
If the bridge *discovery* parameter is set to *true*, the first time a status message is seen from each zone or RF zone a corresponding thing will appear in the inbox.

## Thing Configuration

Alarm Decoder things can be configured through openHAB's management UI, or manually via configuration files.

### ipbridge

The **ipbridge** thing supports a TCP connection to an Alarm Decoder device such as *ad2pi*.

* **hostname** (required) The hostname of the Alarm Decoder device
* **tcpPort** (default = 10000) TCP port number for the Alarm Decoder connection
* **discovery** (default = false) Enable automatic discovery of zones and RF zones
* **reconnect** (1-60, default = 2) The period in minutes that the handler will wait between connection checks and connection attempts
* **heartbeat** (1-60, default = 5) The period in minutes after which the connection will be reset if no valid messages have been received

Example:

```
TBD
```

### serialbridge

The **serialbridge** thing supports a serial or USB connection to an Alarm Decoder device such as *ad2serial* or *ad2usb*.

Parameters:

* **serialPort** (required) The name of the serial port used to connect to the Alarm Decoder device
* **bitrate** Speed of the serial connection
* **discovery** (default=false) Enable automatic discovery of zones and RF zones

Example:

```
TBD
```

### zone

The **zone** thing reports status from zone expanders and relay expanders, and also from built-in zones via emulation.

Parameters:

* **address** (required) Zone address
* **channel** (required) Zone channel

Example:

```
TBD
```

### rfzone

The **rfzone** thing reports status from wireless zones, such as 5800 series RF devices, if your alarm panel has an RF receiver.

Parameters:

* **serial** (required) Serial number of the RF zone

Example:

```
TBD
```

### keypad

The **keypad** thing reports keypad status and optionally sends keypad messages.
For panels that support multiple keypad addresses, it can be configured with an address mask of one or more keypad(s) for which it will receive messages.
When sending messages, it will send from the configured keypad address if only one is configured.
If a mask containing multiple addresses or 0 (all) is configured, it will send messages from the Alarm Decoder's configured address.

Parameters:

* **addressMask** (required) Keypad address mask (0 = All addresses)
* **sendCommands** (default = false) Allow keypad commands to be sent to the alarm system from openHAB. Enabling this means the alarm system will be only as secure as your openHAB system.

Example:

```
TBD
```

### lrr

The **lrr** thing reports messages sent to a Long Range Radio (LRR) or emulated LRR device.
These are specifically formatted messages as described in the [SIA DC-05-1999.09](http://www.alarmdecoder.com/wiki/index.php/File:SIA-ContactIDCodes_Protocol.pdf) standard for Contact ID reporting.
For panels that support multiple partitions, the partition for which a given lrr thing will receive messages can be defined.

* **partition** (default = 0) Partition for which to receive LRR events (0 = All)

Example:

```
TBD
```

## Channels

The alarmdecoder things expose the following channels:

**zone**

|  channel     | type    |RO/RW| description                  |
|--------------|---------|-----|------------------------------|
| contact      | Contact |RO   |Zone contact state            |

**rfzone**

|  channel     | type    |RO/RW| description                  |
|--------------|---------|-----|------------------------------|
| lowbat       | Switch  | RO  |Low battery                   |
| supervision  | Switch  | RO  |Supervision required          |
| loop1        | Contact | RO  |Loop 1 state                  |
| loop2        | Contact | RO  |Loop 2 state                  |
| loop3        | Contact | RO  |Loop 3 state                  |
| loop4        | Contact | RO  |Loop 4 state                  |

**keypad**

|  channel     | type    |RO/RW| description                  |
|--------------|---------|-----|------------------------------|
| zone         | Number  | RO  |Zone number for status        |
| text         | String  | RO  |Keypad message text           |
| armedaway    | Switch  | RO  |Armed/Away Indicator          |
| armedhome    | Switch  | RO  |Armed/Stay Indicator          |
| backlight    | Switch  | RO  |Keypad backlight on           |
| program      | Switch  | RO  |Programming mode              |
| beeps        | Number  | RO  |Number of beeps for message   |
| bypassed     | Switch  | RO  |Zone bypassed                 |
| acpower      | Switch  | RO  |Panel on AC power             |
| chime        | Switch  | RO  |Chime enabled                 |
| alarmoccurred| Switch  | RO  |Alarm occurred in the past    |
| alarm        | Switch  | RO  |Alarm is currently sounding   |
| lowbat       | Switch  | RO  |Low battery warning           |
| delayoff     | Switch  | RO  |Entry delay off               |
| fire         | Switch  | RO  |Fire detected                 |
| sysfault     | Switch  | RO  |System fault                  |
| perimeter    | Switch  | RO  |Perimeter only                |
| command      | String  | RW  |Keypad command                |

**lrr**

|  channel     | type    |RO/RW| description                  |
|--------------|---------|-----|------------------------------|
| partition    | Number  | RO  |Partition number (0=system)   |
| eventdata    | Number  | RO  |CID event data (user or zone) |
| cidmessage   | String  | RO  |SIA Contact ID Protocol msg.  |
| reportcode   | String  | RO  |CID report code               |

## Full Example

TODO: Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap).

## Any custom content here!

## Quirks

The alarmdecoder device cannot query the panel for the state of individual zones.
For this reason, the binding puts contacts into the "unknown" state (UNDEF), *until the panel goes into the READY state*.
At that point, all contacts for which no messages have arrived are presumed to be in the CLOSED state.
In other words: to get to a clean slate after an openHAB restart, close all doors/windows such that the panel is READY.
