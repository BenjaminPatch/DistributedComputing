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
}