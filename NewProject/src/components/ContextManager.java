package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.TopicPrx;

import EnviroSmart.APManager;
import EnviroSmart.AlarmManager;
import EnviroSmart.LocationManager;
import EnviroSmart.PreLocationManagerPrx;
import EnviroSmart.PreferenceManagerPrx;
import EnviroSmart.TemperatureManager;
import EnviroSmart.UiInteractor;
import javafx.util.Pair;

public class ContextManager {
	
	public static class UiInteractorI implements UiInteractor {
		private static Map<String, List<Location>> locs; // Stores the information about all locations
		public UiInteractorI(String filename) {
			locs = readCmFile(filename);
		}

		@Override
		public EnviroSmart.Location getInfoGivenLoc(String loc, Current current) {
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
			String loc = userLocations.get(name);
			if (loc == "") {
				return null;
			}
			String returnMessage = "";
			List<Location> locNames = locs.get(loc);
			for (Location cur : locNames) {
				System.out.println(returnMessage);
				returnMessage = returnMessage.concat(cur.getName() + ", ");
			}
			return returnMessage.substring(0, returnMessage.length() -2).trim();
		}

		@Override
		public void logIn(String name, Current current) {
			userLocations.putIfAbsent(name, "");
		}
	}

    public static class TempManagerI implements TemperatureManager {
    	
		@Override
		public void processTemperature(String name, String temp, Current current) {
			System.out.println("TEMP: " + temp);
		}
    }

    public static class APManagerI implements APManager {
		@Override
		public void processAQI(String name, String aqi, Current current) {
			System.out.println("AQI: " + aqi);
		}
    }

    public static class LocationManagerI implements LocationManager {
		@Override
		public void processLocation(String name, String loc, Current current) {
			System.out.println("LOC: " + loc + " " + name);
			String previousLoc = userLocations.get(name);
			if (previousLoc == null) {
				return;
			}
			userLocations.put(name, loc);
		}
    }

    public static class AlarmManagerI implements AlarmManager {

		@Override
		public void processAlarmMessage(String weather, Current current) {
			System.out.println("weather status:" + weather);
			apoThresholds.put("HI", "There");
		}
    }
    
    public static Pair<PreLocationManagerPrx, Communicator> getLocManager() {
    	Communicator communicator = com.zeroc.Ice.Util.initialize();
  
        
        com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("LocationMiddleman:default -p 10023");
        PreLocationManagerPrx locManager = PreLocationManagerPrx.checkedCast(base);
        
        if(locManager == null)
        {
            throw new Error("Invalid proxy when creating PreLocationManager in ContextManager");
        }
		return new Pair<>(locManager, communicator);
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
    private static Map<String, String> tempThresholds;
    private static Map<String, String> apoThresholds;
    private static Map<String, String> userLocations; // Store users locs at any given time
	
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ContextManager.java [filename]");
            System.exit(1);
        }
        tempThresholds = new HashMap<>();
        apoThresholds = new HashMap<>();
        userLocations = new HashMap<>();

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
    }

	public static int run(Communicator communicator, Thread destroyHook, String[] args) {
    	Communicator rmiCommunicator = com.zeroc.Ice.Util.initialize();
    	
        com.zeroc.Ice.ObjectAdapter rmiAdapter = 
        		rmiCommunicator.createObjectAdapterWithEndpoints("locationInfo", "default -p 10055");
        com.zeroc.Ice.Object objectJ = new UiInteractorI(args[0] + ".txt");

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
        rmiCommunicator.waitForShutdown();
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
				BufferedReader reader = new BufferedReader(new FileReader(filename));
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
