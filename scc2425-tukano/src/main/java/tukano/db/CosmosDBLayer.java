package tukano.db;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import tukano.impl.JavaUsers;
import utils.Props;;

public class CosmosDBLayer {
	//private static final String CONNECTION_URL = "https://scctukanocosmos70068.documents.azure.com:443/";
	private static final String CONNECTION_URL = Props.get("COSMOSDB_URL", "");
	//private static final String DB_KEY = "VByJA8a4Jp8QrnhLaVDhkHIDXOWWBcsUltiS12eLEe35NofjmVjKGajSXOF9iFJ17SZY2qhwEhMhACDbHpYZ1A==";
	private static final String DB_KEY = Props.get("COSMOSDB_KEY", "");
	//private static final String DB_NAME = "Tukano"; // USER db name
	private static final String DB_NAME = Props.get("COSMOSDB_DATABASE_NAME", "");
	public static final String CONTAINER_USERS = "Users";
	public static final String CONTAINER_SHORTS = "Shorts";
	public static final String CONTAINER_FOLLOWING = "Following";
	public static final String CONTAINER_LIKES = "Likes";
	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private static CosmosDBLayer instance;

	public static synchronized CosmosDBLayer getInstance() {
		if (instance != null)
			return instance;

		CosmosClient client = new CosmosClientBuilder()
				.endpoint(CONNECTION_URL)
				.key(DB_KEY)
				// .directMode()
				.gatewayMode()
				// replace by .directMode() for better performance
				.consistencyLevel(ConsistencyLevel.SESSION)
				.connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true)
				.buildClient();
		instance = new CosmosDBLayer(client);
		return instance;

	}

	private CosmosClient client;
	private CosmosDatabase db;

	public CosmosDBLayer(CosmosClient client) {
		this.client = client;
	}

	private synchronized void init() {
		if (db == null) {
			db = client.getDatabase(DB_NAME);
		}
	}

	public void close() {
		client.close();
	}

	public <T> Result<T> getOne(String containerName, String id, Class<T> clazz) {
		return tryCatch(() -> getContainer(containerName).readItem(id, new PartitionKey(id), clazz).getItem());
	}

	public <T> Result<?> deleteOne(String containerName, T obj) {
		return tryCatch(() -> getContainer(containerName).deleteItem(obj, new CosmosItemRequestOptions()).getItem());
	}

	public <T> Result<T> updateOne(String containerName, T obj) {
		return tryCatch(() -> getContainer(containerName).upsertItem(obj).getItem());
	}

	public <T> Result<T> insertOne(String containerName, T obj) {
		return tryCatch(() -> getContainer(containerName).createItem(obj).getItem());
	}

	public <T> Result<List<T>> query(String containerName, Class<T> clazz, String queryStr) {
		return tryCatch(() -> {
			var res = getContainer(containerName).queryItems(queryStr, new CosmosQueryRequestOptions(), clazz);
			return res.stream().toList();
		});
	}

	private CosmosContainer getContainer(String containerName) {
		init();
		return db.getContainer(containerName);
	}

	<T> Result<T> tryCatch(Supplier<T> supplierFunc) {
		try {
			init();
			return Result.ok(supplierFunc.get());
		} catch (CosmosException ce) {
			return Result.error(errorCodeFromStatus(ce.getStatusCode()));
		} catch (Exception x) {
			x.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	static Result.ErrorCode errorCodeFromStatus(int status) {
		return switch (status) {
			case 200 -> ErrorCode.OK;
			case 404 -> ErrorCode.NOT_FOUND;
			case 409 -> ErrorCode.CONFLICT;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}
}
