package devmalik19.litrarr.data.dao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Blocklist
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "search_id")
	private Search search;

	private String identifier;
	private String service;
	private LocalDateTime blockedAt;

	@PrePersist
	protected void onCreate()
	{
		blockedAt = LocalDateTime.now();
	}
}
