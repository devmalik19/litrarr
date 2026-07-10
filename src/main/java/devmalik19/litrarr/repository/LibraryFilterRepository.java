package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.LibraryFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LibraryFilterRepository extends JpaRepository<LibraryFilter, String>
{
}
