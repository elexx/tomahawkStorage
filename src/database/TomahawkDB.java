package database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.model.Album;
import database.model.Artist;
import database.model.DeleteFileAction;
import database.model.FileAction;
import database.model.NewFileAction;
import database.model.Track;

public class TomahawkDB implements TomahawkDBInterface {

	private static final Logger LOG = LoggerFactory.getLogger(TomahawkDB.class);

	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;

	@Override
	public void connect(String persistenceUnitName) {
		LOG.info("connecting to " + persistenceUnitName);
		entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
		entityManager = entityManagerFactory.createEntityManager();
	}

	@Override
	public List<Track> getAllTracks() {
		return entityManager.createNamedQuery("getAllTracks", Track.class).getResultList();
	}

	@Override
	public Track getTrackById(int id) {
		List<Track> tracks = entityManager.createNamedQuery("getTrackById", Track.class).setParameter("id", id).getResultList();
		return tracks.size() > 0 ? tracks.get(0) : null;
	}

	@Override
	public void newFiles(List<Track> audioFiles) {
		if (null != audioFiles && audioFiles.size() > 0) {
			NewFileAction fileAction = new NewFileAction();
			fileAction.uuid = UUID.randomUUID();

			for (Track t : audioFiles) {
				t.newFileAction = fileAction;
			}

			fileAction.newTracks = audioFiles;

			synchronized (entityManager) {
				entityManager.getTransaction().begin();
				entityManager.persist(fileAction);
				entityManager.getTransaction().commit();
			}
		}
	}

	@Override
	public void deleteFiles(List<Track> audioFiles) {
		if (null != audioFiles && audioFiles.size() > 0) {
			DeleteFileAction fileAction = new DeleteFileAction();
			fileAction.uuid = UUID.randomUUID();

			fileAction.deletedFileIds = new ArrayList<>(audioFiles.size());

			synchronized (entityManager) {
				entityManager.getTransaction().begin();
				for (Track t : audioFiles) {
					fileAction.deletedFileIds.add(t.id);
					entityManager.remove(t);
				}
				entityManager.persist(fileAction);
				entityManager.getTransaction().commit();
			}
		}
	}

	@Override
	public void updateFiles(List<Track> audioFiles) {
		if (null != audioFiles && audioFiles.size() > 0) {
			synchronized (entityManager) {
				entityManager.getTransaction().begin();
				for (Track t : audioFiles) {
					entityManager.merge(t);
				}
				entityManager.getTransaction().commit();
			}
		}
	}

	@Override
	public List<FileAction> getAllFileActions() {
		return entityManager.createNamedQuery("getAllFileActions", FileAction.class).getResultList();
	};

	@Override
	public List<FileAction> getFileActionsSince(UUID uuid) {
		return entityManager.createNamedQuery("getFileActionsSince", FileAction.class).setParameter("uuid", uuid).getResultList();
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
		LOG.trace("shutdown");
		entityManager.close();
		entityManagerFactory.close();
	}
}
