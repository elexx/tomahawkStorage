package database.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Track")
@NamedQueries({ @NamedQuery(name = "getAllTracks", query = "SELECT t FROM Track t"), @NamedQuery(name = "getTrackById", query = "SELECT t FROM Track t WHERE id = :id") })
public class Track {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;

	@Column(nullable = false)
	public String title;

	@ManyToOne(cascade = { CascadeType.PERSIST }, optional = false)
	public Artist artist;

	@ManyToOne(cascade = { CascadeType.PERSIST }, optional = false)
	public Album album;

	@Column(nullable = false)
	public Integer tracknumber;

	@Column(nullable = false)
	public Integer releaseyear;

	@Column(nullable = false)
	public String path;

	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date createTimestamp;

	@Column(nullable = false)
	public Long size;

	@Column(nullable = false)
	public String mimetype;

	@Column(nullable = false)
	public Long duration;

	@Column(nullable = false)
	public Long bitrate;

	@ManyToOne(cascade = { CascadeType.PERSIST }, optional = false)
	public NewFileAction newFileAction;

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass() == Track.class) {
			Track t = (Track) obj;
			return (t.album.equals(album) && t.artist.equals(artist) && t.bitrate.equals(bitrate) && t.createTimestamp.getTime() == createTimestamp.getTime() && t.duration.equals(duration) && t.size.equals(size) && t.title.equals(title) && t.tracknumber.equals(tracknumber));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (album.hashCode() ^ artist.hashCode() ^ bitrate.hashCode() ^ createTimestamp.hashCode() ^ duration.hashCode() ^ size.hashCode() ^ title.hashCode() ^ tracknumber.hashCode());
	}

}
