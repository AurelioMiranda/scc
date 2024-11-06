package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.errorOrVoid;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static utils.DB.getOne;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import redis.clients.jedis.Jedis;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.User;
import tukano.cache.RedisCache;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestServer;
import utils.DB;
import utils.JSON;
import tukano.db.CosmosDBLayer;

public class JavaShorts implements Shorts {

	private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

	private static Shorts instance;

	synchronized public static Shorts getInstance() {
		if (instance == null)
			instance = new JavaShorts();
		return instance;
	}

	private JavaShorts() {
	}

	@Override
	public Result<Short> createShort(String userId, String password) {
		Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

		return errorOrResult(okUser(userId, password), user -> {

			var shortId = format("%s+%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);
			var shrt = new Short(shortId, userId, blobUrl);
			shrt.setId(shortId);

			try (Jedis jedis = RedisCache.getCachePool().getResource()) {
				var key = "short:" + shortId;
				var value = JSON.encode(shrt);
				jedis.set(key, value);
				jedis.expire(key, RedisCache.ALIVE_TIME);

				var cnt = jedis.incr(RedisCache.NUM_SHORTS_COUNTER);
				Log.info("Total shorts: " + cnt);
			}

			return errorOrValue(
					CosmosDBLayer.getInstance().insertOne(CosmosDBLayer.CONTAINER_SHORTS, shrt),
					s -> s.copyWithLikes_And_Token(0));
		});
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Log.info(() -> format("getShort : shortId = %s\n", shortId));

		if (shortId == null)
			return error(BAD_REQUEST);

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = "short:" + shortId;
			var val = jedis.get(key);

			if (val != null) {
				jedis.expire(key, RedisCache.ALIVE_TIME);
				var short1 = JSON.decode(val, Short.class);
				return Result.ok(short1);
			}
		}

		var query = format("SELECT VALUE COUNT(1) FROM Likes l WHERE l.shortId = '%s'", shortId);
		var likes = CosmosDBLayer.getInstance().query(CosmosDBLayer.CONTAINER_LIKES, Long.class, query).value();

		return errorOrValue(
				CosmosDBLayer.getInstance().getOne(CosmosDBLayer.CONTAINER_SHORTS, shortId,
						Short.class),
				shrt -> (Short) shrt.copyWithLikes_And_Token(likes.get(0)));
	}

	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = "short:" + shortId;
			var val = jedis.get(key);

