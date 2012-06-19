package database.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "getAlbumByName", query = "SELECT a FROM Album a WHERE a.name = :name")
public class Album {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;

	@Column(nullable = false, unique = true)
	public String name;

	@ManyToOne(cascade = { CascadeType.ALL }, optional = false)
	public Artist artist;
}
