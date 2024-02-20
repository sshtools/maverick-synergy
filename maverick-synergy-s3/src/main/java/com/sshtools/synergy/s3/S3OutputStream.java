package com.sshtools.synergy.s3;

/*-
 * #%L
 * S3 File System
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

public class S3OutputStream extends OutputStream {

  /**
   * Default chunk size is 10MB
   */
  protected static final int BUFFER_SIZE = 5 * 1024 * 1024;

  /**
   * The bucket-name on Amazon S3
   */
  private final String bucket;

  /**
   * The path (key) name within the bucket
   */
  private final String path;

  /**
   * The temporary buffer used for storing the chunks
   */
  private final byte[] buf;

  private final S3Client s3Client;
  /**
   * Collection of the etags for the parts that have been uploaded
   */
  private final List<String> etags;
  /**
   * The position in the buffer
   */
  private int position;
  /**
   * The unique id for this upload
   */
  private String uploadId;
  /**
   * indicates whether the stream is still open / valid
   */
  private boolean open;

  /**
   * Creates a new S3 OutputStream
   *
   * @param s3Client the AmazonS3 client
   * @param bucket   name of the bucket
   * @param path     path within the bucket
   */
  public S3OutputStream(S3Client s3Client, String bucket, String path) {
    this.s3Client = s3Client;
    this.bucket = bucket;
    this.path = path;
    buf = new byte[BUFFER_SIZE];
    position = 0;
    etags = new ArrayList<>();
    open = true;
  }

  public void cancel() {
    open = false;
    if (uploadId != null) {
      s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(path)
          .uploadId(uploadId)
          .build());
    }
  }

  @Override
  public void write(int b) {
    assertOpen();
    if (position >= buf.length) {
      flushBufferAndRewind();
    }
    buf[position++] = (byte) b;
  }

  /**
   * Write an array to the S3 output stream.
   *
   * @param b the byte-array to append
   */
  @Override
  public void write(byte[] b) {
    write(b, 0, b.length);
  }

  /**
   * Writes an array to the S3 Output Stream
   *
   * @param byteArray the array to write
   * @param o         the offset into the array
   * @param l         the number of bytes to write
   */
  @Override
  public void write(byte[] byteArray, int o, int l) {
    assertOpen();
    int ofs = o;
    int len = l;
    int size;
    while (len > (size = buf.length - position)) {
      System.arraycopy(byteArray, ofs, buf, position, size);
      position += size;
      flushBufferAndRewind();
      ofs += size;
      len -= size;
    }
    System.arraycopy(byteArray, ofs, buf, position, len);
    position += len;
  }

  /**
   * Flushes the buffer by uploading a part to S3.
   */
  @Override
  public synchronized void flush() {
    assertOpen();
  }

  @Override
  public void close() {
    if (open) {
      open = false;
      if (uploadId != null) {
        if (position > 0) {
          uploadPart();
        }

        CompletedPart[] completedParts = new CompletedPart[etags.size()];
        for (int i = 0; i < etags.size(); i++) {
          completedParts[i] = CompletedPart.builder()
              .eTag(etags.get(i))
              .partNumber(i + 1)
              .build();
        }

        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
            .parts(completedParts)
            .build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(path)
            .uploadId(uploadId)
            .multipartUpload(completedMultipartUpload)
            .build();
        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
      } else {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .contentLength((long) position)
            .build();

        RequestBody requestBody = RequestBody.fromInputStream(new ByteArrayInputStream(buf, 0, position),
            position);
        s3Client.putObject(putRequest, requestBody);
      }
    }
  }

  private void assertOpen() {
    if (!open) {
      throw new IllegalStateException("Closed");
    }
  }

  protected void flushBufferAndRewind() {
    if (uploadId == null) {
      CreateMultipartUploadRequest uploadRequest = CreateMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(path)
          .build();
      CreateMultipartUploadResponse multipartUpload = s3Client.createMultipartUpload(uploadRequest);
      uploadId = multipartUpload.uploadId();
    }
    uploadPart();
    position = 0;
  }

  protected void uploadPart() {
    UploadPartRequest uploadRequest = UploadPartRequest.builder()
        .bucket(bucket)
        .key(path)
        .uploadId(uploadId)
        .partNumber(etags.size() + 1)
        .contentLength((long) position)
        .build();
    RequestBody requestBody = RequestBody.fromInputStream(new ByteArrayInputStream(buf, 0, position),
        position);
    UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadRequest, requestBody);
    etags.add(uploadPartResponse.eTag());
  }
}

