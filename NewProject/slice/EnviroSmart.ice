module EnviroSmart
{
    interface TemperatureManager
    {
        void processTemperature(string name, string temp);
    }
    
    interface InfoProvider
    {
    	string getInfoGivenLoc(string loc);
    	string getInfoCurrentLoc (
    
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
    	string respondToIndoorResponse(string indoorOrOutdoor);
    }
    
    interface PreferenceManager
    {
    	string processPreferenceRequest(string name, string req);
    	void shutdown();
    }
}