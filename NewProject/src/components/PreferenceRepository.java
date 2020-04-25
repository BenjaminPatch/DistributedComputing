package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.TopicPrx;

import EnviroSmart.PreferenceManager;
import javafx.util.Pair;

public class PreferenceRepository {
	public static class PreferenceManagerI implements PreferenceManager {
		private Map<String, Preference> preferences; // Maps a name to Preference type
		private String filename;
		private Communicator communicator;
		private static final String MEDICAL_CONDITION = "med";
		private static final String PREF1 = "pref1";
		private static final String PREF2 = "pref2";
		private static final String PREF3 = "pref3";
		
		public PreferenceManagerI(String filename, Communicator communicator) {
			super();
			this.filename = filename;
			this.preferences = readPreferenceFile(filename);
			System.out.println(this.preferences.get("bryan").getPref3());
		}
		
		@Override
		public String processPreferenceRequest(String name, String req, Current current) {
			System.out.println("Request received at pref repository: " + req);
			Preference relevantPref = preferences.get(name);
			if (relevantPref == null) {
				return null;
			}
			switch (req) {
			case MEDICAL_CONDITION:
				return relevantPref.getMedicalCondition();
			case PREF1:
				return (relevantPref.getPref1().getKey().toString() + ":" + relevantPref.getPref1().getValue().toString());
			case PREF2:
				return relevantPref.getPref2();
			case PREF3:
				return relevantPref.getPref3();
			default:
				return null;
			}
		}
		
		private Map<String, Preference> readPreferenceFile(String filename) {
			Map<String, Preference> newMap = new HashMap<>();
			
			// Read all lines into LinkedList
			try {
				BufferedReader reader = new BufferedReader(new FileReader(filename));
				String line;
				while (true) {
					Preference newPref = new Preference(null, null, null, null, null);
					line = reader.readLine();
					if (line == null) {
						break;
					} else if (line.length() == 0) {
						continue;
					}
					String[] lineSplit = line.split(":");
					if (lineSplit.length != 2) {
						prefFileExit();
					}
					if (!lineSplit[0].equalsIgnoreCase("name")) {
						prefFileExit();
					}
					newPref.setName(lineSplit[1].replaceAll(" ", ""));
					for (int i = 0; i < 4; i++) {
						line = reader.readLine();
						if (line == null) {
							prefFileExit();
						}
						lineSplit = line.split(":");
						if (lineSplit.length != 2) {
							prefFileExit();
						}
						if (i == 0) {
							if (!lineSplit[0].equalsIgnoreCase("Medical Condition Type")) {
								prefFileExit();
							}
							int val = -1;
							try {
								val = Integer.parseInt(lineSplit[1].replaceAll(" ", ""));
							} catch (NumberFormatException e) {
								prefFileExit();
							}
							if (val < 1 || val > 3) {
								prefFileExit();
							}
							newPref.setMedicalCondition(lineSplit[1].replaceAll(" ", ""));
						} else if (i == 1) {
							if (!lineSplit[0].equals("pref-1")) {
								prefFileExit();
							}
							String[] pref1Split = lineSplit[1].split("//")[0].split(" ");
							if (pref1Split.length != 5) {
								prefFileExit();
							}
							try {
								newPref.setPref1(new Pair<Integer, String>(
										Integer.parseInt(pref1Split[2]), pref1Split[4]));
							} catch (NumberFormatException e) {
								prefFileExit();
							}
						} else if (i == 2) {
							if (!lineSplit[0].equals("pref-2")) {
								prefFileExit();
							}
							String[] pref1Split = lineSplit[1].split("//")[0].split(" ");
							if (pref1Split.length != 5) {
								prefFileExit();
							}
							try {
								newPref.setPref2(pref1Split[4]);
							} catch (NumberFormatException e) {
								prefFileExit();
							}
						} else if (i == 3) {
							if (!lineSplit[0].equals("pref-3")) {
								prefFileExit();
							}
							String[] pref1Split = lineSplit[1].split("//")[0].split(" ");
							if (pref1Split.length != 5) {
								prefFileExit();
							}
							try {
								newPref.setPref3(pref1Split[4]);
							} catch (NumberFormatException e) {
								prefFileExit();
							}
						}
					}
					newMap.put(newPref.getName(), newPref);
				}
				reader.close();
			} catch (IOException e) {
				System.err.println("ioexception");
				e.printStackTrace();
			}
				
			return newMap;
		}
		
		private void prefFileExit() {
			System.err.println("Incorrect preference file format");
			System.exit(1);
		}

		@Override
		public void shutdown(Current current) {
			communicator.shutdown();
			System.exit(0);
		}
		
	}

	private Communicator communicator;
    private static TopicPrx incomingProxy;
    private final static String PROXY = "TopicManager.Proxy";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: PreferenceRepository.java [filename]");
            System.exit(1);
        }
    	Communicator communicator = com.zeroc.Ice.Util.initialize(args);
    	
    	System.out.println("Server starting.");
        
        com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("PreferenceRepository", "default -p 10034");
        com.zeroc.Ice.Object object = new PreferenceManagerI(args[0] + ".txt", communicator);

        adapter.add(object, com.zeroc.Ice.Util.stringToIdentity("preferenceRepo"));
        adapter.activate();
        
        System.out.println("Adapter activated. Waiting for data.");
        communicator.waitForShutdown();
    }
}
