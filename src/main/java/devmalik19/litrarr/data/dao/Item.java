package devmalik19.litrarr.data.dao;

import devmalik19.litrarr.constants.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Item
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String guid;
	private String name;
	private Category type;
	private String path;
	private String image;
	private String author;
	private String publisher;

	private LocalDate releaseOn;

	@Column(nullable = false)
	private boolean missing = false;

	@ManyToOne
	@JoinColumn(name = "parent")
	private Library library;
}
