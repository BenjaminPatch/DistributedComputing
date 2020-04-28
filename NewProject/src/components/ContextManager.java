package components;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.TopicPrx;

import EnviroSmart.APManager;
import EnviroSmart.AlarmManager;
import EnviroSmart.LocationManager;
import EnviroSmart.PreLocationManagerPrx;
import EnviroSmart.PreferenceManagerPrx;
import EnviroSmart.TemperatureManager;
import EnviroSmart.UiInteractor;
import EnviroSmart.WarningGeneratorPrx;
import javafx.util.Pair;

public class ContextManager {
	
	public static class UiInteractorI implements UiInteractor {

		@Override
		public EnviroSmart.Location getInfoGivenLoc(String loc, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received EnviroAppUI \"" + loc + "\"");

			for (String key:locs.keySet()) {
				for (Location cur: locs.get(key)) {
					if (cur.getName().equalsIgnoreCase(loc)) {
						Object[] infoObj = cur.getInfo().toArray();
						Object[] servicesObj = cur.getServices().toArray();
						String[] info = 
								  Arrays.copyOf(infoObj, infoObj.length, String[].class);
						String[] services = 
								  Arrays.copyOf(servicesObj, servicesObj.length, String[].class);
						return new EnviroSmart.Location(loc, cur.getLocCode(), 
								info, services);
					}
				}
			}
			return null;
		}

		@Override
		public String getInfoCurrentLoc(String name, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received EnviroAppUI \"" + name + "\"");

