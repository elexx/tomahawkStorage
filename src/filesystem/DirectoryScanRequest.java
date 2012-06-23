package filesystem;

import java.nio.file.Path;

import database.TomahawkDBInterface;

class DirectoryScanRequest {
	public final ScanRequestType type;
	public final Path path;
	public final TomahawkDBInterface database;

	public DirectoryScanRequest(ScanRequestType type, Path path, TomahawkDBInterface database) {
		this.type = type;
		this.path = path;
		this.database = database;
	}
}
