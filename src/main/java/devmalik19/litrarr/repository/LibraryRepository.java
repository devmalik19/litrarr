package devmalik19.litrarr.repository;

import devmalik19.litrarr.constants.FolderType;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LibraryRepository extends JpaRepository<Library, Integer>
{
	Optional<Library> findByPath(String path);
	List<Library> findByCategory(Category category);
	List<Library> findByTypeAndCategory(FolderType type, Category category);

	@Modifying
	@Transactional
	@Query("UPDATE Library l SET l.metadataFetched = false")
	int resetAllMetadataFlags();
}
