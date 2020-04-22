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

        int status = 0;
        //
        // Try with resources block - communicator is automatically destroyed
        // at the end of this try block
        //
        try(com.zeroc.Ice.Communicator communicator = 
        		com.zeroc.Ice.Util.initialize(args, "configfiles\\config.pub")) {
            //
            // Install shutdown hook to (also) destroy communicator during JVM shutdown.
            // This ensures the communicator gets destroyed when the user interrupts the application with Ctrl-C.
            //
            Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));

            new TemperatureSensor(args[0], communicator);
        }
	}
	
	private static class TemperatureSensor extends Thread {
		private Communicator communicator;
		private String filename;
		private TemperatureManagerPrx tempManager;
		private Consumer<String> function;
		
		private TemperatureSensor(String name, com.zeroc.Ice.Communicator communicator) {
			communicator = 
	        		com.zeroc.Ice.Util.initialize(new String[] {name}, "configfiles\\config.pub");
			System.out.println("1");
			this.function = null;
			filename = name + "Temperature.txt";
	    	this.communicator = communicator;
			com.zeroc.IceStorm.TopicManagerPrx manager = 
					com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
					communicator.propertyToProxy("TopicManager.Proxy"));
			System.out.println("2");
			if(manager == null) {
				System.err.println("invalid proxy");
				return;
			}
			//
			// Retrieve the topic.
			//
			com.zeroc.IceStorm.TopicPrx topic;
			try {
				topic = manager.retrieve("tempSensor");
			} catch(com.zeroc.IceStorm.NoSuchTopic e) {
				System.out.println("3 nosuchtopic");
				try {
					topic = manager.create("tempSensor");
				} catch(com.zeroc.IceStorm.TopicExists i) {
					System.err.println("temporary failure, try again.");
					return;
				}
			}
			System.out.println("4");
			com.zeroc.Ice.ObjectPrx publisher = topic.getPublisher().ice_oneway();
			tempManager = TemperatureManagerPrx.uncheckedCast(publisher);
	    	this.function = tempManager::processTemperature;
			
	    	start();
		}
		
		public void run() {
			System.out.println("boom");
			List <String> allLines = Util.generateLines(filename);
			System.out.println("boom");
			while (!communicator.isShutdown()) {
				System.out.println("6");
				Util.iterateThroughLines(allLines, function, communicator);
			}
			System.out.println("7");
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
