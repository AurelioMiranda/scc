package tukano.impl.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;

public class NewFilesystemStorage {
    private static final String BLOBS_CONTAINER_NAME = "shorts";
    private static final String BLOBS_CONTAINER_CONNEC_STRING = "sdwadwadwdawd";
    private final BlobContainerClient containerClient;

    public NewFilesystemStorage(){
        containerClient = new BlobContainerClientBuilder()
                                .connectionString(BLOBS_CONTAINER_CONNEC_STRING)
                                .containerName(BLOBS_CONTAINER_NAME)
                                .buildClient();
    }

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

    /* 
    * filename -> the name of the blob/video we want to download
    * downloadPath -> the path where we will save the video in our machine
    */
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

    // TODO: delete video, change methods in javablob to call these new methods
    // change just to upload, download, delete get rid of the "video" part
}
