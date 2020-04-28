package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.zeroc.Ice.Communicator;

import EnviroSmart.APManagerPrx;
import EnviroSmart.LocationManagerPrx;
import EnviroSmart.PreLocationManagerPrx;
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

            new TemperatureSensor(args[0]);
            new APSensor(args[0]);
            new LocationSensor(args[0]);
        }
	}
	
	private static abstract class AbstractSensor extends Thread {
		private Communicator communicator;
		private String filename;
		private String username;
		private BiConsumer<String, String> function;

		private AbstractSensor(String name, String topicName) {
			this.username = name;
			this.function = null;
			this.communicator = 
	        		com.zeroc.Ice.Util.initialize(null, "configfiles\\config.pub");
			com.zeroc.IceStorm.TopicManagerPrx manager = 
					com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
					communicator.propertyToProxy("TopicManager.Proxy"));
			if(manager == null) {
				System.err.println("invalid proxy");
				return;
			}
			//
			// Retrieve the topic.
			//
			com.zeroc.IceStorm.TopicPrx topic;
			try {
				topic = manager.retrieve(topicName);
			} catch(com.zeroc.IceStorm.NoSuchTopic e) {
				try {
					topic = manager.create(topicName);
				} catch(com.zeroc.IceStorm.TopicExists i) {
					System.err.println("temporary failure, try again.");
					return;
				}
			}

			com.zeroc.Ice.ObjectPrx publisher = topic.getPublisher().ice_oneway();

			switch (topicName) {
			case "tempSensor":
				TemperatureManagerPrx tempManager;
				tempManager = TemperatureManagerPrx.uncheckedCast(publisher);
				this.filename = name + "Temperature.txt";
				this.function = tempManager::processTemperature;
				break;
			case "preLocSensor":
				PreLocationManagerPrx locManager;
				locManager = PreLocationManagerPrx.uncheckedCast(publisher);
				this.filename = name + "Location.txt";
				this.function = locManager::processPreLocation;
				break;
			case "apSensor":
				APManagerPrx apManager;
				apManager = APManagerPrx.uncheckedCast(publisher);
				this.filename = name + "AQI.txt";
				this.function = apManager::processAQI;
				break;
			}
		}
		
		public void run() {
			List <String> allLines = Util.generateLines(filename);
			while (!communicator.isShutdown()) {
				iterateThroughLines(username, allLines, function, this.communicator);
			}
		}
		
	}
	
	private static class TemperatureSensor extends AbstractSensor {

		private TemperatureSensor(String name) {
			super(name, "tempSensor");
	    	start();
		}
	}
	
	private static class APSensor extends AbstractSensor {

		private APSensor(String name) {
			super(name, "apSensor");
	    	start();
		}
	}
	
	private static class LocationSensor extends Thread {
		Communicator communicator;
		PreLocationManagerPrx locProxy;
		String name;
		private BiConsumer<String, String> function;

		private LocationSensor(String name) {
			this.name = name;
			this.communicator = com.zeroc.Ice.Util.initialize(new String[] {name});
			com.zeroc.Ice.ObjectPrx base = this.communicator.stringToProxy("LocationMiddleman:default -p 10023");
			this.locProxy = PreLocationManagerPrx.checkedCast(base);
			
			if(this.locProxy == null)
			{
				throw new Error("Invalid proxy");
			}
			this.function = locProxy::processPreLocation;
			start();
		}
		
		public void run() {
			List <String> allLines = Util.generateLines(this.name + "Location.txt");
			while (!this.communicator.isShutdown()) {
				iterateThroughLines(name, allLines, this.function, this.communicator);
			}
		}
	}
	
	static void iterateThroughLines(String username, List<String> allLines, BiConsumer<String, String> function, Communicator communicator) {
		for (String line:allLines) {
			String[] splitLine = line.split(",");
			if (splitLine.length != 2) {
				// TODO: Discussion board question: handle
				// https://learn.uq.edu.au/webapps/discussionboard/do/message?action=list_messages&course_id=_128088_1&nav=discussion_board_entry&conf_id=_378320_1&forum_id=_441002_1&message_id=_1166089_1
			}

			for (int i = 0; i < Integer.parseInt(splitLine[1]); i++) {
				if (communicator.isShutdown()) {
					break;
				} try {
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
					LocalDateTime now = LocalDateTime.now();
					if (Character.isLetter(splitLine[0].charAt(0))) {
						System.out.println(dtf.format(now) + " Sent LocationServer \"" + username + " " + splitLine[0] + "\"");
					} else {
						System.out.println(dtf.format(now) + " Sent \"" + username + " " + splitLine[0] + "\"");
					}
					function.accept(username, splitLine[0]);
					Thread.sleep(1000);
				} catch (NumberFormatException e) {
					System.err.println("data file not of correct format");
					communicator.shutdown();
				} catch (InterruptedException e) {
					System.out.println("interrupted");
					communicator.shutdown();
				}
			}
		}
	}
}
