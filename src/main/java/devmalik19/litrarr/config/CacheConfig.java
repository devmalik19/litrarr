package devmalik19.litrarr.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig
{
	@Bean
	public CacheManager cacheManager()
	{
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(
			"ProwlarrSearchResult",
			"GoogleBooksMetadata",
			"ComicVineMetadata",
			"MyAnimeListMetadata"
		);
		cacheManager.setCaffeine(Caffeine.newBuilder()
			.expireAfterWrite(30, TimeUnit.MINUTES)
			.maximumSize(500)
		);
		return cacheManager;
	}
}
