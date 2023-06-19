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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.utils.StringUtils;

public class S3AbstractFileFactory implements AbstractFileFactory<S3File> {

	static Set<String> cachedBucketNames = new HashSet<>();
	
	protected final String bucketName;
	protected final int pageSize;
	protected S3Client s3;
	protected final Region region;
	protected final S3BucketFile bucket;
	protected final boolean create;
	
	public S3AbstractFileFactory(
			Region region, 
			String accessKey,
			String secretKey, 
			String bucketName,
			boolean create,
			int pageSize) throws URISyntaxException, IOException {
		this(region, accessKey, secretKey, bucketName, null, create, pageSize);
	}
	
	public S3AbstractFileFactory(
			Region region, 
			String accessKey,
			String secretKey, 
			String bucketName,
			String endpoint,
			boolean create,
			int pageSize) throws URISyntaxException, IOException {
		this.bucketName = bucketName;
		this.region = region;
		this.create = create;
		this.pageSize = pageSize;
		
		createClient(region, accessKey, secretKey, endpoint);
	
		this.bucket = new S3BucketFile(this, s3, verifyBucket(bucketName));
	}
	
	private void createClient(Region region, String accessKey, String secretKey, String endpoint)  {
		
		
		S3ClientBuilder builder = S3Client.builder()
				.region(region);
		
		if(StringUtils.isBlank(accessKey)) {
			builder = builder.credentialsProvider(InstanceProfileCredentialsProvider.builder().build());
		} else {
				builder = builder.credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
				
					@Override
					public String secretAccessKey() {
						return secretKey;
					}
					
					@Override
					public String accessKeyId() {
						return accessKey;
					}
			}));
		}
		
		if(StringUtils.isNotBlank(endpoint)) {
			try {
				builder = builder.endpointOverride(new URI(endpoint));
			} catch (URISyntaxException e) {
				throw new UncheckedIOException(new IOException(endpoint + " is an invalid URI"));
			}
		}
		
		this.s3 = builder.build();
	}

	public S3AbstractFileFactory(
			Region region, 
			String bucketName,
			boolean create,
			int pageSize) throws URISyntaxException, IOException {
		
		this.bucketName = bucketName;
		this.region = region;
		this.create = create;
		this.pageSize = pageSize;
		
		S3ClientBuilder builder = S3Client.builder()
				.region(region)
				.credentialsProvider(InstanceProfileCredentialsProvider.builder().build());
		
		this.s3 = builder.build();
		
		
		this.bucket = new S3BucketFile(this, s3, verifyBucket(bucketName));
	}
	
	private Bucket verifyBucket(String bucketName) throws IOException {
		if(!cachedBucketNames.contains(bucketName)) {
			try { 
				GetBucketLocationRequest request = GetBucketLocationRequest.builder()
						.bucket(bucketName)
						.build();
				s3.getBucketLocation(request);
				cachedBucketNames.add(bucketName);
			} catch (AwsServiceException | SdkClientException e) {
				if(create) {
					CreateBucketRequest req = CreateBucketRequest.builder().bucket(bucketName).build();
					s3.createBucket(req);
					cachedBucketNames.add(bucketName);
				} else {
					throw new FileNotFoundException(bucketName + " is not an existing bucket name!");
				}
			}
		}
		
		for(Bucket bucket : s3.listBuckets().buckets()) {
			if(bucket.name().equals(bucketName)) {
				return bucket;
			}
		}
		
		throw new FileNotFoundException("");
	}

	@Override
	public S3File getFile(String path) throws PermissionDeniedException, IOException {
		if(StringUtils.isBlank(FileUtils.checkStartsWithNoSlash(path))) {
			return bucket;
		}
		if(path.equals(bucket.getAbsolutePath())) {
			return bucket;
		}
		return resolveFile(bucket, path);
	}

	protected S3File resolveFile(S3BucketFile bucket, String child) throws IOException, PermissionDeniedException {
		
		try {
			String resolvePath;
			if(child.startsWith(bucket.getAbsolutePath())) {
				resolvePath = FileUtils.checkStartsWithNoSlash(child.substring(bucket.getAbsolutePath().length()));
			} else {
				resolvePath = FileUtils.checkEndsWithNoSlash(child);
			}
			
			if(StringUtils.isBlank(resolvePath)) {
				return bucket;
			}
			
            ListObjectsV2Request listObjects = ListObjectsV2Request
                .builder()
                .bucket(bucket.getName())
                .maxKeys(1)
                .delimiter("/")
                .prefix(resolvePath)
                .build();

            ListObjectsV2Response res = s3.listObjectsV2(listObjects);
            List<S3Object> objects = res.contents();
            
            for (S3Object object : objects) {
            	String key = FileUtils.checkEndsWithNoSlash(object.key());
            	if(key.equals(resolvePath)) {
            		return new S3AbstractFile(this, s3, bucket, object);
            	}
             }
            
            for(CommonPrefix prefix : res.commonPrefixes()) {
            	String key = FileUtils.checkEndsWithNoSlash(prefix.prefix());
            	if(key.equals(resolvePath)) {
            		return new S3AbstractFolder(this, bucket, prefix);
            	}
            }

            return new S3AbstractFile(this, s3, bucket, resolvePath);
            
        } catch (S3Exception e) {
           throw new IOException(e.awsErrorDetails().errorMessage());
        }
	}

	protected List<AbstractFile> resolveBuckets() throws IOException, PermissionDeniedException {
		
		List<AbstractFile> results = new ArrayList<>();
		
		for(Bucket bucket : s3.listBuckets().buckets()) {
			results.add(new S3BucketFile(this, s3, bucket));
		}
		
		return results;
	}
	protected List<AbstractFile> resolveChildren(S3BucketFile bucket, String path) throws IOException, PermissionDeniedException {
		
	
		String resolvePath;
		if(path.startsWith(bucket.getAbsolutePath())) {
			resolvePath = FileUtils.checkStartsWithNoSlash(path.substring(bucket.getAbsolutePath().length()));
		} else {
			resolvePath = path;
		}
		if(StringUtils.isNotBlank(resolvePath)) {
			resolvePath = FileUtils.checkEndsWithSlash(resolvePath);
		}
	
		return listBucketObjects(bucket, resolvePath);

	}
	
	public List<AbstractFile> listBucketObjects(S3BucketFile bucket, String path) throws IOException {
        
		try {
        
			List<AbstractFile> results = new ArrayList<>();
			
			ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket.getName())
                .prefix(path)
                .delimiter("/")
                .maxKeys(1000)
                .build();

           ListObjectsV2Iterable listRes = s3.listObjectsV2Paginator(listReq);
            
           listRes.forEach((res)->{ 
        	   
        	   for(S3Object obj : res.contents()) {
        		   String name = sanitizeName(obj.key(), path);
        		   if(StringUtils.isBlank(name)) {
        			   continue;
        		   }
        		   results.add(new S3AbstractFile(this, s3, bucket, obj));
        	   }

        	   for(CommonPrefix prefix : res.commonPrefixes()) {
        		   String name = sanitizeName(prefix.prefix(), path);
        		   if(StringUtils.isBlank(name)) {
        			   continue;
        		   }
        		   results.add(new S3AbstractFile(this, s3, bucket, prefix.prefix()));
        	   }
           });
    
           return results;

		} catch (S3Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

	private String sanitizeName(String key, String path) {
		String name = FileUtils.checkStartsWithNoSlash(key.substring(path.length()));
		return name;
	}

}
