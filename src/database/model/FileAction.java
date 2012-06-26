package database.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NamedQueries({ @NamedQuery(name = "getAllFileActions", query = "SELECT f FROM FileAction f"), @NamedQuery(name = "getFileActionsSince", query = "SELECT f FROM FileAction f WHERE f.created >= (SELECT MAX(ff.created) FROM FileAction ff WHERE ff.uuid = :uuid) ORDER BY f.created ") })
public class FileAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;

	@Temporal(value = TemporalType.TIMESTAMP)
	@Column(nullable = false, updatable = false)
	public Date created;

	@Column(unique = true, nullable = false, updatable = false)
	public UUID uuid;

	@PrePersist
	void created() {
		created = new Date();
	}
}
