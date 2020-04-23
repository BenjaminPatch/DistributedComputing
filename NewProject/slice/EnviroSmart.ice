module EnviroSmart
{
    interface TemperatureManager
    {
        void processTemperature(string name, string temp);
    }
    
    interface APManager
    {
    	void processAQI(string name, string aqi);
    }
    
    interface LocationManager
    {
    	void processLocation(string name, string loc);
    }
    
    interface AlarmManager
    {
    	void processAlarmMessage(string alarm);
    }
    
    interface PreLocationManager
    {
    	void processPreLocation(string name, string loc);
    }
    
    interface PreferenceManager
    {
    	void processPreferenceRequest(string req);
    }
}