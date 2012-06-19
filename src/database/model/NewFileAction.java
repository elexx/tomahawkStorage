package database.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
public class NewFileAction extends FileAction {

	@OneToMany(cascade = { CascadeType.PERSIST }, mappedBy = "newFileAction")
	public List<Track> newTracks;

}
