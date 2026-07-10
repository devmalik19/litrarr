package devmalik19.litrarr.data.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Getter
@Setter
@Table(name = "indexes")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Index
{
    @Id
    private int id;
    private String name;
	@ColumnTransformer(write = "UPPER(?)")
	private String protocol;
	private int[] tags;
	private boolean enable;
}