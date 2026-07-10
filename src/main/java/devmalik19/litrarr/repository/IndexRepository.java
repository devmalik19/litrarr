package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.Index;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

}