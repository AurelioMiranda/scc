package tukano.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.User;
import utils.Props;

public class PostgreSQLLayer {
    private static final String DB_URL = Props.get("POSTGRE_URL", "jdbc:postgresql://c-scc-tukano-postgresqk-70068.fientyiqa2kzrw.postgres.cosmos.azure.com:5432/tukano?user=citus&password=abcde12345!&sslmode=require"); // Null connection probably because of this url??
    private static final String DB_USER = Props.get("POSTGRE_USER", "citus");
    private static final String DB_PASSWORD = Props.get("POSTGRE_PASS", "abcde12345!");
    private static Logger Log = Logger.getLogger(PostgreSQLLayer.class.getName());

    String url = "jdbc:postgresql://c-scc-tukano-postgresqk-70068.fientyiqa2kzrw.postgres.cosmos.azure.com:5432/tukano?user=citus&password=abcde12345!&sslmode=require";
    String username = "citus";
    String password = "abcde12345!";

    private static PostgreSQLLayer instance;
    private Connection connection;

    public static synchronized PostgreSQLLayer getInstance() {
        if (instance != null)
            return new PostgreSQLLayer();

        instance = new PostgreSQLLayer();
        return instance;
    }

    /* 
    private PostgreSQLLayer() {
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

    
    // Private constructor to initialize PostgreSQL connection
    private PostgreSQLLayer() {
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            
            // Establish the connection
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException | ClassNotFoundException e) {
            // Handle exceptions
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to PostgreSQL", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Should work, but getting null connection
    //TODO 
    //Generalize this method to work with Short class
    public <T> Result<T> insertOne(String tableName, T obj) {
        // INSERT a USER
        if(obj instanceof User user ){
            // Create the insert query
            // Each ? will have a corresponding value, these are just placeholders
            String insertQuery = "INSERT INTO " + tableName + " (userId, id, pwd, email, displayName) VALUES (?, ?, ?, ?, ?)";

            // THIS IS NOT WORKING BECAUSE THE CONNECTION IS NULL
            // ON A STANDALONE PROJECT IT WORKS FINE
            // AT THIS POINT, I DO NOT KNOW .-.
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                // Set the parameters based on the User object
                statement.setString(1, user.getUserId());       
                statement.setString(2, user.getId());            
                statement.setString(3, user.getPwd());           
                statement.setString(4, user.getEmail());         
                statement.setString(5, user.getDisplayName());   
                
                // Execute the query
                statement.executeUpdate();
                return Result.ok(obj);
            } catch (SQLException e) {
                e.printStackTrace();
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
        }
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    //TODO Rest of CRUD operations
    //View CosmosDBLayer for the methods to implement
}