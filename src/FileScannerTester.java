import java.nio.file.FileSystems;

import database.TomahawkDB;
import database.TomahawkDBInterface;
import filesystem.FileScanner;

public class FileScannerTester {

	public static void main(String[] args) {
		FileScanner fs = new FileScanner();
		Thread fst = new Thread(fs);
		fst.start();

		TomahawkDBInterface db = new TomahawkDB();
		db.connect("tomahawk.storage.jpa");

		fs.processDirectory(FileSystems.getDefault().getPath("/Users/alexander/Downloads/Trifling_Wings_-_a46901_---_Jamendo_-_MP3_VBR_192k"), db);

		fs.stopScanner();
		try {
			fst.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		db.close();
	}
}
