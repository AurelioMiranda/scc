package tukano.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {
	private static final String RedisHostname = "scc70068tukanoredis.redis.cache.windows.net";
	private static final String RedisKey = "NA0WgRPMdhZFdLi1WWxszdLO2tWu5hAPPAzCaFpg3pU=";
	private static final int REDIS_PORT = 6380;
	private static final int REDIS_TIMEOUT = 1000;
	private static final boolean Redis_USE_TLS = true;
	public static final String NUM_USERS_COUNTER = "NumUsers";
	public static final String NUM_SHORTS_COUNTER = "NumShorts";
	public static final int ALIVE_TIME = 600; // too much? too little?
	
	private static JedisPool instance;
	
	public synchronized static JedisPool getCachePool() {
		if( instance != null)
			return instance;
		
		var poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		instance = new JedisPool(poolConfig, RedisHostname, REDIS_PORT, REDIS_TIMEOUT, RedisKey, Redis_USE_TLS);
		return instance;
	}
}
