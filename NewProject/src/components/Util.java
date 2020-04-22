package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.zeroc.Ice.Communicator;

public final class Util {
	static List<String> generateLines(String filename) {
		System.out.println("lineoss");
		List<String> allLines = new LinkedList<>();
		// Read all lines into LinkedList
		try {
			System.out.println("lineossa");
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
			while (line != null) {
				System.out.println("lineossb");
				allLines.add(line);
				line = reader.readLine();
			}
			System.out.println("lineossc");
			reader.close();
		} catch (IOException e) {
			System.out.println("ioexception");
			e.printStackTrace();
		}

		System.out.println("done line");
		return allLines;
	}
	
	static void iterateThroughLines(List<String> allLines, Consumer<String> function, Communicator communicator) {
		System.out.println("Iterating");
		for (String line:allLines) {
			String[] splitLine = line.split(",");
			if (splitLine.length != 2) {
				// TODO: Discussion board question: handle
				// https://learn.uq.edu.au/webapps/discussionboard/do/message?action=list_messages&course_id=_128088_1&nav=discussion_board_entry&conf_id=_378320_1&forum_id=_441002_1&message_id=_1166089_1
			}
			for (int i = 0; i < Integer.parseInt(splitLine[1]); i++) {
				if (communicator.isShutdown()) {
					break;
				}
				try {
					System.out.println("sending");
					function.accept(splitLine[0]);
					Thread.sleep(1000);
				} catch (NumberFormatException e) {
					System.err.println("data file not of correct format");
					communicator.shutdown();
				} catch (InterruptedException e) {
					System.out.println("interrupted");
					communicator.shutdown();
				}
			}
		}
	}
}
