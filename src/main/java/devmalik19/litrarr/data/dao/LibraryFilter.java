package devmalik19.litrarr.data.dao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class LibraryFilter
{
	@Id
	private String path;
	private String type;
}
