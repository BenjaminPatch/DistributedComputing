package components;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;

import EnviroSmart.PreferenceManager;
import javafx.util.Pair;

public class PreferenceRepository {
	public static class PreferenceManagerI implements PreferenceManager {
		private Map<String, Preference> preferences; // Maps a name to Preference type
		private Communicator communicator;
		private static final String MEDICAL_CONDITION = "med";
		private static final String TEMP_PREF = "pref1";
		private static final String APO_PREF = "pref2";
		private static final String WEATHER_PREF = "pref3";
		
		public PreferenceManagerI(String filename, Communicator communicator) {
			super();
			this.preferences = readPreferenceFile(filename);
		}

		@Override
		public String getSuggestion(String nameAndEvent, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received ContextManager\"" + nameAndEvent + "\"");

			String[] nameEvent = nameAndEvent.split(",");
			Preference prefs = preferences.get(nameEvent[0].trim());
			switch (nameEvent[1].trim().toLowerCase()) {
			case "alarm":
				return prefs.getMedicalCondition();
			case "aqi":
				return prefs.getPref2();
			case "temp":
				return prefs.getPref1().getValue();
			default:
				System.err.println("Error in suggestion request");
			}
			return null;
		}
		
		@Override
		public String processPreferenceRequest(String name, String req, Current current) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Received ContextManager\"" + name + " " + req + "\"");

			Preference relevantPref = preferences.get(name);
			if (relevantPref == null) {
				return null;
			}
			switch (req) {
			case MEDICAL_CONDITION:
				return relevantPref.getMedicalCondition();
			case TEMP_PREF:
				return (relevantPref.getPref1().getKey().toString() + ":" + relevantPref.getPref1().getValue().toString());
			case APO_PREF:
				return relevantPref.getPref2();
			case WEATHER_PREF:
				return relevantPref.getPref3();
			default:
				return null;
			}
		}
		
		private Map<String, Preference> readPreferenceFile(String filename) {
			Map<String, Preference> newMap = new HashMap<>();
			
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

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: PreferenceRepository.java [filename]");
            System.exit(1);
        }
    	Communicator communicator = com.zeroc.Ice.Util.initialize(args);
    	
        com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("PreferenceRepository", "default -p 10034");
        com.zeroc.Ice.Object object = new PreferenceManagerI(args[0] + ".txt", communicator);

        adapter.add(object, com.zeroc.Ice.Util.stringToIdentity("preferenceRepo"));
        adapter.activate();
        
        communicator.waitForShutdown();
    }
}
