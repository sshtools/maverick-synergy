/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.s3;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
		
	
		try {
			GetBucketLocationRequest request = GetBucketLocationRequest.builder()
					.bucket(bucketName)
					.build();
			s3.getBucketLocation(request);
		} catch (AwsServiceException | SdkClientException e) {
			CreateBucketRequest req = CreateBucketRequest.builder().bucket(bucketName).build();
			s3.createBucket(req);
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
