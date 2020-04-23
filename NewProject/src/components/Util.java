package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import com.zeroc.Ice.Communicator;

public final class Util {
	static List<String> generateLines(String filename) {
		List<String> allLines = new LinkedList<>();
		// Read all lines into LinkedList
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
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
