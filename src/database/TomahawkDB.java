package database;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import database.model.Album;
import database.model.Artist;
import database.model.NewFileAction;
import database.model.Track;

public class TomahawkDB implements TomahawkDBInterface {

	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;

	@Override
	public void connect(String persistenceUnitName) {
		entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
		entityManager = entityManagerFactory.createEntityManager();
	}

	@Override
	public List<Track> getAllTracks() {
		return entityManager.createNamedQuery("getAllTracks", Track.class).getResultList();
	}

	@Override
	public synchronized void newFiles(List<Track> audioFiles) {
		if (null != audioFiles && audioFiles.size() > 0) {
			NewFileAction fileAction = new NewFileAction();
			fileAction.uuid = UUID.randomUUID();

			for (Track t : audioFiles) {
				t.newFileAction = fileAction;
			}

			fileAction.newTracks = audioFiles;

			entityManager.getTransaction().begin();
			entityManager.persist(fileAction);
			entityManager.getTransaction().commit();
		}
	}

	@Override
	public synchronized void deleteFiles(List<Track> audioFiles) {
		if (null != audioFiles && audioFiles.size() > 0) {
			entityManager.getTransaction().begin();
			entityManager.remove(audioFiles);
			entityManager.getTransaction().commit();
		}
	}

	@Override
	public Artist getArtistByName(String name) {
		List<Artist> artists = entityManager.createNamedQuery("getArtistByName", Artist.class).setParameter("name", name).getResultList();
		return artists.size() > 0 ? artists.get(0) : new Artist();
	}

	@Override
	public Album getAlbumByName(String name) {
		List<Album> albums = entityManager.createNamedQuery("getAlbumByName", Album.class).setParameter("name", name).getResultList();
		return albums.size() > 0 ? albums.get(0) : new Album();
	}

	@Override
	public void close() {
		entityManager.close();
		entityManagerFactory.close();
	}
}
