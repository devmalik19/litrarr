package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.Item;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, String>
{
	Optional<Item> findByPath(String path);
}
