package devmalik19.litrarr.data.dao;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Search
{
	@Id
	private String query;
	private String data;
}
