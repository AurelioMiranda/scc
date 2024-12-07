package tukano.srv;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

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
import tukano.api.Result;
import tukano.api.User;
import tukano.impl.JavaUsers;
import tukano.srv.auth.RequestCookies;
import utils.JSON;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Path(Authentication.PATH)
public class Authentication {
	static final String PATH = "login";
	static final String USER = "username";
	static final String PWD = "password";
	static final String COOKIE_KEY = "scc:session";
	static final String LOGIN_PAGE = "login.html";
	private static final int MAX_COOKIE_AGE = 3600;
	static final String REDIRECT_TO_AFTER_LOGIN = "ctrl/login";

	private static final JedisPool jedisPool = new JedisPool("redis", 6379);

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

			// Save to cache
			try (Jedis jedis = jedisPool.getResource()) {
				String key = "session:" + uid;
				String value = JSON.encode(new Session(uid, user));
				jedis.set(key, value);
				jedis.expire(key, MAX_COOKIE_AGE);
			}

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

		// Retrieve from cache
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionData = jedis.get("session:" + cookie.getValue());

			if (sessionData == null)
				throw new NotAuthorizedException("No valid session initialized");

			Session session = JSON.decode(sessionData, Session.class);

			if (session.user() == null || session.user().isEmpty())
				throw new NotAuthorizedException("No valid session initialized");

			if (!session.user().equals(userId))
				throw new NotAuthorizedException("Invalid user: " + session.user());

			return session;
		}
	}
}