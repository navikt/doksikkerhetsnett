package no.nav.doksikkerhetsnett.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String STS_CACHE = "stsCache";

    @Bean
    @Primary
    @Profile({"nais"})
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
				new CaffeineCache(STS_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(55, MINUTES)
						.build())
		));
        return manager;
    }
}
