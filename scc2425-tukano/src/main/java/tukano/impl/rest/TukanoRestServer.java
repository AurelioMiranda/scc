package tukano.impl.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.core.Application;
import tukano.impl.Token;
import utils.Args;
import utils.IP;
import utils.Props;

public class TukanoRestServer extends Application {
	final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	private static final String SERVER_BASE_URI = "http://%s:%s/rest";
	public static final int PORT = 8080;

	static final String INETADDR_ANY = "0.0.0.0";
	public static String serverURI;

	private Set<Class<?>> resources = new HashSet<>();
	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}

	public TukanoRestServer() {
		serverURI = String.format(SERVER_BASE_URI, IP.hostname(), PORT);
		loadResources();
		Props.loadProps("azurekeys-region.props");
	}

	private void loadResources() {
		resources.add(RestBlobsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(RestShortsResource.class);
	}

	protected void start() throws Exception {
		ResourceConfig config = new ResourceConfig();

		resources.forEach(config::register);


		Log.info(String.format("Tukano Server ready @ %s", serverURI));
	}

	public static void main(String[] args) throws Exception {
		Args.use(args);

		Props.loadProps("azurekeys-region.props");
		Log.info("###################################################################################################################");
		Log.info(Props.get("BLOB_CONTAINER_NAME", "not workingz"));
		Token.setSecret(Args.valueOf("-secret", ""));
		// Props.load( Args.valueOf("-props", "").split(","));
		new TukanoRestServer().start();
	}

	public Set<Class<?>> getClasses() {
		return resources;
	}
}