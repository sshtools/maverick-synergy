package com.sshtools.synergy.s3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.FileUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.utils.StringUtils;

public class S3AbstractFile implements S3File {

	S3Client s3;
	String key;
	S3Object file;
	S3BucketFile bucket;
	S3AbstractFileFactory factory;
	
	S3AbstractFile(S3AbstractFileFactory factory, 
			S3Client s3, S3BucketFile bucket, String path) {
		this.factory = factory;
		this.s3 = s3;
		this.bucket= bucket;
		this.key = path;
	}
	
	S3AbstractFile(S3AbstractFileFactory factory, S3Client s3, S3BucketFile bucket, S3Object file) {
		this.factory = factory;
		this.s3 = s3;
		this.bucket = bucket;
		this.key = file.key();
		this.file = file;
	}
	
	@Override
	public String getName() {
		return FileUtils.lastPathElement(FileUtils.checkEndsWithNoSlash(key));
	}
	
	public S3Client getClient() {
		return factory.s3;
	}
	
	public String getKey() {
		return key;
	}
	
	public Bucket getBucket() {
		return bucket.bucket;
	}

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket.getName())
                .key(key)
                .build();

        return s3.getObject(getObjectRequest);
	}

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		if(Objects.isNull(file) && key.endsWith("/")) {
			return true;
		}
		return Objects.nonNull(file);
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return factory.resolveChildren(bucket, getAbsolutePath());
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return bucket.getAbsolutePath() + key;
	}

	@Override
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		if(StringUtils.isBlank(key)) {
			return bucket;
		}
		String parentPath = FileUtils.getParentPath(key);
		return factory.getFile(parentPath);
	}

	@Override
	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return StringUtils.isBlank(key) || key.endsWith("/");
	}

	@Override
	public boolean isFile() throws IOException, PermissionDeniedException {
		return !isDirectory();
	}

	@Override
	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		return new S3OutputStream(s3, bucket.getName(), key);
	}

	@Override
	public boolean isHidden() throws IOException, PermissionDeniedException {
		return false;
	}

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		
		if(exists()) {
			return false;
		}
		
		this.key = FileUtils.checkEndsWithSlash(key);
		PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket.getName()).key(key).build();
 
		s3.putObject(request, RequestBody.empty());
		
		S3Waiter waiter = s3.waiter();
        HeadObjectRequest requestWait = HeadObjectRequest.builder()
                        .bucket(bucket.getName()).key(key).build();
         
        WaiterResponse<HeadObjectResponse> waiterResponse = waiter.waitUntilObjectExists(requestWait);
         
        if(waiterResponse.matched().response().isPresent()) {
        	return true;
        }
        
		return false;
	}

	@Override
	public boolean isReadable() throws IOException, PermissionDeniedException {
		return exists();
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {
		
		if(!src.exists()) {
			throw new FileNotFoundException();
		}
		
		if(src.isDirectory()) {
			throw new IOException(key + " is a directory and cannot be copied using copyFrom");
		}
		src.getInputStream().transferTo(getOutputStream());
		refresh();
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		if(isDirectory()) {
			throw new IOException(key + " is a directory and cannot be moved using moveTo");
		}
		target.copyFrom(this);
		delete(false);
		refresh();
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket.getName())
                .key(key)
                .build();

        s3.deleteObject(deleteObjectRequest);

		
		return true;
	}

	@Override
	public void refresh()  {
		
	}

	@Override
	public long lastModified() throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		if(Objects.nonNull(file)) {
			return file.lastModified().toEpochMilli();
		}
		return 0L;
	}

	@Override
	public long length() throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		return Objects.nonNull(file) ? file.size() : 0L;
	}

	@Override
	public boolean isWritable() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		
		if(exists()) {
			throw new IOException(key + " already exists!");
		}
		
		try {
			truncate();
			return true;
		} catch(IOException e) { }
        
		return false;
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		
		getOutputStream().close();
		refresh();

	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		/**
		 * We purposely ignore this because errors can cause problems with clients
		 */
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return getAbsolutePath();
	}

	@Override
	public boolean supportsRandomAccess() {
		return false;
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		
		if(child.startsWith("/")) {
			return factory.resolveFile(bucket, FileUtils.checkStartsWithNoSlash(child));
		}
		return factory.resolveFile(bucket, FileUtils.checkEndsWithSlash(getAbsolutePath()) + child);
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return factory;
	}
	
}
