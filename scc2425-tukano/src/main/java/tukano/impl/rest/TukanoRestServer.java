package tukano.impl.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tukano.api.Result;
import tukano.api.User;
import tukano.clients.rest.RestShortsClient;
import tukano.clients.rest.RestUsersClient;
import tukano.impl.Token;
import utils.Args;
import utils.IP;
import utils.Props;

public class TukanoRestServer {
	private static final Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	private static final String SERVER_BASE_URI = "http://%s:%d/rest";
	public static final int PORT = 80;

	private Set<Class<?>> resources = new HashSet<>();
	public static String serverURI;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}

	public TukanoRestServer() {
		serverURI = String.format(SERVER_BASE_URI, IP.hostname(), PORT);
		loadResources();
		initializeDefaultValues();
		System.out.println(Props.get("STORAGE_ACCOUNT_NAME", "bruh"));
		System.out.println(Props.get("COSMOSDB_DATABASE", "bruh mas 2"));
	}

	private void loadResources() {
		resources.add(RestBlobsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(RestShortsResource.class);
	}

	protected void start() throws Exception {
		ResourceConfig config = new ResourceConfig();

		resources.forEach(config::register);

		JdkHttpServerFactory.createHttpServer(URI.create(serverURI.replace(IP.hostname(), "0.0.0.0")), config);


		Log.info(String.format("Tukano Server ready @ %s", serverURI));
	}

	public static void main(String[] args) throws Exception {
		Args.use(args);

		Props.loadProps("azurekeys-region.props");

		Token.setSecret(Args.valueOf("-secret", ""));

		TukanoRestServer server = new TukanoRestServer();
		server.start();

		

		Log.info("Default values initialized successfully.");
	}

	private static void initializeDefaultValues() {
		String baseUri = String.format(SERVER_BASE_URI, IP.hostname(), PORT);
		RestUsersClient usersClient = new RestUsersClient(baseUri);
		RestShortsClient shortsClient = new RestShortsClient(baseUri);

		usersClient.createUser(new User("defaultUser1", "password1", "user1@example.com", "Default User One"));
		usersClient.createUser(new User("defaultUser2", "password2", "user2@example.com", "Default User Two"));

		Result<tukano.api.Short> shortResult = shortsClient.createShort("defaultUser1", "password1");
		if (shortResult.isOK()) {
			Log.info("Default short created successfully for defaultUser1.");
		} else {
			Log.warning("Failed to create default short: " + shortResult.error());
		}

		shortsClient.follow("defaultUser2", "defaultUser1", true, "password2");

	}
}