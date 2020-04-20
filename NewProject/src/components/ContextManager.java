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
        
        com.zeroc.Ice.ObjectAdapter adapter = server.communicator.createObjectAdapterWithEndpoints("ContextManager", "default -p 10001");
        com.zeroc.Ice.Object tempManager = new TempManagerWorkerI();

        adapter.add(tempManager, com.zeroc.Ice.Util.stringToIdentity("TempManagerWorker"));
        adapter.activate();
        
        System.out.println("Adapter activated. Waiting for data.");
        server.communicator.waitForShutdown();
        
        System.out.println("Server ending");
    }

	public static class TempManagerWorkerI implements EnviroSmart.TemperatureManager {

		@Override
		public void processTemperature(int temp, Current current) {
			System.out.println("TEMP RECEIVED: " + temp);
		}
	}
}
