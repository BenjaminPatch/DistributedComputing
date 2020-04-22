package components;

import java.util.HashMap;

import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.Topic;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

public class ContManager extends com.zeroc.Ice.Application {
    private final String PROXY = "ContextManager.Proxy";
	@Override
	public int run(String[] arg0) {
		System.out.println("In cont manager");

		return 0;
	}
	
	TopicPrx getTopic(String topic) {
		TopicManagerPrx manager = com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
	            communicator().propertyToProxy("TopicManager.Proxy"));
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

    ObjectPrx getSubscriber(TopicPrx topic, String adapterName, 
    		com.zeroc.Ice.Object iceObject) {
        
        com.zeroc.Ice.ObjectAdapter adapter = 
        		communicator().createObjectAdapter(adapterName);
        
        com.zeroc.Ice.Identity id = new com.zeroc.Ice.Identity(null, "");
        id.name = java.util.UUID.randomUUID().toString();

        ObjectPrx subscriber = adapter.add(iceObject, id);
        adapter.activate();
        subscriber.ice_oneway();
       
        try {
            topic.subscribeAndGetPublisher(new HashMap<String, String>(),
                    subscriber);
        } catch (AlreadySubscribed e) {
            return subscriber;
        } catch (BadQoS e) {
        	System.err.println("BadQos");
        } catch (InvalidSubscriber e) {
        	System.err.println("Invalid Subscriber");
        	e.printStackTrace();
        }

        return subscriber;
    }

}
