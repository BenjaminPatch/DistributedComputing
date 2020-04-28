package components;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.zeroc.Ice.Communicator;

import EnviroSmart.AlarmManagerPrx;

/*
 * Send a message every 60 seconds with new weather information
 */
public class WeatherAlarm extends Thread {
	private Communicator communicator;
	private String filename;
	private final String TOPIC_NAME = "weatherSensor";
	AlarmManagerPrx alarmManager;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: WeatherAlarm.java filename");
		}
		new WeatherAlarm(args[0]);
	}

	private WeatherAlarm(String name) {
		this.filename = name;
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
			topic = manager.retrieve(TOPIC_NAME);
		} catch(com.zeroc.IceStorm.NoSuchTopic e) {
			try {
				topic = manager.create(TOPIC_NAME);
			} catch(com.zeroc.IceStorm.TopicExists i) {
				System.err.println("temporary failure, try again.");
				return;
			}
		}

		com.zeroc.Ice.ObjectPrx publisher = topic.getPublisher().ice_oneway();
		this.alarmManager = AlarmManagerPrx.uncheckedCast(publisher);
		start();
	}
	
	public void run() {
		List <String> allLines = Util.generateLines(filename);
		while (!communicator.isShutdown()) {
			for (String line: allLines) {
				try {
					int code = Integer.parseInt(line);
					if (code < 0 || code > 3) {
						System.err.println("Error in weather alarm");
						System.exit(1);
					}
				} catch (NumberFormatException e) {
					System.err.println("Error in weather alarm");
					System.exit(1);
				}

				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
				LocalDateTime now = LocalDateTime.now();
				System.out.println(dtf.format(now) + " Sent \"" + line + "\"");

				alarmManager.processAlarmMessage(line);
				try {
					Thread.sleep(60 * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
