package database.model;

import java.util.ArrayList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
public class DeleteFileAction extends FileAction {

	@Lob
	@Column(nullable = false)
	public ArrayList<Integer> deletedFileIds;

}
