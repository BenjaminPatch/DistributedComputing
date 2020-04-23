package components;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.TopicPrx;

import EnviroSmart.PreferenceManager;

public class PreferenceRepository {
	public static class PreferenceManagerI implements PreferenceManager {
		@Override
		public void processPreferenceRequest(String req, Current current) {
			System.out.println("Request received at pref repository: " + req);
		}
		
	}

	private Communicator communicator;
    private static TopicPrx incomingProxy;
    private final static String PROXY = "TopicManager.Proxy";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: PreferenceRepository.java [filename]");
            System.exit(1);
        }
		Communicator communicator = 
				com.zeroc.Ice.Util.initialize(null, "configfiles\\config.sub");

		//
        // Destroy communicator during JVM shutdown
        //
        Thread destroyHook = new Thread(() -> communicator.destroy());
        // Runtime.getRuntime().addShutdownHook(destroyHook);

        int status = 0;
        try
        {
            status = run(communicator, destroyHook, args[0]);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            status = 1;
        }
    }

	public static int run(Communicator communicator, Thread destroyHook, String filename) {
		com.zeroc.IceStorm.TopicManagerPrx manager = com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
	            communicator.propertyToProxy(PROXY));
        if(manager == null) {
            System.err.println("invalid proxy");
            return 1;
        }
        
        com.zeroc.Ice.ObjectAdapter adapter = 
        		communicator.createObjectAdapter("EnviroSmart.PreLocationManager");
        incomingProxy = SubscriberUtil.getTopic("preLocSensor", communicator, manager);
        ObjectPrx sub = SubscriberUtil.getSubscriber(incomingProxy, new PreferenceManagerI(), communicator, adapter);
        adapter.activate();
        sub = sub.ice_oneway();
       
        //
        // Replace the shutdown hook to unsubscribe during JVM shutdown
        //
        final com.zeroc.IceStorm.TopicPrx incProxy = incomingProxy;
        final com.zeroc.Ice.ObjectPrx subscriberLoc = sub;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                incProxy.unsubscribe(subscriberLoc);
            } finally {
                communicator.destroy();
            }
        }));
        Runtime.getRuntime().removeShutdownHook(destroyHook); // remove old destroy-only shutdown hook
		return 0;
    }
}
