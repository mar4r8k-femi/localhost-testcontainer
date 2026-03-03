package com.example.service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

/**
 * Thin wrapper around the AWS S3 SDK.
 * This is the production class exercised by S3LocalStackTest.
 */
public class S3Service {

    private final S3Client s3;

    public S3Service(S3Client s3) {
        this.s3 = s3;
    }

    public void createBucket(String bucket) {
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        // Block until the bucket is ready
        s3.waiter().waitUntilBucketExists(
            HeadBucketRequest.builder().bucket(bucket).build()
        );
    }

    public void putObject(String bucket, String key, byte[] data, String contentType) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(data)
        );
    }

    public byte[] getObject(String bucket, String key) {
        ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        );
        return response.asByteArray();
    }

    public List<String> listObjectKeys(String bucket) {
        return s3.listObjects(ListObjectsRequest.builder().bucket(bucket).build())
            .contents()
            .stream()
            .map(S3Object::key)
            .toList();
    }

    public void deleteObject(String bucket, String key) {
        s3.deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
    }

    public void deleteBucket(String bucket) {
        // Bucket must be empty before deletion
        listObjectKeys(bucket).forEach(key -> deleteObject(bucket, key));
        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
    }

    public boolean bucketExists(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
}