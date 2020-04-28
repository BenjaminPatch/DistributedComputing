package components;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public final class Util {
	static List<String> generateLines(String filename) {
		List<String> allLines = new LinkedList<>();
		// Read all lines into LinkedList
		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("Error: Cannot access " + filename + ". Check the file name, and that the file exists. Program exiting");
				System.exit(1);
			}
			String line = reader.readLine();
			while (line != null) {
				allLines.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("ioexception");
			e.printStackTrace();
		}

		return allLines;
	}
}
