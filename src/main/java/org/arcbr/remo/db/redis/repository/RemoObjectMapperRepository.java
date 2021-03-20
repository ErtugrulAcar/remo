package org.arcbr.remo.db.redis.repository;

import org.arcbr.remo.app.RedisConnection;
import org.arcbr.remo.exception.RemoRedisEntityDecodeException;
import org.arcbr.remo.exception.RemoRedisEntityEncodeException;
import org.arcbr.remo.exception.RemoRedisEntityNotFoundException;
import org.springframework.data.redis.hash.ObjectHashMapper;
import redis.clients.jedis.Jedis;

import java.util.LinkedHashMap;
import java.util.Map;

public class RemoObjectMapperRepository implements RemoRedisRepository {

    private RedisConnection redisConnection;
    private ObjectHashMapper objectMapper;
    private int ttl;

    public RemoObjectMapperRepository(RedisConnection redisConnection, int ttl) {
        this.redisConnection = redisConnection;
        this.ttl = ttl;
        this.objectMapper = new ObjectHashMapper();
    }


    @Override
    public void set(String key, Object o) {
        Map<String, String> stringMap;
        try{
            Map<byte [], byte []> byteMap = objectMapper.toHash(o);
            stringMap = toStringMap( byteMap );
        }catch (Exception e){
            e.printStackTrace();
            throw new RemoRedisEntityEncodeException(e.getMessage());
        }
        Jedis jedis = redisConnection.get();
        jedis.hset(key, stringMap);
        if (ttl != -1)
            jedis.expire(key, ttl);
        jedis.close();
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Jedis jedis = redisConnection.get();
        Map<String, String> stringMap = jedis.hgetAll(key);
        if (stringMap.isEmpty()){
            throw new RemoRedisEntityNotFoundException("Entity not found with key: " + key);
        }
        jedis.close();
        try{
            Map<byte [], byte []> byteMap = toByteMap(stringMap);
            return objectMapper.fromHash(byteMap, clazz);
        }catch (Exception e){
            throw new RemoRedisEntityDecodeException(e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        Jedis jedis = redisConnection.get();
        jedis.del(key);
        jedis.close();
    }


    private Map<String, String> toStringMap(Map<byte[], byte []> val1){
        Map<String, String> val2 = new LinkedHashMap<>();
        val1.forEach( (k, v) -> val2.put( new String( k ), new String( v ) ));
        return val2;
    }

    private Map<byte [], byte []> toByteMap(Map<String, String> val1){
        Map<byte [], byte []> val2 = new LinkedHashMap<>();
        val1.forEach( (k, v) -> val2.put(k.getBytes(), v.getBytes()));
        return val2;
    }
}
