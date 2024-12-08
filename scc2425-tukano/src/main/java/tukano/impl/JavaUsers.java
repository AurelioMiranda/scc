package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import tukano.cache.RedisCache;
import utils.DB;
import utils.JSON;

public class JavaUsers implements Users {

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());
	private static Users instance;

	synchronized public static Users getInstance() {
		if (instance == null)
			instance = new JavaUsers();
		return instance;
	}

	private JavaUsers() {
	}

	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if (badUserInfo(user))
			return error(FORBIDDEN);

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = "user:" + user.getUserId();
			var value = JSON.encode(user);
			jedis.set(key, value);
			jedis.expire(key, RedisCache.ALIVE_TIME);
			var cnt = jedis.incr(RedisCache.NUM_USERS_COUNTER);
			Log.info("Total users: " + cnt);
		}

		return errorOrValue(DB.insertOne(user), user.getUserId());
	}

	@Override
	public Result<User> getUser(String userId, String pwd) {
		Log.info(() -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null)
			return error(BAD_REQUEST);

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = "user:" + userId;
			var val = jedis.get(key);
		
			if (val != null) {
				jedis.expire(key, RedisCache.ALIVE_TIME);
				var user = JSON.decode(val, User.class);
				return validatedUserOrError(ok(user), pwd);
			}
		}

		return validatedUserOrError(DB.getOne(userId, User.class), pwd);
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = "user:" + userId;
			var val = jedis.get(key);

			if (val != null) {
				jedis.expire(key, RedisCache.ALIVE_TIME);
				jedis.set(key, JSON.encode(other));
			}
		}

		return errorOrResult(validatedUserOrError(DB.getOne(userId, User.class), pwd),
				user -> DB.updateOne(user.updateFrom(other)));
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null)
			return error(BAD_REQUEST);

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = "user:" + userId;
			var val = jedis.get(key);
	
			if (val != null) {
				jedis.del(key);
			}
		}

		return errorOrResult(validatedUserOrError(DB.getOne(userId, User.class), pwd), user -> {

			Executors.defaultThreadFactory().newThread(() -> {
				JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
				JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
			}).start();

			return DB.deleteOne(user);
		});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info(() -> format("searchUsers : patterns = %s\n", pattern));

		var query = format("SELECT * FROM User u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
		var hits = DB.sql(query, User.class)
				.stream()
				.map(User::copyWithoutPassword)
				.toList();
		return ok(hits);
	}

	private Result<User> validatedUserOrError(Result<User> res, String pwd) {
		if (res.isOK())
			return res.value().getPwd().equals(pwd) ? res : error(FORBIDDEN);
		else
			return res;
	}

	private boolean badUserInfo(User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}

	private boolean badUpdateUserInfo(String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getUserId() != null && !userId.equals(info.getUserId()));
	}
}