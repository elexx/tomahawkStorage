package database.model;

import java.util.Calendar;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Track")
@NamedQuery(name = "getAllTracks", query = "SELECT t FROM Track t")
public class Track {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;

	@Column(nullable = false)
	public String title;

	@ManyToOne(cascade = { CascadeType.ALL }, optional = false)
	public Artist artist;

	@ManyToOne(cascade = { CascadeType.ALL }, optional = false)
	public Album album;

	@Column(nullable = false)
	public int tracknumber;

	@Column(nullable = false)
	public int releaseyear;

	@Column(nullable = false)
	public String path;

	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Calendar createTimestamp;

	@Column(nullable = false)
	public long size;

	@Column(nullable = false)
	public String mimetype;

	@Column(nullable = false)
	public long duration;

	@Column(nullable = false)
	public long bitrate;

	@ManyToOne(cascade = { CascadeType.ALL }, optional = false)
	public NewFileAction newFileAction;
}
