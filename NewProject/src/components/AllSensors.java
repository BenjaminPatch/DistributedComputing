package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.zeroc.Ice.Communicator;

import EnviroSmart.TemperatureManagerPrx;

public class AllSensors {
	
	public static void main(String[] args) throws InterruptedException {
		if (args.length != 1) {
			System.out.println("Incorrect arg(s) for AllSensors");
			System.exit(1);
		}

		new SensorThread(args[0], "TemperatureSensor");
		new SensorThread(args[0], "APSensor");
		new SensorThread(args[0], "LocationSensor");
		for (int i = 0 ; i < 8; i++) {
			System.out.println("IN MAIN");
		}
	}

	private static class SensorThread extends Thread {
		private Communicator communicator;
		private String filename;
		final Consumer<String> function;

		private SensorThread(String name, String type) {
			switch (type) {
			case "APSensor":
				this.filename = name + "AQI.txt";
				function = SensorThread::apSensor;
				break;
			case "TemperatureSensor":
				this.filename = name + "Temperature.txt";
				function = SensorThread::temperatureSensor;
				break;
			case "LocationSensor":
				this.filename = name + "Location.txt";
				function = SensorThread::locationSensor;
				break;
			default:
				// System.err.println("Fatal: Incorrect sensor name given.");
				// TODO: 6.1: Error handling
				function = null;
				System.exit(1);
			}
			start();
		}
		
		private static void apSensor(String line) {
			System.out.println("In APSensor");
		}

		private static void temperatureSensor(String line) {
			System.out.println("In tempSensor");
			Communicator communicator = com.zeroc.Ice.Util.initialize();
	    	communicator = com.zeroc.Ice.Util.initialize();
	    	com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("TempManagerWorker:default -p 10001");
	    	TemperatureManagerPrx prx = TemperatureManagerPrx.checkedCast(base);
	    	
	    	if (prx == null) {
	    		System.out.println("PRX == NULL");
	    	}
	    	prx.processTemperature(Integer.parseInt(line));
		}

		private static void locationSensor(String line) {
			System.out.println("In locationSensor");
		}

		public void run() {
			List<String> allLines = new LinkedList<>();
			// Read all lines into LinkedList
			try {
				BufferedReader reader = new BufferedReader(new FileReader(filename));
				String line = reader.readLine();
				while (line != null) {
					allLines.add(line);
					System.out.println(line);
					line = reader.readLine();
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				System.out.println("HELLO");
				for (String line : allLines) {
					String[] splitLine = line.split(",");
					if (splitLine.length != 2) {
						// TODO: Discussion board question: handle
						// https://learn.uq.edu.au/webapps/discussionboard/do/message?action=list_messages&course_id=_128088_1&nav=discussion_board_entry&conf_id=_378320_1&forum_id=_441002_1&message_id=_1166089_1
					}
					int time = 0;
					try {
						time = Integer.parseInt(splitLine[1]);
					} catch (NumberFormatException e) {
						// TODO: Handle
						System.exit(1);
					}
					for (int i = 0; i < time; i++) {
						function.accept(splitLine[0]);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							System.err.println("Sensor Interrupted");
							System.exit(1);
						}
					}
				}
			}
		}
	}
}
