package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.DB;
import tukano.db.CosmosDBLayer;

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
			return error(BAD_REQUEST);

		return Result.errorOrValue(
				CosmosDBLayer.getInstance().insertOne(user),
				user.getUserId());
	}

	@Override
	public Result<User> getUser(String userId, String pwd) {
		Log.info(() -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null) {
			return Result.error(Result.ErrorCode.BAD_REQUEST);
		}

		return Result.errorOrResult(
				CosmosDBLayer.getInstance().getOne(userId, User.class),
				user -> validatedUserOrError(Result.ok(user), pwd));
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);

		return errorOrResult(
				validatedUserOrError(CosmosDBLayer.getInstance().getOne(userId, User.class), pwd),
				user -> CosmosDBLayer.getInstance().updateOne(user.updateFrom(other)));
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null)
			return error(BAD_REQUEST);

		return errorOrResult(
				validatedUserOrError(CosmosDBLayer.getInstance().getOne(userId, User.class), pwd),
				user -> { // TODO: switch to function
					Executors.defaultThreadFactory().newThread(() -> {
						JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
						JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
					}).start();
					
					var deletedUser = CosmosDBLayer.getInstance().deleteOne(user);

					if ((Result<User>)deletedUser instanceof Result<User>){
						return (Result<User>)deletedUser;
					}

					return error(NOT_FOUND);
				});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info(() -> format("searchUsers : pattern = %s\n", pattern));

		var query = format("SELECT * FROM c WHERE CONTAINS(LOWER(c.userId), '%s')", pattern.toLowerCase());
		var hits = CosmosDBLayer.getInstance().query(User.class, query);

		List<User> users = hits.value().stream()
				.map(User::copyWithoutPassword)
				.toList();
		return Result.ok(users);
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
