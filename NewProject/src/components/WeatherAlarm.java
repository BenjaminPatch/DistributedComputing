package components;

import java.util.List;
import java.util.function.Consumer;

import com.zeroc.Ice.Communicator;

import EnviroSmart.AlarmManagerPrx;

import components.Util;

/*
 * Send a message every 60 seconds with new weather information
 */
public class WeatherAlarm extends Thread {
	private Communicator communicator;
	private String filename;
	private AlarmManagerPrx prx;
	final Consumer<String> function;
	
	private WeatherAlarm(String name) {
		filename = name;
		communicator = com.zeroc.Ice.Util.initialize();
    	com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("WeatherAlarmManagerWorker:default -p 10014");
    	prx = AlarmManagerPrx.checkedCast(base);
    	function = prx::processAlarmMessage;
    	
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
