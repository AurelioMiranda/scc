package tukano.impl.storage;

import static tukano.api.Result.error;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.CONFLICT;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;

import java.util.function.Consumer;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;
import utils.Props;

public class FilesystemStorage implements BlobStorage{
    private static final String BLOBS_CONTAINER_NAME = Props.get("BLOB_CONTAINER_NAME", "");
    //private static final String BLOBS_CONTAINER_NAME = "shorts";
    private static final String BLOBS_CONTAINER_CONNECTION_STRING = Props.get("STORAGE_ACCOUNT_CONNECTION", "");
    //private static final String BLOBS_CONTAINER_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=scctukano70068;AccountKey=3HbFmeMeRintQiY47zzBdY/bWc/s2SvvzA9h+fClVf92W0c0E3QQ8GP/ve433Jozp2tQ0WGtrpSv+ASt3kZN0A==;EndpointSuffix=core.windows.net";
    private final BlobContainerClient containerClient;

    public FilesystemStorage(){
        containerClient = new BlobContainerClientBuilder()
                                .connectionString(BLOBS_CONTAINER_CONNECTION_STRING)
                                .containerName(BLOBS_CONTAINER_NAME)
                                .buildClient();
    }
    
    @Override
	public Result<Void> write(String path, byte[] bytes) {
		if (path == null || bytes == null){
			return error(BAD_REQUEST);
        }

        // Create a blob for the video to upload
        // getBlobClient(name)
		BlobClient blob = containerClient.getBlobClient(path);
        
        // Create Binary Data 
        BinaryData binary = BinaryData.fromBytes(bytes);

        // Upload the blob
        blob.upload(binary, true);
		System.out.println( "File uploaded : " + path);
		return ok();
	}
    
	@Override
	public Result<byte[]> read(String path) {
		if (path == null)
			return error(BAD_REQUEST);
        
        BlobClient blobClient = containerClient.getBlobClient(path);
        
        if(!blobClient.exists()){
            return error(CONFLICT);
        }
        
        var bytes = blobClient.downloadContent().toBytes();
		return bytes != null ? ok( bytes ) : error( INTERNAL_ERROR );
	}

    @Override
	public Result<Void> read(String path, Consumer<byte[]> sink) {
		if (path == null || sink == null)
			return error(BAD_REQUEST);
        
        BlobClient blobClient = containerClient.getBlobClient(path);
        
        if(!blobClient.exists()){
            return error(CONFLICT);
        }

        sink.accept(blobClient.downloadContent().toBytes());
		return ok();
	}

    @Override
	public Result<Void> delete(String path) {
		if (path == null){
		    return error(BAD_REQUEST);
        }

		BlobClient blobClient = containerClient.getBlobClient(path);
			
        if(!blobClient.deleteIfExists()){
            return error(INTERNAL_ERROR);
        }

        return ok();
	}

    /* 
    public void uploadVideo(String filename){
        try {
            // Convert the video to upload to a BinaryData
            // To upload a blob the Azure API is expecting data in the BinaryData format
            BinaryData data = BinaryData.fromFile(Path.of(filename));

            // Create a blob for the video to upload
            // getBlobClient(name)
		    BlobClient blob = containerClient.getBlobClient(filename);

            // Upload the blob
			blob.upload(data);
			
			System.out.println( "File uploaded : " + filename);
        } catch (Exception e) {
            System.err.println("Error uploading video: " + e.getMessage());
            System.err.println(e.getStackTrace().toString());
        }
    }
    */

    /* 
    * filename -> the name of the blob/video we want to download
    * downloadPath -> the path where we will save the video in our machine
    */
    /*
    public void downloadVideo(String filename, String downloadPath) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);

            // Download the blob present in Azure to a file
            blobClient.downloadToFile(downloadPath, true);

            System.out.println("File downloaded to: " + downloadPath);
        } catch (BlobStorageException e) {
            System.err.println("Blob Storage Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getStackTrace().toString());
        }
    }
    */
}
