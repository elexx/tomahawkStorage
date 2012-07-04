package filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.TomahawkDBInterface;
import database.model.Album;
import database.model.Artist;
import database.model.Track;

public class FileScanner implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);

	private final Queue<DirectoryScanRequest> queue = new ConcurrentLinkedQueue<>();
	private final List<NewFileCallback> callbacks = new LinkedList<>();
	private final Set<Path> watchedDirectories = new HashSet<>();
	private final TomahawkDBInterface database;

	public FileScanner(final TomahawkDBInterface database) {
		this.database = database;
	}

	public void watchDirectory(final Path path) {
		LOG.debug("adding {}", path);
		watchedDirectories.add(path);
	}

	public void stopScanner() {
		synchronized (queue) {
			queue.add(new DirectoryScanRequest(ScanRequestType.STOP_SCANNER));
			queue.notify();
		}
	}

	public void addNetworkCallback(NewFileCallback callback) {
		callbacks.add(callback);
	}

	public void asyncScanWatchedDirectories() {
		synchronized (queue) {
			queue.add(new DirectoryScanRequest(ScanRequestType.SCAN_DIRECTOIES));
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

			for (Path watchedDir : watchedDirectories) {
				try {
					LOG.debug("parsing {}", watchedDir);
					Files.walkFileTree(watchedDir.toAbsolutePath(), fileWorker);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			List<Track> alreadyKnownTracks = database.getAllTracks();
			List<Path> fileList = fileWorker.getList();

			Map<String, Artist> artists = new HashMap<>();
			Map<String, Album> albums = new HashMap<>();
			List<Track> newTracks = new ArrayList<>();
			List<Track> movedTracks = new ArrayList<>();

			for (Path file : fileList) {
				try {
					BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
					Date lastModification = new Date(fileAttributes.lastModifiedTime().toMillis());

					AudioFile audioFile = AudioFileIO.read(file.toFile());
					MP3File mp3File = (MP3File) audioFile;

					Track track = new Track();
					track.bitrate = mp3File.getMP3AudioHeader().getBitRateAsNumber();
					track.createTimestamp = lastModification;
					track.duration = (long) mp3File.getAudioHeader().getTrackLength();
					track.mimetype = "audio/mpeg";
					track.path = file.toAbsolutePath().toString();
					track.releaseyear = parseIntOrZero(mp3File.getID3v2TagAsv24().getFirst(FieldKey.YEAR));
					track.size = fileAttributes.size();
					track.title = mp3File.getID3v2TagAsv24().getFirst(FieldKey.TITLE);
					track.tracknumber = parseIntOrZero(mp3File.getID3v2TagAsv24().getFirst(FieldKey.TRACK));

					String artistName = mp3File.getID3v2TagAsv24().getFirst(FieldKey.ARTIST);
					Artist artist;
					if (!artists.containsKey(artistName)) {
						artist = database.getArtistByName(artistName);
						artist.name = artistName;
						artists.put(artistName, artist);
					} else {
						artist = artists.get(artistName);
					}
					track.artist = artist;

					String albumName = mp3File.getID3v2TagAsv24().getFirst(FieldKey.ALBUM);
					Album album;
					if (!albums.containsKey(albumName)) {
						album = database.getAlbumByName(albumName);
						album.name = albumName;

						if (null == album.artist) {
							String albumArtistName = mp3File.getID3v2TagAsv24().getFirst(FieldKey.ALBUM_ARTIST);
							if (albumArtistName.length() == 0) {
								album.artist = artist;
							} else {
								Artist albumArtist;
								if (!artists.containsKey(albumArtistName)) {
									albumArtist = database.getArtistByName(albumArtistName);
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

					boolean knownTrack = false;
					Iterator<Track> alreadyKnownTracksIterator = alreadyKnownTracks.iterator();
					while (alreadyKnownTracksIterator.hasNext()) {
						Track t = alreadyKnownTracksIterator.next();
						if (track.equals(t)) {
							alreadyKnownTracksIterator.remove();
							if (!track.path.equals(t.path)) {
								t.path = track.path;
								movedTracks.add(t);
							}
							knownTrack = true;
							break;
						}
					}
					if (!knownTrack) {
						newTracks.add(track);
					}

				} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
					e.printStackTrace();
				}
			}

			LOG.debug("{} new files", newTracks.size());
			if (newTracks.size() > 0) {
				database.newFiles(newTracks);
			}

			LOG.debug("{} files deleted", alreadyKnownTracks.size());
			if (alreadyKnownTracks.size() > 0) {
				database.deleteFiles(alreadyKnownTracks);
			}

			LOG.debug("{} files updated", movedTracks.size());
			if (movedTracks.size() > 0) {
				database.updateFiles(movedTracks);
			}

			if (newTracks.size() > 0 || alreadyKnownTracks.size() > 0) {
				for (NewFileCallback callback : callbacks) {
					callback.filesAddedOrRemoved();
				}
			}
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
