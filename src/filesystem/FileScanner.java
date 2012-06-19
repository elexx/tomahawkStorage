package filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import database.TomahawkDBInterface;
import database.model.Album;
import database.model.Artist;
import database.model.Track;

public class FileScanner implements Runnable {

	private final Queue<DirectoryScanRequest> queue = new ConcurrentLinkedQueue<>();

	public void processDirectory(final Path path, final TomahawkDBInterface database) {
		synchronized (queue) {
			queue.add(new DirectoryScanRequest(ScanRequestType.SCAN_PATH, path, database));
			queue.notify();
		}
	}

	public void stopScanner() {
		synchronized (queue) {
			queue.add(new DirectoryScanRequest(ScanRequestType.STOP_SCANNER, null, null));
			queue.notify();
		}
	}

	@Override
	public void run() {
		DirectoryScanRequest scanRequest;

		while (true) {
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {}
				}
				scanRequest = queue.remove();
			}
			if (ScanRequestType.STOP_SCANNER == scanRequest.type) {
				return;
			}

			FileScannerWorker<Path> fileWorker = new FileScannerWorker<>();

			try {
				System.out.println(scanRequest.path.toAbsolutePath());
				Files.walkFileTree(scanRequest.path.toAbsolutePath(), fileWorker);
			} catch (IOException e) {
				e.printStackTrace();
			}

			List<Path> fileList = fileWorker.getList();
			Map<String, Artist> artists = new HashMap<>();
			Map<String, Album> albums = new HashMap<>();
			List<Track> trackList = new ArrayList<>();

			for (Path file : fileList) {
				try {
					BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
					GregorianCalendar lastModification = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
					lastModification.setTimeInMillis(fileAttributes.lastModifiedTime().toMillis());

					AudioFile audioFile = AudioFileIO.read(file.toFile());
					MP3File mp3File = (MP3File) audioFile;

					Track track = new Track();
					track.bitrate = mp3File.getMP3AudioHeader().getBitRateAsNumber();
					track.createTimestamp = lastModification;
					track.duration = mp3File.getAudioHeader().getTrackLength();
					track.mimetype = "audio/mpeg";
					track.path = file.toAbsolutePath().toString();
					track.releaseyear = parseIntOrZero(mp3File.getID3v2TagAsv24().getFirst(FieldKey.YEAR));
					track.size = fileAttributes.size();
					track.title = mp3File.getID3v2TagAsv24().getFirst(FieldKey.TITLE);
					track.tracknumber = parseIntOrZero(mp3File.getID3v2TagAsv24().getFirst(FieldKey.TRACK));

					String artistName = mp3File.getID3v2TagAsv24().getFirst(FieldKey.ARTIST);
					Artist artist;
					if (!artists.containsKey(artistName)) {
						artist = scanRequest.database.getArtistByName(artistName);
						artist.name = artistName;
						artists.put(artistName, artist);
					} else {
						artist = artists.get(artistName);
					}
					track.artist = artist;

					String albumName = mp3File.getID3v2TagAsv24().getFirst(FieldKey.ALBUM);
					Album album;
					if (!albums.containsKey(albumName)) {
						album = scanRequest.database.getAlbumByName(albumName);
						album.name = albumName;

						if (null == album.artist) {
							String albumArtistName = mp3File.getID3v2TagAsv24().getFirst(FieldKey.ALBUM_ARTIST);
							if (albumArtistName.length() == 0) {
								album.artist = artist;
							} else {
								Artist albumArtist;
								if (!artists.containsKey(albumArtistName)) {
									albumArtist = scanRequest.database.getArtistByName(albumArtistName);
									albumArtist.name = artistName;
									artists.put(albumArtistName, albumArtist);
								} else {
									albumArtist = artists.get(albumArtistName);
								}
								album.artist = albumArtist;
							}
						}
						albums.put(albumName, album);
					} else {
						album = albums.get(albumName);
					}
					track.album = album;

					trackList.add(track);

				} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
					e.printStackTrace();
				}
			}

			scanRequest.database.newFiles(trackList);
		}
	}

	private static int parseIntOrZero(String numberToBe) {
		try {
			return Integer.parseInt(numberToBe);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
