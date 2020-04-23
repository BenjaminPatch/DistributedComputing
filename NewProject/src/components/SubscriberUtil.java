package components;

import java.util.HashMap;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.TopicPrx;

public final class SubscriberUtil {
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

    static ObjectPrx getSubscriber(TopicPrx topic, com.zeroc.Ice.Object iceObject, 
    		Communicator communicator, com.zeroc.Ice.ObjectAdapter adapter) {
        
        com.zeroc.Ice.Identity id = new com.zeroc.Ice.Identity(null, "");
        id.name = java.util.UUID.randomUUID().toString();

        ObjectPrx subscriber = adapter.add(iceObject, id);
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
