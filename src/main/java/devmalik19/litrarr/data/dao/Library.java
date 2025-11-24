package devmalik19.litrarr.data.dao;

import devmalik19.litrarr.constants.FolderType;
import devmalik19.litrarr.constants.Category;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Library
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String guid;
	private String name;
	private FolderType type;
	private Category category;
	private String path;
	private String image;
	private String creator;

	@ManyToOne
	@JoinColumn(name = "parent")
	private Library library;

	@OneToMany(mappedBy = "library")
	private List<Library> libraryList;

	@OneToMany(mappedBy = "library")
	private List<Item> itemList;
}
