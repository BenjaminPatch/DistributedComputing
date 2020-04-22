package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.zeroc.Ice.Communicator;

import EnviroSmart.APManagerPrx;
import EnviroSmart.LocationManagerPrx;
import EnviroSmart.LocationServerPrx;
import EnviroSmart.TemperatureManagerPrx;

import components.Util;

public class AllSensors {
	
	public static void main(String[] args) throws InterruptedException {
		if (args.length != 1) {
			System.err.println("Incorrect arg(s) for AllSensors");
			System.exit(1);
		}

		/*
		new TemperatureSensor(args[0]);
		new APSensor(args[0]);
		new LocationSensor(args[0]);
		*/
        int status = 0;

        //
        // Try with resources block - communicator is automatically destroyed
        // at the end of this try block
        //
        System.out.println("a");
        try(com.zeroc.Ice.Communicator communicator = 
        		com.zeroc.Ice.Util.initialize(args, "configfiles\\config.pub")) {
            //
            // Install shutdown hook to (also) destroy communicator during JVM shutdown.
            // This ensures the communicator gets destroyed when the user interrupts the application with Ctrl-C.
            //
            Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));

            status = run(communicator);
            System.out.println(status);
        }
        System.out.println("b");
        System.exit(status);
	}
	
	private static int run(com.zeroc.Ice.Communicator communicator) {
		com.zeroc.IceStorm.TopicManagerPrx manager = com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
	            communicator.propertyToProxy("TopicManager.Proxy"));
	    if(manager == null) {
            System.err.println("invalid proxy");
            return 1;
        }
	    //
        // Retrieve the topic.
        //
        com.zeroc.IceStorm.TopicPrx topic;
        try
        {
            topic = manager.retrieve("tempSensor");
        }
        catch(com.zeroc.IceStorm.NoSuchTopic e)
        {
            try
            {
                topic = manager.create("tempSensor");
            }
            catch(com.zeroc.IceStorm.TopicExists ex)
            {
                System.err.println("temporary failure, try again.");
                return 1;
            }
        }
        com.zeroc.Ice.ObjectPrx publisher = topic.getPublisher().ice_oneway();
        TemperatureManagerPrx tempManager = TemperatureManagerPrx.uncheckedCast(publisher);
        while (!communicator.isShutdown()) {
        	tempManager.processTemperature("YEETUS");
			System.out.println("sending yeetus");
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.err.println("interrupted");
			}
        }
		return 0;
	}
	
	private static class TemperatureSensor extends Thread {
		private Communicator communicator;
		private String filename;
		private TemperatureManagerPrx prx;
		final Consumer<String> function;
		
		private TemperatureSensor(String name) {
			filename = name + "Temperature.txt";
			communicator = com.zeroc.Ice.Util.initialize();
	    	com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("TempManagerWorker:default -p 10014");
	    	prx = TemperatureManagerPrx.checkedCast(base);
	    	function = prx::processTemperature;
	    	
	    	if (prx == null) {
	    		System.err.println("PRX == NULL");
	    	}
	    	start();
		}
		
		public void run() {
			List <String> allLines = Util.generateLines(filename);
			while (!communicator.isShutdown()) {
				Util.iterateThroughLines(allLines, function, communicator);
			}
		}
		
		public void shutdown() {
			communicator.shutdown();
			System.exit(0);
		}
	}

	private static class APSensor extends Thread {
		private Communicator communicator;
		private String filename;
		private APManagerPrx prx;
		final Consumer<String> function;
		
		private APSensor(String name) {
			filename = name + "AQI.txt";
			communicator = com.zeroc.Ice.Util.initialize();
	    	com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("APManagerWorker:default -p 10014");
	    	prx = APManagerPrx.checkedCast(base);
	    	function = prx::processAQI;
	    	
	    	if (prx == null) {
	    		System.err.println("PRX == NULL");
	    	}
	    	start();
		}
		
		public void run() {
			List <String> allLines = Util.generateLines(filename);
			while (!communicator.isShutdown()) {
				Util.iterateThroughLines(allLines, function, communicator);
			}
		}
		
		public void shutdown() {
			communicator.shutdown();
			System.exit(0);
		}
	}
	
	private static class LocationSensor extends Thread {
		private Communicator communicator;
		private String filename;
		private LocationServerPrx prx;
		final Consumer<String> function;
		
		private LocationSensor(String name) {
			filename = name + "Location.txt";
			communicator = com.zeroc.Ice.Util.initialize();
	    	com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("LocationServer:default -p 10013");
	    	prx = LocationServerPrx.checkedCast(base);
	    	function = prx::processLocation;
	    	
	    	if (prx == null) {
	    		System.err.println("PRX == NULL");
	    	}
	    	start();
		}
		
		public void run() {
			List <String> allLines = Util.generateLines(filename);
			while (!communicator.isShutdown()) {
				Util.iterateThroughLines(allLines, function, communicator);
			}
		}
		
		public void shutdown() {
			communicator.shutdown();
			System.exit(0);
		}
	}
	
}
