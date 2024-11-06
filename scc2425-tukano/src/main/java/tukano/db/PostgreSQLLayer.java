import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import tukano.api.Result;
import tukano.api.User;
import utils.Props;

public class PostgreSQLLayer {
    private static final String DB_URL = Props.get("POSTGRE_URL", ""); // Null connection probably because of this url??
    private static final String DB_USER = Props.get("POSTGRE_USER", "");
    private static final String DB_PASSWORD = Props.get("POSTGRE_PASS", "");
    private static Logger Log = Logger.getLogger(PostgreSQLLayer.class.getName());

    private static PostgreSQLLayer instance;
    private Connection connection;

    public static synchronized PostgreSQLLayer getInstance() {
        if (instance != null)
            return instance;

        instance = new PostgreSQLLayer();
        return instance;
    }

    private PostgreSQLLayer() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
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
        if(obj instanceof User ){
            User user = (User) obj;

            // Create the insert query
            // Each ? will have a corresponding value, these are just placeholders
            String insertQuery = "INSERT INTO " + tableName + " (userId, id, pwd, email, displayName) VALUES (?, ?, ?, ?, ?)";
            
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
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    //TODO Rest of CRUD operations
    //View CosmosDBLayer for the methods to implement
}