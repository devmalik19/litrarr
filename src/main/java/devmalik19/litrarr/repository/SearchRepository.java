package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.Search;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchRepository extends JpaRepository<Search, String>
{
}