			String loc = userLocations.get(name);
			if (loc == "") {
				return null;
			}
			String returnMessage = "";
			List<Location> locNames = locs.get(loc);
			for (Location cur : locNames) {
				returnMessage = returnMessage.concat(cur.getName() + ", ");
			}
			return returnMessage.substring(0, returnMessage.length() -2).trim();
		}

		@Override
		public boolean logIn(String name, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received EnviroAppUI \"" + name + "\"");

			PreferenceManagerPrx prefManager = getPrefRepo().getKey();
			String tempPref = prefManager.processPreferenceRequest(name, "pref1");

			if (tempPref == null || tempPref.length() == 0) {
				return false;
			}
			userLocations.putIfAbsent(name, ""); // managers now know to start
			tempThresholds.put(name, Integer.parseInt(tempPref.split(":")[0]));
			userComms.put(name, getWarningGen());
			return true;
		}
		
		@Override
		public void signOut(String name, Current current) {
			userLocations.remove(name);
			if (userLocations.isEmpty()) {
				shutdown.set(true);
			}
		}
	}

    public static class TempManagerI implements TemperatureManager {
    	private boolean isAboveThreshold;
    	
    	public TempManagerI() {
    		super();
    		isAboveThreshold = false;
    	}
    	
		@Override
		public void processTemperature(String name, String temp, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received EnviroAppUI \"" + name + "\"");

			Integer threshold = tempThresholds.get(name);
			if (threshold == null) {
				return;
			}
			Integer newTemp = Integer.parseInt(temp);
			
			System.out.println(dtf.format(now) + " Sent EnviroAppUi \"temp " + newTemp + 
					" " + threshold + " " + userLocations.get(name) + " " +
					generateSuggestion(name, "temp") + " " + weather);
			// Greater than threshold AND there has been a change
			if (!isAboveThreshold && newTemp >= threshold && newTemp != userTemps.get(name)) {
				userComms.get(name).getKey().generateWarning(
						"temp", newTemp, threshold, userLocations.get(name), 
						generateSuggestion(name, "temp"), weather);
				isAboveThreshold = true;
			}
			if (newTemp < threshold) {
				isAboveThreshold = false;
			}
			userTemps.put(name, newTemp);
		}
    }

    public static class APManagerI implements APManager {
    	Map<String, Boolean> isAlreadyIn;
    	
    	public APManagerI() {
    		super();
    		isAlreadyIn = new HashMap<>();
    	}
		@Override
		public void processAQI(String name, String aqi, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received \"" + name + " " + aqi + "\"");

			Pair<Integer, Integer> apoAndDuration = userApos.get(name);
			int aqiInt = Integer.parseInt(aqi);
			
			if (userLocations.get(name) == null) {
				return;
			}

			System.out.println(dtf.format(now) + " Sent LocationServer \"" + 
					getLocManager().getKey().getInOrOut(userLocations.get(name) + "\""));

			if (apoAndDuration == null || apoAndDuration.getKey() != aqiInt
					|| getLocManager().getKey().getInOrOut(userLocations.get(name)).equalsIgnoreCase("indoor")) {
				userApos.put(name, new Pair<>(aqiInt, 0));
				isAlreadyIn.put(name, false);
				return;
			} else {
				userApos.put(name, new Pair<>(aqiInt, apoAndDuration.getValue() + 1));
			}
			apoAndDuration = userApos.get(name);

			int baseTime;
			if (aqiInt >= 0 && aqiInt <= 50) {
				baseTime = 30;
			} else if (aqiInt >= 51 && aqiInt <= 100) {
				baseTime = 15;
			} else if (aqiInt >= 101 && aqiInt <= 150) {
				baseTime = 10;
			} else if (aqiInt >= 151 && aqiInt <= 200) {
				baseTime = 5;
			} else {
				System.err.println("AQI out of range");
				return;
			}

			String medicalCondition = getPrefRepo().getKey().processPreferenceRequest(name, "med");
			int threshold = Integer.parseInt(medicalCondition) * baseTime;
			
			if (apoAndDuration.getValue() >= threshold) {
				if (isAlreadyIn.get(name) == true) {
					return;
				}

				System.out.println(dtf.format(now) + " Sent EnviroAppUi \"aqi " + aqi + 
						" " + threshold + " " + userLocations.get(name) +  
						generateSuggestion(name, "aqi") + weather + "\"");

				userComms.get(name).getKey().generateWarning("aqi", aqiInt,
						threshold, userLocations.get(name), generateSuggestion(name, "aqi"), weather);
				isAlreadyIn.put(name, true);
			} else {
				isAlreadyIn.put(name, false);
			}
		}
    }

    public static class LocationManagerI implements LocationManager {
		@Override
		public void processLocation(String name, String loc, String indoorOutdoor, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received \"" + name + " " + loc +
					" " + indoorOutdoor + "\"");

			String previousLoc = userLocations.get(name);
			if (previousLoc == null) {
				return;
			}
			userLocations.put(name, loc);
		}
    }

    public static class AlarmManagerI implements AlarmManager {

		@Override
		public void processAlarmMessage(String incomingWeather, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received \"" + incomingWeather + "\"");
			weather = Integer.parseInt(incomingWeather);
			for (String name: userLocations.keySet()) {
				userComms.get(name).getKey().generateWarning("alarm", weather, -1, userLocations.get(name), generateSuggestion(name, "alarm"), weather);
			}
		}
    }
    
	public static String generateSuggestion(String name, String type) {
		String service = getPrefRepo().getKey().getSuggestion(name + ", " + type);
		String returnStr = "";
		String orOutdoor = "";
		if (weather == 0 && !type.equalsIgnoreCase("aqi") && !type.equalsIgnoreCase("alarm")) {
			orOutdoor = " or outdoor";
		}
		returnStr = "Suggestion - please go to an indoor" + orOutdoor + " cinema at one of these locations: ";
		for (String key: locs.keySet()) {
			for (Location loc: locs.get(key)) {
				if (weather != 0 && 
						getLocManager().getKey().getInOrOut(
						loc.getLocCode()).equalsIgnoreCase("outdoor")) {
					continue;
				}
				if (loc.getServices().contains(" " + service)) {
					returnStr = returnStr.concat(loc.getName() + ", ");
				}
			}
		}
		return returnStr.substring(0, returnStr.length() -2).trim();
	}

    public static Pair<PreLocationManagerPrx, Communicator> getLocManager() {
    	Communicator communicator = com.zeroc.Ice.Util.initialize();
  
        
        com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("LocationMiddleman:default -p 10023");
        PreLocationManagerPrx locManager = PreLocationManagerPrx.checkedCast(base);
        
        if(locManager == null) {
            throw new Error("Invalid proxy when creating PreLocationManager in ContextManager");
        }
		return new Pair<>(locManager, communicator);
    }
    
    public static Pair<WarningGeneratorPrx, Communicator> getWarningGen() {
    	Communicator communicator = com.zeroc.Ice.Util.initialize();
  
    	int port = 10066;
        
        com.zeroc.Ice.ObjectPrx base;
    	while (true) {
    		try {
    			base = communicator.stringToProxy("WarningGen:default -p " + Integer.toString(port));
    		} catch (com.zeroc.Ice.SocketException e) {
        		port++;
        		continue;
        	}
    		break;
    	}
        WarningGeneratorPrx manager = WarningGeneratorPrx.checkedCast(base);
        
        if(manager == null) {
            throw new Error("Invalid proxy when creating WarningGenerator in ContextManager");
        }
		return new Pair<>(manager, communicator);
    }
    
    public static Pair<PreferenceManagerPrx, Communicator> getPrefRepo() {
    	Communicator communicator = com.zeroc.Ice.Util.initialize();
  
        
        com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("preferenceRepo:default -p 10034");
        PreferenceManagerPrx locManager = PreferenceManagerPrx.checkedCast(base);
        
        if(locManager == null)
        {
            throw new Error("Invalid proxy when creating PreferenceManager in ContextManager");
        }
		return new Pair<>(locManager, communicator);
    }

    private final static String PROXY = "TopicManager.Proxy";
    private static TopicPrx tempProxy;
    private static TopicPrx locProxy;
    private static TopicPrx aqiProxy;
    private static TopicPrx weatherProxy;
    private static Map<String, Integer> tempThresholds;
    private static Map<String, Integer> userTemps; // Stores the user's temperature
    private static Map<String, Pair<Integer, Integer>> userApos; // Map user to APO and duration
    private static Map<String, String> userLocations; // Store users locs at any given time
    private static int weather; // Current weather condition
	private static Map<String, List<Location>> locs; // Stores the information about all locations
	private static Map<String, Pair<WarningGeneratorPrx, Communicator>> userComms;
	private static AtomicBoolean shutdown;
	
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ContextManager.java [filename]");
            System.exit(1);
        }
        shutdown = new AtomicBoolean(false);
        tempThresholds = new HashMap<>();
        userApos = new HashMap<>();
        userLocations = new HashMap<>();
        userTemps = new HashMap<>();
		userComms = new HashMap<>();
		locs = readCmFile(args[0] + ".txt");

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
            status = run(communicator, destroyHook, args);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            status = 1;
        }
        System.exit(status);
    }

	public static int run(Communicator communicator, Thread destroyHook, String[] args) {
    	Communicator rmiCommunicator = com.zeroc.Ice.Util.initialize();
    	
        com.zeroc.Ice.ObjectAdapter rmiAdapter = 
        		rmiCommunicator.createObjectAdapterWithEndpoints("locationInfo", "default -p 10055");
        com.zeroc.Ice.Object objectJ = new UiInteractorI();

        rmiAdapter.add(objectJ, com.zeroc.Ice.Util.stringToIdentity("infoProvider"));
        rmiAdapter.activate();

		com.zeroc.IceStorm.TopicManagerPrx manager = 
				com.zeroc.IceStorm.TopicManagerPrx.checkedCast(communicator.propertyToProxy(PROXY));
        if(manager == null) {
            System.err.println("invalid proxy");
            return 1;
        }
        
        com.zeroc.Ice.ObjectAdapter adapter = 
        		communicator.createObjectAdapter("EnviroSmart.APManager");
        tempProxy = SubscriberUtil.getTopic("tempSensor", communicator, manager);
        locProxy = SubscriberUtil.getTopic("locSensor", communicator, manager);
        aqiProxy = SubscriberUtil.getTopic("apSensor", communicator, manager);
        weatherProxy = SubscriberUtil.getTopic("weatherSensor", communicator, manager);

        ObjectPrx tempSub = SubscriberUtil.getSubscriber(tempProxy, new TempManagerI(), communicator, adapter);
        ObjectPrx locSub = SubscriberUtil.getSubscriber(locProxy, new LocationManagerI(), communicator, adapter);
        ObjectPrx aqiSub = SubscriberUtil.getSubscriber(aqiProxy, new APManagerI(), communicator, adapter);
        ObjectPrx weatherSub = SubscriberUtil.getSubscriber(weatherProxy, new AlarmManagerI(), communicator, adapter);

        adapter.activate();
        tempSub = tempSub.ice_oneway();
        locSub = locSub.ice_twoway();
        aqiSub = aqiSub.ice_oneway();
        weatherSub = weatherSub.ice_oneway();
       
        //
        // Replace the shutdown hook to unsubscribe during JVM shutdown
        //
        final com.zeroc.IceStorm.TopicPrx topicTemp = tempProxy;
        final com.zeroc.Ice.ObjectPrx subscriberTemp = tempSub;
        final com.zeroc.IceStorm.TopicPrx topicLoc = locProxy;
        final com.zeroc.Ice.ObjectPrx subscriberLoc = locSub;
        final com.zeroc.IceStorm.TopicPrx topicAqi = aqiProxy;
        final com.zeroc.Ice.ObjectPrx subscriberAqi = aqiSub;
        final com.zeroc.IceStorm.TopicPrx topicWeather = weatherProxy;
        final com.zeroc.Ice.ObjectPrx subscriberWeather = weatherSub;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // topicTemp.unsubscribe(subscriberTemp);
                topicLoc.unsubscribe(subscriberLoc);
                topicAqi.unsubscribe(subscriberAqi);
                topicTemp.unsubscribe(subscriberTemp);
                topicWeather.unsubscribe(subscriberWeather);
            } finally {
                communicator.destroy();
            }
        }));
        Runtime.getRuntime().removeShutdownHook(destroyHook); // remove old destroy-only shutdown hook
        
        while (!shutdown.get()) {
        	try {
        		Thread.sleep(1000);
        	} catch (InterruptedException e) {
        		//
        	}
        }
        System.exit(0);
		return 0;
	}
	
	/**
	 * Reads the file given to main as argument,
	 * which has all locations and information about them.
	 */
	private static Map<String, List<Location>> readCmFile(String filename) {
		Map<String, List<Location>> newMap = new HashMap<>();
		
		// Read all lines into LinkedList
		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("Error: Cannot access " + filename + ". Check the file name, and that the file exists. Program exiting");
				System.exit(1);
			}
			String line;
			while (true) {
				Location newLoc = new Location(null, null, new LinkedList<>(), new LinkedList<>());
				line = reader.readLine();
				if (line == null) {
					break;
				} else if (line.length() == 0) {
					continue;
				}
				String[] lineSplit = line.split(":");
				if (lineSplit.length != 2) {
					cmFileExit();
				}
				if (!lineSplit[0].equalsIgnoreCase("name")) {
					cmFileExit();
				}
				newLoc.setName(lineSplit[1].trim());
				for (int i = 0; i < 2; i++) {
					line = reader.readLine();
					if (line == null) {
						cmFileExit();
					}
					lineSplit = line.split(":");
					if (lineSplit.length != 2) {
						cmFileExit();
					}
					if (i == 0) {
						if (!lineSplit[0].equalsIgnoreCase("location")) {
							cmFileExit();
						}
						newLoc.setLocCode(lineSplit[1].replaceAll(" ", ""));
					} else if (i == 1) {
						if (!lineSplit[0].equalsIgnoreCase("information")) {
							cmFileExit();
						}
						newLoc.addToInfo(lineSplit[1].trim());
						while (true) {
							line = reader.readLine();
							if (line == null) {
								cmFileExit();
							}
							lineSplit = line.split(":");
							if (lineSplit.length == 1) {
								newLoc.addToInfo(line.trim());
							} else if (lineSplit[0].equalsIgnoreCase("services")) {
								LinkedList<String> serv = new LinkedList<String>(Arrays.asList(lineSplit[1].split(",")));
								newLoc.setServices(serv);
								break;
							}
							
						}
					}
				}
				if (newMap.get(newLoc.getLocCode()) == null) {
					newMap.put(newLoc.getLocCode(), new LinkedList<Location>(Arrays.asList(newLoc)));
				} else {
					newMap.get(newLoc.getLocCode()).add(newLoc);
				}
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("ioexception");
			e.printStackTrace();
		}
			
		return newMap;
	}

	private static void cmFileExit() {
		System.err.println("Incorrect cm file format");
		System.exit(1);
	}
}
