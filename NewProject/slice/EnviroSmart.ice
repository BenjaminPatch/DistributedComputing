module EnviroSmart
{
    interface TemperatureManager
    {
        void processTemperature(string temp);
    }
    
    interface APManager
    {
    	void processAQI(string aqi);
    }
    
    interface LocationManager
    {
    	void processLocation(string loc);
    }
    
    interface AlarmManager
    {
    	void processAlarmMessage(string alarm);
    }
    
    interface LocationServer
    {
    	void processLocation(string loc);
    }
    interface Clock
    {
        void tick(string time);
    }
}