package components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.zeroc.Ice.Communicator;

import EnviroSmart.UiInteractorPrx;

public class EnviroAppUI {
	private String name;
	UiInteractorPrx provider;
	Communicator communicator;
    
    public EnviroAppUI(String name) {
    	communicator = com.zeroc.Ice.Util.initialize();
        com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("infoProvider:default -p 10055");
        provider = UiInteractorPrx.checkedCast(base);
        
        if(provider == null)
        {
            throw new Error("Invalid proxy");
        }
        provider.logIn(name);

    	this.name = name;
    	// TODO check if name exists
    }

    public static void main(String[] args) throws IOException {
    	System.out.println("Context-aware Enviro Smart Application");
    	System.out.println("Please enter your user name:");
        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));
        String name = reader.readLine();
        
        EnviroAppUI mainClient = new EnviroAppUI(name);
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
        	System.out.println("Could not find this location");
        	return; //TODO loop
        }

        System.out.println("Information about " + line + ":");
        for (String infoLine:response.info) {
        	System.out.println(infoLine);
        }
    }
    
    private void itemAtLoc() {
        String response = provider.getInfoCurrentLoc(this.name);
        System.out.println("The following items of interest are in your location:");
        System.out.println(response);
        // TODO query context manager
    }
    
    private void doLoop() {
    	while (true) {
    		getOption();
    	}
    }
}