			if (val != null) {
				jedis.del(key);
			}
		}

		return errorOrResult(getShort(shortId), shrt -> {
			return errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
				Result<?> deleteShortResult = CosmosDBLayer.getInstance().deleteOne(CosmosDBLayer.CONTAINER_SHORTS,
						shrt);

				if (!deleteShortResult.isOK()) {
					return Result.error(deleteShortResult.error());
				}

				return Result.ok();
			});
		});
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		// cache for search?

		var query = format("SELECT s.shortId FROM Short s WHERE s.ownerId = '%s'", userId);
		Result<List<Short>> shortIdResults = CosmosDBLayer.getInstance().query(CosmosDBLayer.CONTAINER_SHORTS,
				Short.class, query);

		if (shortIdResults.isOK()) {
			List<String> shortIds = shortIdResults.value().stream()
					.map(Short::getShortId)
					.collect(Collectors.toList());
			return ok(shortIds);
		} else {
			return error(shortIdResults.error());
		}
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
		Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n", userId1, userId2,
				isFollowing, password));

		return errorOrResult(okUser(userId1, password), user -> {
			var f = new Following(userId1, userId2);
			return errorOrVoid(okUser(userId2),
					isFollowing ? CosmosDBLayer.getInstance().insertOne(CosmosDBLayer.CONTAINER_FOLLOWING, f)
							: CosmosDBLayer.getInstance().deleteOne(CosmosDBLayer.CONTAINER_FOLLOWING, f));
		});
	}

	@Override
	public Result<List<String>> followers(String userId, String password) {
		Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

		var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
		return errorOrValue(okUser(userId, password),
				CosmosDBLayer.getInstance().query(CosmosDBLayer.CONTAINER_SHORTS, String.class, query));
	}

	@Override
	public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
		Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked,
				password));

		return errorOrResult(getShort(shortId), shrt -> {
			var l = new Likes(userId, shortId, shrt.getOwnerId());
			return errorOrVoid(okUser(userId, password),
					isLiked ? CosmosDBLayer.getInstance().insertOne(CosmosDBLayer.CONTAINER_LIKES, l)
							: CosmosDBLayer.getInstance().deleteOne(CosmosDBLayer.CONTAINER_LIKES, l));
		});
	}

	@Override
	public Result<List<String>> likes(String shortId, String password) {
		Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

		return errorOrResult(getShort(shortId), shrt -> {
			var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);
			Log.info(() -> "Executing query: " + query);

			var queryResult = CosmosDBLayer.getInstance().query(CosmosDBLayer.CONTAINER_SHORTS, String.class, query);

			Log.info(() -> "Query Result: " + queryResult.value());

			if (queryResult == null || queryResult.value().isEmpty()) {
				Log.info("No likes found for shortId: " + shortId);
				return Result.ok(Collections.emptyList());
			}

			return errorOrValue(okUser(shrt.getOwnerId(), password), queryResult);
		});
	}

	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));
		String key = "feed :" + userId;

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var cachedFeed = jedis.get(key);
			if (cachedFeed != null) {
				jedis.expire(key, RedisCache.ALIVE_TIME);
				List<String> feed = Arrays
						.asList(cachedFeed.replace("[", "")
								.replace("]", "").replace("\"", "").split(","));
				return Result.ok(feed);
			}
		}

		final var QUERY_FMT = """
				SELECT s.shortId, s.timestamp FROM Short s WHERE	s.ownerId = '%s'
				UNION
				SELECT s.shortId, s.timestamp FROM Short s, Following f
					WHERE
						f.followee = s.ownerId AND f.follower = '%s'
				ORDER BY s.timestamp DESC""";
		var queryResult = CosmosDBLayer.getInstance().query(CosmosDBLayer.CONTAINER_SHORTS, String.class,
				format(QUERY_FMT, userId, userId));

		if (queryResult.isOK()) {
			List<String> feed = queryResult.value();
			try (Jedis jedis = RedisCache.getCachePool().getResource()) {
				jedis.set(key, JSON.encode(feed));
				jedis.expire(key, RedisCache.ALIVE_TIME);
			}
			return Result.ok(feed);
		}

		return queryResult;
	}

	protected Result<User> okUser(String userId, String pwd) {
		return JavaUsers.getInstance().getUser(userId, pwd);
	}

	private Result<Void> okUser(String userId) {
		var res = okUser(userId, "");
		if (res.error() == FORBIDDEN)
			return ok();
		else
			return error(res.error());
	}

	@Override
	public Result<Void> deleteAllShorts(String userId, String password, String token) {
		Log.info(() -> format("deleteAllShorts : userId = %s\n", userId));

		String query = format("SELECT * FROM c WHERE c.ownerId = '%s'", userId);
		Result<List<Short>> shortsRes = CosmosDBLayer.getInstance().query(CosmosDBLayer.CONTAINER_SHORTS, Short.class,
				query);

		if (!shortsRes.isOK()) {
			return Result.error(shortsRes.error());
		}

		for (Short shrt : shortsRes.value()) {
			Result<?> deleteShortResult = CosmosDBLayer.getInstance().deleteOne(CosmosDBLayer.CONTAINER_SHORTS, shrt);

			if (!deleteShortResult.isOK()) {
				return Result.error(deleteShortResult.error());
			}

			try {
				Result<Void> blobDeleteRe = JavaBlobs.getInstance().delete(shrt.getBlobUrl(), token);
				if (!blobDeleteRe.isOK()) {
					return Result.error(blobDeleteRe.error());
				}
			} catch (IllegalArgumentException e) {
				Log.info("Failed to delete blob due to invalid connection string: " + e.getMessage());
				return Result.ok();
			}
		}

		return Result.ok();
	}

}