package components;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;

import EnviroSmart.TemperatureManager;

public class ContextManager {
	private Communicator communicator;
	
    public static void main(String[] args)
    {
    	
    	ContextManager server = new ContextManager();
    	server.communicator = com.zeroc.Ice.Util.initialize(args);
    	
    	System.out.println("Server starting.");
        
        com.zeroc.Ice.ObjectAdapter adapter = server.communicator.createObjectAdapterWithEndpoints("ContextManager", "default -p 10014");
        com.zeroc.Ice.Object tempManager = new TempManagerWorkerI();
        com.zeroc.Ice.Object apManager = new APManagerWorkerI();

        adapter.add(tempManager, com.zeroc.Ice.Util.stringToIdentity("TempManagerWorker"));
        adapter.add(apManager, com.zeroc.Ice.Util.stringToIdentity("APManagerWorker"));
        adapter.activate();
        
        System.out.println("Adapter activated. Waiting for data.");
        server.communicator.waitForShutdown();
        
        System.out.println("Server ending");
    }

	public static class TempManagerWorkerI implements EnviroSmart.TemperatureManager {

		@Override
		public void processTemperature(String temp, Current current) {
			System.out.println("TEMP RECEIVED: " + temp);
		}
	}

	public static class APManagerWorkerI implements EnviroSmart.APManager {

		@Override
		public void processAQI(String aqi, Current current) {
			System.out.println("AQI RECEIVED: " + aqi);
		}
	}
}
