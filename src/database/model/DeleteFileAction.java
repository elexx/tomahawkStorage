package database.model;

import java.util.ArrayList;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class DeleteFileAction extends FileAction {

	@Column(nullable = false)
	public ArrayList<Integer> deletedFileIds;

}
