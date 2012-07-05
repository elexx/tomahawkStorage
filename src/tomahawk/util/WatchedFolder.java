package tomahawk.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchedFolder implements Iterable<Path> {

	private static final Logger LOG = LoggerFactory.getLogger(WatchedFolder.class);

	private Set<Path> directories = new HashSet<>();

	public WatchedFolder(String filename) {
		File file = new File(filename);
		if (file.exists()) {
			try {
				Scanner fileScanner = new Scanner(file);
				while (fileScanner.hasNextLine()) {
					directories.add(FileSystems.getDefault().getPath(fileScanner.nextLine()));
				}
			} catch (FileNotFoundException e) {
				LOG.warn("FileNotFound!", e);
			}
		}
	}

	@Override
	public Iterator<Path> iterator() {
		return directories.iterator();
	}

}
