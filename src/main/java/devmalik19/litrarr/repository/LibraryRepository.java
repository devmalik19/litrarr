package devmalik19.litrarr.repository;

import devmalik19.litrarr.constants.LibraryTypes;
import devmalik19.litrarr.data.dao.Library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRepository extends JpaRepository<Library, String>
{
	Optional<Library> findByPath(String path);
	List<Library> findByType(LibraryTypes type);
}
