package org.openhab.binding.radiothermostat.internal.data;

public class RadioThermostatTstatDatalog {
    public class RunDate {
        private RunMode today;
        private RunMode yesterday;

        public RunDate(RunMode today, RunMode yesterday) {
            this.today = today;
            this.yesterday = yesterday;
        }
    }

    public class RunMode {
        private Datalog heat_runtime;
        private Datalog cool_runtime;

        public Datalog getHeat() {
            return heat_runtime;
        }

        public RunMode(Datalog heat_runtime, Datalog cool_runtime) {
            this.heat_runtime = heat_runtime;
            this.cool_runtime = cool_runtime;
        }
    }

    public class Datalog {
        private int hour;
        private int minute;

        public int getUsage() {
            return hour;
        }

        public Datalog(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }
    }

    RunDate theDay;

    public RunMode getYesterday() {
        return theDay.yesterday;
    }

    RunMode theMode;

    public Datalog getMode() {
        return theMode.heat_runtime;
    }

    Datalog theData;

    public int getUsage() {
        return theData.hour;
    }

    public RadioThermostatTstatDatalog() {
    }

    public RadioThermostatTstatDatalog(RunDate theDay, RunMode theMode, Datalog theData) {
        this.theDay = theDay;
        this.theMode = theMode;
        this.theData = theData;
    }

    public int getLog() {
        return theDay.today.heat_runtime.hour;
    }
}
