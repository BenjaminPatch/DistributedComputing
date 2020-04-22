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
			e.printStackTrace();
		}

		return allLines;
	}
	
	static void iterateThroughLines(List<String> allLines, Consumer<String> function, Communicator communicator) {
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
					function.accept(splitLine[0]);
					Thread.sleep(1000);
				} catch (NumberFormatException e) {
					System.err.println("data file not of correct format");
					communicator.shutdown();
				} catch (InterruptedException e) {
					communicator.shutdown();
				}
			}
		}
	}
}
