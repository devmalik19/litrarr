package devmalik19.litrarr.repository;

import devmalik19.litrarr.data.dao.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<Setting, String>
{
}
