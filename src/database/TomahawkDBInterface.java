package database;

import java.util.List;
import java.util.UUID;

import database.model.Album;
import database.model.Artist;
import database.model.FileAction;
import database.model.Track;

public interface TomahawkDBInterface {

	public void connect(String persistenceUnitName);

	public void close();

	public List<Track> getAllTracks();

	public Track getTrackById(int id);

	public void newFiles(List<Track> audioFiles);

	public void deleteFiles(List<Track> audioFiles);

	public List<FileAction> getAllFileActions();

	public List<FileAction> getFileActionsSince(UUID uuid);

	/**
	 * returns the artist or an empty artist object. storing a new artist is implicit done by saving the track.
	 * 
	 * @param name
	 * @return
	 */
	public Artist getArtistByName(String name);

	/**
	 * returns the album or an empty album object. storing a new album is implicit done by saving the track.
	 * 
	 * @param name
	 * @return
	 */
	public Album getAlbumByName(String name);
}
