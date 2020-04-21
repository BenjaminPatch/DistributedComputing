package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.zeroc.Ice.Communicator;

import EnviroSmart.APManagerPrx;
import EnviroSmart.TemperatureManagerPrx;

public class AllSensors {
	
	public static void main(String[] args) throws InterruptedException {
		if (args.length != 1) {
			System.out.println("Incorrect arg(s) for AllSensors");
			System.exit(1);
		}

		new TemperatureSensor(args[0]);
		new APSensor(args[0]);
		for (int i = 0 ; i < 8; i++) {
			System.out.println("IN MAIN");
		}
	}
	
	private static void iterateThroughLines(List<String> allLines, Consumer<String> function, Communicator communicator) {
		for (String line:allLines) {
			String[] splitLine = line.split(",");
			if (splitLine.length != 2) {
				// TODO: Discussion board question: handle
				// https://learn.uq.edu.au/webapps/discussionboard/do/message?action=list_messages&course_id=_128088_1&nav=discussion_board_entry&conf_id=_378320_1&forum_id=_441002_1&message_id=_1166089_1
			}
			for (int i = 0; i < Integer.parseInt(splitLine[1]); i++) {
				try {
					function.accept(splitLine[0]);
					Thread.sleep(1000);
				} catch (NumberFormatException e) {
					System.err.println("data file not of correct format");
					communicator.shutdown();
				} catch (InterruptedException e) {
					communicator.shutdown();
				}
			}
		}
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
			List <String> allLines = generateLines(filename);
			System.out.println("In tempSensor");
			while (true) {
				iterateThroughLines(allLines, function, communicator);
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
			List <String> allLines = generateLines(filename);
			System.out.println("In apSensor");
			while (true) {
				iterateThroughLines(allLines, function, communicator);
			}
		}
		
		public void shutdown() {
			communicator.shutdown();
			System.exit(0);
		}
	}
	
	private static List<String> generateLines(String filename) {
		List<String> allLines = new LinkedList<>();
		// Read all lines into LinkedList
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
			while (line != null) {
				allLines.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return allLines;
	}

	private static class SensorThread extends Thread {
		private Communicator communicator;
		private String filename;
		final Consumer<String> function;

		private SensorThread(String name, String type) {
			this.function = null;
			start();
		}
		
		private static void apSensor(String line) {
			System.out.println("In APSensor");
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
