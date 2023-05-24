/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.synergy.s3;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.Multipart;
import com.sshtools.common.sftp.MultipartTransfer;
import com.sshtools.common.sftp.OpenFile;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.utils.StringUtils;

public class S3AbstractFile implements AbstractFile {

	S3Client s3;
	String bucketName;
	String path;
	S3Object file;
	S3AbstractFileFactory factory;
	
	S3AbstractFile(S3AbstractFileFactory factory, 
			S3Client s3, String bucketName, String path) {
		this.factory = factory;
		this.s3 = s3;
		this.bucketName = bucketName;
		this.path = path;
	}
	
	S3AbstractFile(S3AbstractFileFactory factory, S3Client s3, String bucketName, S3Object file) {
		this.factory = factory;
		this.s3 = s3;
		this.bucketName = bucketName;
		this.path = file.key();
		this.file = file;
	}
	
	@Override
	public String getName() {
		return FileUtils.lastPathElement(FileUtils.checkEndsWithNoSlash(path));
	}

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();

        return s3.getObject(getObjectRequest);
	}

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		return Objects.nonNull(file);
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		try {
            ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket(bucketName)
                .prefix(path)
                .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();
            List<AbstractFile> results = new ArrayList<>();
            String currentDirectory = StringUtils.isNotBlank(path) ? FileUtils.checkEndsWithSlash(path) : "";
            for (S3Object object : objects) {
            	String key = object.key().replaceFirst(currentDirectory, "");
            	String[] pathElements = key.split("/");
            	
            	if(StringUtils.isNotBlank(key) && pathElements.length == 1) {
            		results.add(new S3AbstractFile(factory, s3, bucketName, object));
            	} 
             }

            return results;
            
        } catch (S3Exception e) {
           throw new IOException(e.awsErrorDetails().errorMessage());
        }
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return FileUtils.checkEndsWithNoSlash(path);
	}

	@Override
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		if(StringUtils.isBlank(path)) {
			throw new FileNotFoundException();
		}
		String parentPath = FileUtils.getParentPath(path);
		return factory.getFile(parentPath);
	}

	@Override
	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return StringUtils.isBlank(path) || path.endsWith("/");
	}

	@Override
	public boolean isFile() throws IOException, PermissionDeniedException {
		return !isDirectory();
	}

	@Override
	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		return new S3OutputStream(s3, bucketName, path);
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
		
		this.path = FileUtils.checkEndsWithSlash(path);
		PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName).key(path).build();
 
		s3.putObject(request, RequestBody.empty());
		
		S3Waiter waiter = s3.waiter();
        HeadObjectRequest requestWait = HeadObjectRequest.builder()
                        .bucket(bucketName).key(path).build();
         
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
			throw new IOException(path + " is a directory and cannot be copied using copyFrom");
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
			throw new IOException(path + " is a directory and cannot be moved using moveTo");
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
                .bucket(bucketName)
                .key(path)
                .build();

        s3.deleteObject(deleteObjectRequest);

		
		return false;
	}

	@Override
	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		SftpFileAttributes attrs = new SftpFileAttributes(
				isDirectory() ? SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY : SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR,
						"UTF-8");
		attrs.setSize(new UnsignedInteger64(length()));
		UnsignedInteger64 t = new UnsignedInteger64(lastModified());
		attrs.setTimes(t, t);
		return attrs;
	}

	@Override
	public void refresh()  {
		
		try {
			ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket(bucketName)
                .prefix(path)
                .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();
            
            for (S3Object object : objects) {
            	String key = FileUtils.checkEndsWithNoSlash(object.key());
            	if(key.equals(FileUtils.checkEndsWithNoSlash(path))) {
            		this.file = object;
            		return;
            	}
            }
 
        } catch (S3Exception e) {
           // TODO log?
        }
		
		this.file = null;

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
			throw new IOException(path + " already exists!");
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
		throw new UnsupportedOperationException();
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
		
		try {
			String resolvePath = 
					(StringUtils.isBlank(path) ? "" : FileUtils.checkEndsWithSlash(path)) + FileUtils.checkEndsWithNoSlash(child);
            ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket(bucketName)
                .prefix(resolvePath)
                .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();
            
            for (S3Object object : objects) {
            	String key = FileUtils.checkEndsWithNoSlash(object.key());
            	if(key.equals(resolvePath)) {
            		return new S3AbstractFile(factory, s3, bucketName, object);
            	}
             }

            return new S3AbstractFile(factory, s3, bucketName, resolvePath);
            
        } catch (S3Exception e) {
           throw new IOException(e.awsErrorDetails().errorMessage());
        }
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return factory;
	}
	
	public boolean supportsMultipartTransfers() {
		return true;
	}

	public MultipartTransfer startMultipartUpload(Collection<Multipart> multiparts) throws IOException, PermissionDeniedException {
		
		CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();

        CreateMultipartUploadResponse response = s3.createMultipartUpload(createMultipartUploadRequest);
        return new S3MultipartTransfer(path, this, response.uploadId(), multiparts, exists());
	}
	
	class S3MultipartTransfer implements MultipartTransfer {

		String uploadId;
		String uuid = UUID.randomUUID().toString();
		List<S3OpenFilePart> parts = new ArrayList<>();
//		ExecutorService executor;
		PipedOutputStream out;
		PipedInputStream in;
		int partNumber = 1;
		Collection<Multipart> multiparts;
		Collection<Multipart> completedParts = new ArrayList<>();
		String path;
		boolean exists;
		AbstractFile file;
		boolean cancelled;
		
		public S3MultipartTransfer(String path, AbstractFile file, String uploadId, Collection<Multipart> multiparts, boolean exists) {
			this.uploadId = uploadId;
			this.multiparts = multiparts;
			this.file = file;
			this.path = path;
//			executor = Executors.newCachedThreadPool();
		}
		
		public Collection<Multipart> getParts() {
			return multiparts;
		}
		
		@Override
		public String getUploadId() {
			return uploadId;
		}
		
		@Override
		public String getUuid() {
			return uuid;
		}
		
		public Multipart getPart(String id) {
			for(Multipart multipart : multiparts) {
				if(multipart.getPartIdentifier().equals(id)) {
					return multipart;
				}
			}
			throw new IllegalStateException("No part for id " + id);
		}
		
		public synchronized int nextPartNumber() {
			return partNumber++;
		}
		
		@Override
		public OpenFile openPart(Multipart part) throws IOException, PermissionDeniedException {
			
			S3OpenFilePart partThread = new S3OpenFilePart(part, this);
			parts.add(partThread);
			return partThread;
		}
		
		public void closePart(Multipart part) throws IOException {
			completedParts.add(part);
			
			if(Log.isErrorEnabled()) {
				Log.info("REMOVEME: Closing part {} {}/{} with id {} {} {}", part.getPartIdentifier(),
						bucketName, path, getUploadId(), completedParts.size(), multiparts.size());
			}
			
			if(completedParts.size() == multiparts.size()) {
				if(cancelled) {
					cancelUpload();
				} else {
					combineParts();
				}
			}
		}
		
		private void cancelUpload() {
			try {
				if(Log.isErrorEnabled()) {
					Log.info("REMOVEME: Cancelling upload {}/{} with id {}", bucketName, path, getUploadId());
				}
				s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
				      .bucket(bucketName)
				      .key(path)
				      .uploadId(getUploadId()).build());
			} catch (AwsServiceException | SdkClientException e) {
				Log.error("Failed to cancel multpart upload {}", getUploadId(), e);
			}
		}

		public void cancel() {
			this.cancelled = true;
		}
		
		@Override
		public void combineParts() throws IOException {
			
//			executor.shutdown();
//			try {
//				executor.awaitTermination(60, TimeUnit.SECONDS);
//			} catch (InterruptedException e) {
//				throw new IllegalStateException(e.getMessage(), e);
//			}
			
			List<CompletedPart> completed = new ArrayList<>();
			for(S3OpenFilePart part : parts) {
				completed.addAll(part.getCompletedParts());
			}
			
			if(Log.isInfoEnabled()) {
				Log.info("REMOVEME: Combining {} parts into final file {} for upload {}", completed.size(), path, uploadId);
			}
			
			try {
				CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
				        .parts(completed)
				        .build();
				
				CompleteMultipartUploadRequest completeMultipartUploadRequest =
				        CompleteMultipartUploadRequest.builder()
				                .bucket(bucketName)
				                .key(path)
				                .uploadId(uploadId)
				                .multipartUpload(completedMultipartUpload)
				                .build();

				s3.completeMultipartUpload(completeMultipartUploadRequest);
				
				if(Log.isInfoEnabled()) {
					Log.info("REMOVEME: Completed multipart upload of file {} for upload {}", parts.size(), path, uploadId);
				}
			} catch (AwsServiceException | SdkClientException e) {
				Log.error("REMOVEME: Captured error attempting to combine multipart file {} upload {}", path, uploadId);
				cancelUpload();
				throw e;
			}
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public boolean getExists() {
			return exists;
		}

		public AbstractFile getFile() {
			return file;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}
	}
	
	class S3OpenFilePart implements OpenFile {

		Multipart part;
		S3MultipartTransfer transfer;

		final static int BUFFER_SIZE = 5 * 1024 * 1024;
		byte[] buffer = new byte[BUFFER_SIZE];
		
		long pointer;
		long transfered;
		byte[] handle;
		int bufferPointer = 0;
		
		List<CompletedPart> completedParts = new ArrayList<>();
		final int startPartNumber;
		int currentPartNumber;
		int totalParts;
		
		S3OpenFilePart(Multipart part, S3MultipartTransfer transfer) throws UnsupportedEncodingException {
			this.part = part;
			this.transfer = transfer;
			this.handle = UUID.randomUUID().toString().getBytes("UTF-8");
			this.pointer =  part.getStartPosition().longValue();
			this.transfered = 0L;
			this.totalParts = (int) ((part.getLength().longValue() / BUFFER_SIZE));
			if(part.getLength().longValue() % BUFFER_SIZE != 0) {
				this.totalParts++;
			}
			this.currentPartNumber = this.startPartNumber = 
					((int) part.getStartPosition().longValue() /  BUFFER_SIZE) + 1;

			if(Log.isInfoEnabled()) {
				Log.info("REMOVEME: Part {} starts at position {} with a length of {} and starting part number {} upload {}", 
						part.getPartIdentifier(),
						pointer,
						part.getLength().longValue(),
						startPartNumber, transfer.getUploadId());
			}
		}
		
		public Collection<CompletedPart> getCompletedParts() {
			return completedParts;
		}

		@Override
		public AbstractFile getFile() {
			return S3AbstractFile.this;
		}

		@Override
		public UnsignedInteger32 getFlags() {
			return new UnsignedInteger32(AbstractFileSystem.OPEN_WRITE);
		}

		@Override
		public boolean isTextMode() {
			return false;
		}

		@Override
		public long getFilePointer() throws IOException {
			return pointer;
		}

		@Override
		public void seek(long longValue) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int read(byte[] buf, int start, int numBytesToRead) throws IOException, PermissionDeniedException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void write(byte[] data, int off, int len) throws IOException, PermissionDeniedException {
			
			if(transfer.isCancelled()) {
				throw new EOFException();
			}
			
			if(transfered + len > part.getLength().longValue()) {
				Log.error("REMOVEME: Upload bounds error for {}", transfer.getUploadId());
				throw new PermissionDeniedException("Multipart upload bounds error! Client uploaded more data than initially reported");
			}
			
			int processed = 0;
			
			while(processed < len) {
				int toProcess = Math.min(buffer.length - bufferPointer, len - processed);
				System.arraycopy(data, off, buffer, bufferPointer, toProcess);
				bufferPointer += toProcess;
				
				if(bufferPointer == buffer.length || transfered == part.getLength().longValue()) {
				
					int i = 0;
					while(true) {
						try {
						
							if(Log.isInfoEnabled()) {
								Log.info("REMOVEME: Uploading {} block number {} attempt {} for upload {}", 
										part.getPartIdentifier(), currentPartNumber, i, transfer.getUploadId());
							}
							UploadPartRequest uploadPartRequest1 = UploadPartRequest.builder()
							        .bucket(bucketName)
							        .key(path)
							        .uploadId(transfer.getUploadId())
							        .partNumber(currentPartNumber).build();
							
							 String etag1 = s3.uploadPart(uploadPartRequest1, 
							    		RequestBody.fromByteBuffer(ByteBuffer.wrap(buffer, 0, bufferPointer))).eTag();
							 
							 completedParts.add(CompletedPart.builder().partNumber(currentPartNumber).eTag(etag1).build());
							 
							 currentPartNumber++;
							 bufferPointer = 0;
							 
							 break;
						} catch (AwsServiceException | SdkClientException e) {
							Log.error("REMOVEME: Failed to upload block {} of part {}/{} for upload {}", 
									currentPartNumber, part.getTargetFile().getName(), 
									part.getPartIdentifier(), transfer.getUploadId(), e);
							
							if(++i > 3) {
								if(Log.isInfoEnabled()) {
									Log.info("REMOVEME: Marking upload {} as failed", 
											transfer.getUploadId());
								}
								transfer.cancel();
								throw e;
							}
						} catch(Throwable e) {
							if(Log.isInfoEnabled()) {
								Log.info("REMOVEME: Marking upload {} as failed due to unknown exception", 
										transfer.getUploadId(), e);
							}
							
							transfer.cancel();
							throw new IOException(e.getMessage(), e);
						}
					}
				}
				
				pointer += toProcess;
				transfered += toProcess;
				processed += toProcess;
				
			}

		}

		@Override
		public void close() throws IOException {
			if(totalParts != completedParts.size()) {
				Log.info("REMOVEME: Marking upload {} as failed because completed parts {} does not equal expected total parts {}", 
						transfer.getUploadId(), completedParts.size(), totalParts);
				transfer.cancel();
			}
			
			transfer.closePart(part);
				
		}

		@Override
		public void processEvent(Event evt) {
			evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE, part.getTargetFile());
		}

		@Override
		public byte[] getHandle() {
			return handle;
		}

	}
	
}
