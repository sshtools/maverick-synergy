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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.utils.StringUtils;

public class S3AbstractFileFactory implements AbstractFileFactory<S3AbstractFile> {

	static Set<String> cachedBucketNames = new HashSet<>();
	
	String bucketName;
	S3Client s3;
	Region region;
	
	public S3AbstractFileFactory(
			Region region, 
			String accessKey,
			String secretKey, 
			String bucketName) throws URISyntaxException {
		this(region, accessKey, secretKey, bucketName, null);
	}
	
	public S3AbstractFileFactory(
			Region region, 
			String accessKey,
			String secretKey, 
			String bucketName,
			String endpoint) throws URISyntaxException {
		this.bucketName = bucketName;
		this.region = region;
		
		S3ClientBuilder builder = S3Client.builder()
				.region(region)
				.credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
			
					@Override
					public String secretAccessKey() {
						return secretKey;
					}
					
					@Override
					public String accessKeyId() {
						return accessKey;
					}
		}));
		
		if(StringUtils.isNotBlank(endpoint)) {
			builder = builder.endpointOverride(new URI(endpoint));
		}
		
		this.s3 = builder.build();
		
	
		if(!cachedBucketNames.contains(bucketName)) {
			try {
				GetBucketLocationRequest request = GetBucketLocationRequest.builder()
						.bucket(bucketName)
						.build();
				s3.getBucketLocation(request);
				cachedBucketNames.add(bucketName);
			} catch (AwsServiceException | SdkClientException e) {
				CreateBucketRequest req = CreateBucketRequest.builder().bucket(bucketName).build();
				s3.createBucket(req);
				cachedBucketNames.add(bucketName);
			}
		}
		
	}

	@Override
	public S3AbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		return (S3AbstractFile) new S3AbstractFile(this, s3, bucketName, "").resolveFile(path);
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public S3AbstractFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}
}