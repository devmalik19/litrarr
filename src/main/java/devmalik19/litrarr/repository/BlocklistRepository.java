package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.Blocklist;
import devmalik19.litrarr.data.dao.Search;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlocklistRepository extends JpaRepository<Blocklist, Integer>
{
	List<Blocklist> findBySearch(Search search);

	void deleteBySearch(Search search);
}
