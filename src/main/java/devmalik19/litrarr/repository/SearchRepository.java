package devmalik19.litrarr.repository;

import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dao.Search;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchRepository extends JpaRepository<Search, Integer>
{
	@Modifying
	@Transactional
	@Query("UPDATE Search s SET s.status = :newStatus WHERE s.status = :oldStatus")
	int update(SearchStatus oldStatus, SearchStatus newStatus);

    List<Search> findByStatus(SearchStatus searchStatus);
}
