package components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class EnviroAppUI {
	private String name;

    public static void main(String[] args) throws IOException {
    	System.out.println("Context-aware Enviro Smart Application");
    	System.out.println("Please enter your user name:");
        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));
        String name = reader.readLine();
        
        EnviroAppUI mainClient = new EnviroAppUI(name);
        mainClient.getOption();

    }
    
    public EnviroAppUI(String name) {
    	this.name = name;
    	// TODO check if name exists
    }
    
    private void getOption() {
     	System.out.println(name);
     	System.out.println("Context-aware Enviro Smart Application Main Menu");
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
        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));

        String line;
        try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
        // TODO query context manager
    }
    
    private void itemAtLoc() {
        BufferedReader reader =
        		new BufferedReader(new InputStreamReader(System.in));

        String line;
        try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
        // TODO query context manager
    }
}
