package database.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "getArtistByName", query = "SELECT a FROM Artist a WHERE a.name = :name")
public class Artist {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;

	@Column(nullable = false, unique = true)
	public String name;

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass() == Artist.class) {
			Artist a = (Artist) obj;
			return a.name.equals(name);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
