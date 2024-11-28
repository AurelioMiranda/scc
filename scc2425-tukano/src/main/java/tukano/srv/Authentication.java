package tukano.srv;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.hsqldb.persist.Log;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import tukano.api.Result;
import tukano.impl.JavaShorts;
import tukano.impl.JavaUsers;
import tukano.api.User;
import tukano.cache.RedisCache;
import tukano.srv.auth.RequestCookies;
import utils.JSON;
import java.util.logging.Logger;
import utils.Props;

@Path(Authentication.PATH)
public class Authentication {
	static final String PATH = "login";
	static final String USER = "username";
	static final String PWD = "password";
	static final String COOKIE_KEY = "scc:session";
	static final String LOGIN_PAGE = "login.html";
	private static final int MAX_COOKIE_AGE = 3600;
	static final String REDIRECT_TO_AFTER_LOGIN = "ctrl/login";
	private static Logger Log = Logger.getLogger(Authentication.class.getName());

	@POST
	public Response login(@FormParam(USER) String user, @FormParam(PWD) String password) {
		System.out.println("user: " + user + " pwd:" + password);

		Result<User> userResult = JavaUsers.getInstance().getUser(user, password);
		boolean pwdOk = false;

		if (userResult.isOK()) {
			pwdOk = true;
		}

		if (pwdOk) {
			String uid = UUID.randomUUID().toString();
			var cookie = new NewCookie.Builder(COOKIE_KEY)
					.value(uid).path("/")
					.comment("sessionid")
					.maxAge(MAX_COOKIE_AGE)
					.secure(false) // ideally it should be true to only work for https requests
					.httpOnly(true)
					.build();

			/*
			 * try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			 * var key = "session:" + user;
			 * var value = JSON.encode(user); // TODO: encode a Session
			 * jedis.set(key, value);
			 * jedis.expire(key, RedisCache.ALIVE_TIME);
			 * }
			 */
			FakeRedisLayer.getInstance().putSession(new Session(uid, user));

			return Response.seeOther(URI.create(REDIRECT_TO_AFTER_LOGIN))
					.cookie(cookie)
					.build();
		} else
			throw new NotAuthorizedException("Incorrect login");
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String login() {
		try {
			var in = getClass().getClassLoader().getResourceAsStream(LOGIN_PAGE);
			return new String(in.readAllBytes());
		} catch (Exception x) {
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
	}

	static public Session validateSession(String userId) throws NotAuthorizedException {
		var cookies = RequestCookies.get();
		return validateSession(cookies.get(COOKIE_KEY), userId);
	}

	static public Session validateSession(Cookie cookie, String userId) throws NotAuthorizedException {

		if (cookie == null)
			throw new NotAuthorizedException("No session initialized");

		// var session = getUserSession(cookie.getValue()); // TODO: use cache
		var session = FakeRedisLayer.getInstance().getSession(cookie.getValue());

		if (session == null)
			throw new NotAuthorizedException("No valid session initialized");

		if (session.user() == null || session.user().length() == 0)
			throw new NotAuthorizedException("No valid session initialized");

		if (!session.user().equals(userId))
			throw new NotAuthorizedException("Invalid user : " + session.user());

		return session;
	}

	private static Session getUserSession(String cookieValue) {

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			var key = cookieValue;
			var val = jedis.get(key);

			if (val != null) {
				jedis.expire(key, RedisCache.ALIVE_TIME);
				var session = JSON.decode(val, Session.class);
				return session;
			}
		}

		return null;
	}
}