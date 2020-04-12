package org.openhab.binding.radiothermostat.internal.data;

public class RadioThermostatTstatDatalog {

    // private RunMode today; // don't really need this during the day?
    private RunMode yesterday;

    public RunMode getYesterday() {
        return yesterday;
    }

    public class RunMode {
        private Datalog heat_runtime;
        private Datalog cool_runtime;

        public Datalog getHeat() {
            return heat_runtime;
        }

        public Datalog getCool() {
            return cool_runtime;
        }
    }

    public class Datalog {
        private int hour;
        private int minute;

        public int getUsage() {
            /*
             * No point in doing this if we can't [easily] persist
             * ZonedDateTime foo= new ZonedDateTime(null, null, ZoneId.systemDefault());
             * ZonedDateTime elapsedTime=ZonedDateTime.plusMinutes(minute).plusHours(hour);
             * private DateTimeType test= new DateTimeType(elapsedTime);
             */
            return hour * 60 + minute;
        }
    }
}
