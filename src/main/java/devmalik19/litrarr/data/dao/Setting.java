package devmalik19.litrarr.data.dao;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "settings")
public class Setting
{
	@Id
	private String key;
	private String value;
}
