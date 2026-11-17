package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {
    private final RedisConfig redisConfig = new RedisConfig();
    @Test
    void shouldConfigureRedisTemplateCorrectly() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);
        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }
}