package tukano.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;
import utils.Props;

public class PostgreSQLLayer {
    private static final String DB_URL = Props.get("POSTGRE_URL", "jdbc:postgresql://c-scc-tukano-postgresqk-70068.fientyiqa2kzrw.postgres.cosmos.azure.com:5432/tukano?user=citus&password=abcde12345!&sslmode=require"); // Null connection probably because of this url??
    private static final String DB_USER = Props.get("POSTGRE_USER", "citus");
    private static final String DB_PASSWORD = Props.get("POSTGRE_PASS", "abcde12345!");
    private static Logger Log = Logger.getLogger(PostgreSQLLayer.class.getName());

   public static final String TABLE_USERS = "users";
   public static final String TABLE_SHORTS = "shorts";

    private static PostgreSQLLayer instance;
    private Connection connection;

    public static synchronized PostgreSQLLayer getInstance() {
        if (instance != null)
            return new PostgreSQLLayer();

        instance = new PostgreSQLLayer();
        return instance;
    }
  
    // Private constructor to initialize PostgreSQL connection
    private PostgreSQLLayer() {
        try {
            // Load PostgreSQL driver (THIS IS WHY THE CONNECTION WAS NULL DO NOT REMOVE FROM HERE)
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

        // Insert a SHORT
        if(obj instanceof Short st){
            String insertQuery = "INSERT INTO " + tableName + " (shortId, id, ownerId, bloburl, timestamp, totalLikes) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                // Set the parameters based on the User object
                statement.setString(1, st.getShortId());       
                statement.setString(2, st.getId()); 
                statement.setString(3, st.getOwnerId());          
                statement.setString(4, st.getBlobUrl());           
                statement.setLong(5, st.getTimestamp());         
                statement.setInt(6, st.getTotalLikes());   
                
                // Execute the query
                statement.executeUpdate();
                return Result.ok(obj);
            } catch (SQLException e) {
                e.printStackTrace();

                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
        }

        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }

    //TODO Rest of CRUD operations
    //Missing: searchAll, 
    
    public <T> Result<T> getOne(String tableName, String id, Class<T> clazz){
        if(clazz == User.class){
            String getQuery = "SELECT id, userId, pwd, email, displayName FROM " + tableName + " WHERE id = ?";

            try (PreparedStatement statement = connection.prepareStatement(getQuery)) {
                // Set the id parameter on getQuery
                statement.setString(1, id);
                
                // Execute the query and get the result set object
                try (ResultSet resultSet = statement.executeQuery()) {
                    // Check if a result was returned
                    if (resultSet.next()) {
                        // Retrieve the data
                        User user = new User();
                        user.setUserId(resultSet.getString("userId"));
                        user.setId(resultSet.getString("id"));
                        user.setPwd(resultSet.getString("pwd"));
                        user.setEmail(resultSet.getString("email"));
                        user.setDisplayName(resultSet.getString("displayName"));
        
                        return (Result<T>) Result.ok(user);
                    } else {
                        // If no user is found, return error code
                        return Result.error(Result.ErrorCode.NOT_FOUND);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();

                return Result.error(Result.ErrorCode.CONFLICT);
            }
        }

        if(clazz == Short.class){
            String getQuery = "SELECT * FROM " + tableName + " WHERE id = ?";

            try (PreparedStatement statement = connection.prepareStatement(getQuery)) {
                // Set the id parameter on getQuery
                statement.setString(1, id);
                
                // Execute the query and get the result set object
                try (ResultSet resultSet = statement.executeQuery()) {
                    // Check if a result was returned
                    if (resultSet.next()) {
                        // Retrieve the data
                        Short st = new Short();
                        st.setId(resultSet.getString("id"));
                        st.setShortId(resultSet.getString("shortId"));
                        st.setOwnerId(resultSet.getString("ownerId"));
                        st.setTimestamp(resultSet.getLong("timestamp"));
                        st.setBlobUrl(resultSet.getString("blobUrl"));
                        st.setTotalLikes(resultSet.getInt("totalLikes"));
        
                        return (Result<T>) Result.ok(st);
                    } else {
                        // If no short is found, return error code
                        return Result.error(Result.ErrorCode.NOT_FOUND);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();

                return Result.error(Result.ErrorCode.CONFLICT);
            }
        }

        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }

    // Passes all validation but doesn't update the row in postgre for some reason
    public <T> Result<T> updateOne(String tableName, T obj) {
        if(obj instanceof User user){
            String updateQuery = "UPDATE " + tableName + " SET pwd = ?, email = ?, displayName = ? WHERE id = ?";
            try(PreparedStatement statement = connection.prepareStatement(updateQuery)){
                // Set the update data
                statement.setString(1, user.getPwd());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getDisplayName());

                // Set the id we are looking for
                statement.setString(4, user.getId());
                
                // Execute the update query
                if (statement.executeUpdate() > 0) {
                    // Rows updated, sucessfull

                    return (Result<T>) Result.ok(user);
                } else {
                    // No rows were updated, the user was not found

                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }
            }catch(SQLException e){
                e.printStackTrace();
                return Result.error(Result.ErrorCode.CONFLICT);
            }
        }
        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }

    public <T> Result<?> deleteOne(String tableName, T obj){
        if(obj instanceof User user){
            String deleteQuery = "DELETE FROM " + tableName + " WHERE id = ?";

            try(PreparedStatement statement = connection.prepareStatement(deleteQuery)){
                statement.setString(1, user.getId());

                if(statement.executeUpdate() > 0){

                    return Result.ok();
                }else{

                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }
            }catch(SQLException e){
                e.printStackTrace();

                return Result.error(Result.ErrorCode.CONFLICT);
            }
        }

        if(obj instanceof Short st){
            String deleteQuery = "DELETE FROM " + tableName + " WHERE id = ?";

            try(PreparedStatement statement = connection.prepareStatement(deleteQuery)){
                statement.setString(1, st.getId());

                if(statement.executeUpdate() > 0){

                    return Result.ok();
                }else{

                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }
            }catch(SQLException e){
                e.printStackTrace();

                return Result.error(Result.ErrorCode.CONFLICT);
            }
        }

        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }

    
}