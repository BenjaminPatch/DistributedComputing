package components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;

import EnviroSmart.UiInteractorPrx;
import EnviroSmart.WarningGenerator;
import javafx.scene.shape.Line;

// TODO: When user exits, send logout message

public class EnviroAppUI {
	
	public class WarningGeneratorI implements WarningGenerator {

		@Override
		public void generateWarning(String type, int value, int threshold, String currentLocation, String suggestion,
				int weatherAlarm, Current current) {
			if (type.equalsIgnoreCase("temp")) {
				printTempWarning(value, threshold);
			} else if (type.equalsIgnoreCase("aqi")) {
				printAqiWarning(value, threshold);
			} else if (type.equalsIgnoreCase("alarm")) {
				printAlarm(value);
			}

			System.out.println("Current location: " + currentLocation);
			
			if (type.equalsIgnoreCase("temp")) {
				System.out.printf("Current weather alarm status: ");
				if (weatherAlarm != 0) {
					System.out.println("alarm");
				} else {
					System.out.println("no alarm");
				}
			}

			System.out.println(suggestion);
			System.out.println();
		}
		
		private void printTempWarning(int value, int threshold) {
			System.out.println("Warning, TEMPERATURE is now " + value + " (" 
					+ name + "'s limit is " + threshold + ")");
		}

		private void printAlarm(int value) {
			String type;
			if (value == 1) {
				type = "HEAVY RAIN";
			} else if (value == 2) {
				type = "HAIL STORM";
			} else if (value == 3) {
				type = "STRONG WIND";
			}
			System.out.println("Warning, EXTREME WEATHER detected, "
					+ "the current weather event is " + value);
		}

		private void printAqiWarning(int value, int threshold) {
			System.out.println("Warning, SIGNIFICANT AIR POLUTION detected, "
					+ "the current AQI is " + value + " (" + name + "'s limit is " + threshold + ")");
		}
	}
	
	private String name;
	UiInteractorPrx provider;
	Communicator communicator;
	Communicator incCommunicator;
    
    public EnviroAppUI(String name) {
        
        this.incCommunicator = com.zeroc.Ice.Util.initialize();
        
        int currentPort = 10066;
    	
        com.zeroc.Ice.ObjectAdapter adapter;
        while (true) {
        	try {
        	adapter = this.incCommunicator.createObjectAdapterWithEndpoints("WarningGenerator", "default -p " + Integer.toString(currentPort));
        	} catch (com.zeroc.Ice.SocketException e) {
        		currentPort++;
        		continue;
        	}
        	break;
        }
        System.out.println("currentport: " + currentPort);

        com.zeroc.Ice.Object object = new WarningGeneratorI();

        adapter.add(object, com.zeroc.Ice.Util.stringToIdentity("WarningGen"));
        adapter.activate();

    	communicator = com.zeroc.Ice.Util.initialize();
        com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("infoProvider:default -p 10055");
        provider = UiInteractorPrx.checkedCast(base);
        
        if(provider == null)
        {
            throw new Error("Invalid proxy");
        }
        if (!provider.logIn(name)) {
        	System.out.println("Error: The provided name was not found. " +
        			"Please check the name, restart the user interface, " +
        			"and enter the name again.");
        	System.exit(1);
        }
        
        System.out.println("Adapter activated. Waiting for data.");

    	this.name = name;
    }

    public static void main(String[] args) throws IOException {
    	System.out.println("Context-aware Enviro Smart Application");
    	System.out.println("Please enter your user name:");
        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));
        String name = reader.readLine();

        EnviroAppUI mainClient = new EnviroAppUI(name.toLowerCase());

     	System.out.println("Context-aware Enviro Smart Application Main Menu");
     	mainClient.doLoop();
     	// TODO DATE TIME
    }
    
    private void getOption() {
     	System.out.println("--Please select an option--:");
     	System.out.println("1. Search for information on a specific item of interest");
     	System.out.println("2. Search for items of interest in current location");
     	System.out.println("E. Exit");

        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));

        String line;
        try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

        switch (line.toLowerCase()) {
        case "1":
        	infoOnItem();
        	break;
        case "2":	
        	itemAtLoc();
        	break;
        case "e":
        	System.out.println("CLOSING, May take up to 60 seconds");
        	// TODO exit procedure
        	System.exit(0);
        	break;
        default:
        	System.out.println("Invalid input");
        	// TODO what to do here
        }
    }
    
    private void infoOnItem() {
    	System.out.println("Please enter name of item of interest:");
        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

        EnviroSmart.Location response = provider.getInfoGivenLoc(line);

        if (response == null) {
        	System.out.println("No match found for item of interest");
        	return;
        }

        System.out.println("Information about " + line + ":");
        for (String infoLine:response.info) {
        	System.out.println(infoLine);
        }
       	System.out.println();
    }
    
    private void itemAtLoc() {
        String response = provider.getInfoCurrentLoc(this.name);
        System.out.println("The following items of interest are in your location:");
        if (response.length() == 0) {
        	System.out.println("There are no items of interest in your current location.");
        }
        for (String line:response.split(",")) {
        	System.out.println(line.trim());
        }
        System.out.println();
    }
    
    private void doLoop() {
    	while (true) {
    		getOption();
    	}
    }
}
