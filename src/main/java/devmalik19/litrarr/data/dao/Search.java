package devmalik19.litrarr.data.dao;


import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dto.DownloadState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
public class Search
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String title;
	private String author;
	private String issue;
	private String year;
	private Category category;
	private SearchStatus status;

	@ManyToOne
	@JoinColumn(name = "library")
	private Library library;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "TEXT")
	private DownloadState data;
}
