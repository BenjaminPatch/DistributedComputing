package components;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;

import EnviroSmart.TemperatureManager;


public class LocationServer {
	private Communicator communicator;
    public static void main(String[] args)
    {
    	
    	LocationServer server = new LocationServer();
    	server.communicator = com.zeroc.Ice.Util.initialize(args);
    	
    	System.out.println("Server starting.");
        
        com.zeroc.Ice.ObjectAdapter adapter = server.communicator.createObjectAdapterWithEndpoints("LocationServer", "default -p 10013");
        com.zeroc.Ice.Object locManager = new LocationServerWorkerI();

        adapter.add(locManager, com.zeroc.Ice.Util.stringToIdentity("LocationServer"));
        adapter.activate();
        
        System.out.println("Adapter activated. Waiting for data.");
        server.communicator.waitForShutdown();
        
        System.out.println("Server ending");
    }

	public static class LocationServerWorkerI implements EnviroSmart.LocationServer {

		@Override
		public void processLocation(String temp, Current current) {
			System.out.println("LOC RECEIVED: " + temp);
		}
	}
}
