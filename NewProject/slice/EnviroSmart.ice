module EnviroSmart
{
	sequence<string> stringList;
    interface TemperatureManager
    {
        void processTemperature(string name, string temp);
    }
    
    struct Location
    {
    	string name;
		string locCode;
		stringList info;
		stringList services;
	}
    
    interface UiInteractor
    {
    	Location getInfoGivenLoc(string loc);
    	string getInfoCurrentLoc(string name);
    	bool logIn(string name);
		void signOut(string name);
    }
    
    interface WarningGenerator
    {
    	void generateWarning(string type, int value, int threshold,
    			string currentLocation, string suggestion, int weatherAlarm);
    }
    
    interface APManager
    {
    	void processAQI(string name, string aqi);
    }
    
    interface LocationManager
    {
    	void processLocation(string name, string loc, string indoorOutdoor);
    }
    
    interface AlarmManager
    {
    	void processAlarmMessage(string alarm);
    }
    
    interface PreLocationManager
    {
    	void processPreLocation(string name, string loc);
    	string respondToIndoorResponse(string indoorOrOutdoor);
    	string getInOrOut(string locCode);
    }
    
    interface PreferenceManager
    {
    	string processPreferenceRequest(string name, string req);
    	string getSuggestion(string nameAndEvent);
    	void shutdown();
    }
}