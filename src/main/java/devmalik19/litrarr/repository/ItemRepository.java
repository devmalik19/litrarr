package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, String>
{
	Optional<Item> findByPath(String path);

	List<Item> findByLibrary(Library library);

	List<Item> findByLibraryAndMissingTrue(Library library);
}
