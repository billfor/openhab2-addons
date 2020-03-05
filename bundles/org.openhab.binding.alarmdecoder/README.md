# alarmdecoder Binding

The [Alarm Decoder](http://www.alarmdecoder.com) from Nu Tech Software Solutions is a hardware adapter that interfaces with Ademco/Honeywell and DSC alarm panels.
It acts essentially like a keypad, reading and writing messages on the serial bus that connects keypads with the main panel.

There are several versions of the adapter available: 

* ad2pi (a board that plugs into a Raspberry Pi and so offers network-based TCP connectivity)
* ad2serial (serial port access)
* or ad2usb (emulated serial port via USB).

This binding allows openHAB to access the status of contacts and motion detectors connected to supported alarm panels, and also to optionally send keypad commands.

## Supported Things

The binding supports the following thing types:

* **ipbridge**
* **serialbridge**
* **keypad**
* **zone**
* **rfzone**
* **lrr**

## Discovery

Background discovery is currently supported for **zone** and **rfzone** things.
If the bridge *discovery* parameter is set to *true*, the first time a status message is seen from each zone or RF zone a corresponding thing will appear in the inbox.

## Thing Configuration

_Describe what is needed to manually configure a thing, either through the (Paper) UI or via a thing-file. This should be mainly about its mandatory and optional configuration parameters. A short example entry for a thing file can help!_

### ipbridge

* **hostname** (required) The hostname of the Alarm Decoder device
* **tcpPort** (default = 10000) TCP port number for the Alarm Decoder connection
* **discovery** (default = false) Enable automatic discovery of zones and RF zones
* **reconnect** (1-60, default = 5) The period in minutes that the handler will wait between connection attempts
* **heartbeat** (1-60, default = 5) The period in minutes between connection heartbeat checks

### serialbridge

* **serialPort** (required) The name of the serial port used to connect to the Alarm Decoder device
* **bitrate** Speed of the serial connection
* **discovery** (default=false) Enable automatic discovery of zones and RF zones

### zone

* **address** (required) Zone address
* **channel** (required) Zone channel

### rfzone

* **serial** (required) Serial number of the RF zone

### keypad

* **addressMask** (required) Keypad address mask (0 = All addresses)
* **sendCommands** (default = false) Allow keypad commands to be sent to the alarm system from openHAB. Enabling this means the alarm system will be only as secure as your openHAB system.

### lrr

* **partition** (default = 0) Partition for which to receive LRR events (0 = All)

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

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._

## Any custom content here!

## Quirks

The alarmdecoder device cannot query the panel for the state of individual zones.
For this reason, the binding puts contacts into the "unknown" state (UNDEF), *until the panel goes into the READY state*.
At that point, all contacts for which no messages have arrived are presumed to be in the CLOSED state.
In other words: to get to a clean slate after an openHAB restart, close all doors/windows such that the panel is READY.
