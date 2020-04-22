package components;

import java.util.HashMap;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.TopicPrx;

import EnviroSmart.TemperatureManager;

public class ContextManager {

    public static class TempManagerI implements TemperatureManager {

		@Override
		public void processTemperature(String temp, Current current) {
			System.out.println("TEMP: " + temp);
		}
    }
    private final static String PROXY = "TopicManager.Proxy";
    private static TopicPrx tempProxy;
    private TopicPrx locProxy;
    private TopicPrx aqiProxy;
	
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ContextManager.java [filename]");
            System.exit(1);
        }
		java.util.List<String> extraArgs = new java.util.ArrayList<String>();
		Communicator communicator = 
				com.zeroc.Ice.Util.initialize(args, "configfiles\\config.sub");

		//
        // Destroy communicator during JVM shutdown
        //
        Thread destroyHook = new Thread(() -> communicator.destroy());
        // Runtime.getRuntime().addShutdownHook(destroyHook);

        int status = 0;
        try
        {
            status = run(communicator, destroyHook, extraArgs.toArray(new String[extraArgs.size()]));
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            status = 1;
        }
    }

	public static int run(Communicator communicator, Thread destroyHook, String[] args) {
		com.zeroc.IceStorm.TopicManagerPrx manager = com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
	            communicator.propertyToProxy(PROXY));
        if(manager == null) {
            System.err.println("invalid proxy");
            return 1;
        }
        tempProxy = getTopic("tempSensor", communicator, manager);
        ObjectPrx tempSub = getSubscriber(tempProxy, "EnviroSmart.TemperatureManager", new TempManagerI(), communicator);

        //
        // Replace the shutdown hook to unsubscribe during JVM shutdown
        //
        final com.zeroc.IceStorm.TopicPrx topicF = tempProxy;
        final com.zeroc.Ice.ObjectPrx subscriberF = tempSub;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                topicF.unsubscribe(subscriberF);
            } finally {
                communicator.destroy();
            }
        }));
        Runtime.getRuntime().removeShutdownHook(destroyHook); // remove old destroy-only shutdown hook
		return 0;
	}
	
	static TopicPrx getTopic(String topic, Communicator communicator, com.zeroc.IceStorm.TopicManagerPrx manager) {
        if(manager == null) {
            System.err.println("invalid proxy");
            return null;
        }
	       
        TopicPrx topicObj;

        //
        // Retrieve the topic.
        //
        try {
            topicObj = manager.retrieve(topic);
        }
        catch(com.zeroc.IceStorm.NoSuchTopic e) {
            try {
                topicObj = manager.create(topic);
            }
            catch(com.zeroc.IceStorm.TopicExists ex) {
                System.err.println("temporary failure, try again.");
                return null;
            }
        }
		return topicObj;
	}

    static ObjectPrx getSubscriber(TopicPrx topic, String adapterName, 
    		com.zeroc.Ice.Object iceObject, Communicator communicator) {
        
        com.zeroc.Ice.ObjectAdapter adapter = 
        		communicator.createObjectAdapter(adapterName);
        
        com.zeroc.Ice.Identity id = new com.zeroc.Ice.Identity(null, "");
        id.name = java.util.UUID.randomUUID().toString();

        ObjectPrx subscriber = adapter.add(iceObject, id);
        adapter.activate();
        subscriber = subscriber.ice_oneway();
       
        try {
            topic.subscribeAndGetPublisher(new HashMap<String, String>(),
                    subscriber);
        } catch (AlreadySubscribed e) {
            return subscriber;
        } catch (BadQoS e) {
        	System.out.println("BadQos");
        } catch (InvalidSubscriber e) {
        	System.out.println("Invalid Subscriber");
        	e.printStackTrace();
        }
        return subscriber;
    }

}
