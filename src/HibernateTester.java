import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import database.model.Album;
import database.model.Artist;
import database.model.DeleteFileAction;
import database.model.NewFileAction;
import database.model.Track;

public class HibernateTester {

	public static void main(String[] args) {
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("tomahawk.storage.jpa");

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		NewFileAction fileAction = new NewFileAction();
		fileAction.newTracks = new ArrayList<>();
		fileAction.uuid = UUID.randomUUID();

		{
			Track t = new Track();
			t.createTimestamp = Calendar.getInstance();
			t.mimetype = "mime";
			t.path = "path";
			t.title = "title";
			t.album = new Album();
			t.album.name = "album name";
			t.album.artist = new Artist();
			t.album.artist.name = "artist name";
			t.artist = t.album.artist;
			t.newFileAction = fileAction;
			fileAction.newTracks.add(t);
			entityManager.persist(fileAction);
		}
		{
			DeleteFileAction df = new DeleteFileAction();
			df.deletedFileIds = new ArrayList<>();
			df.deletedFileIds.add(1);
			df.deletedFileIds.add(2);
			df.deletedFileIds.add(3);
			df.uuid = UUID.randomUUID();
			entityManager.persist(df);
		}

		entityManager.getTransaction().commit();
		entityManager.close();

		entityManagerFactory.close();
	}
}
