package com.centerton.centerton.global.storage.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageTest {

    private static final String BUCKET = "centerton-private";
    private static final String OBJECT_KEY = "preconsult-submissions/file.jpg";

    @Mock
    private S3Client s3Client;

    private S3ObjectStorage objectStorage;

    @BeforeEach
    void setUp() {
        objectStorage = new S3ObjectStorage(
                s3Client,
                new S3StorageProperties("ap-northeast-2", BUCKET)
        );
    }

    @Test
    void uploadsWithBucketKeyContentTypeAndContentLength() {
        byte[] content = "image".getBytes(StandardCharsets.UTF_8);
        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        )).thenReturn(PutObjectResponse.builder().build());

        objectStorage.upload(
                OBJECT_KEY,
                "image/jpeg",
                content.length,
                new ByteArrayInputStream(content)
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor =
                ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(
                requestCaptor.capture(),
                bodyCaptor.capture()
        );

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(OBJECT_KEY);
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(request.contentLength()).isEqualTo(content.length);
        assertThat(bodyCaptor.getValue().contentLength())
                .isEqualTo(content.length);
    }

    @Test
    void loadsStreamingResourceThatCanOpenANewStreamEachTime() throws Exception {
        byte[] content = "streamed-image".getBytes(StandardCharsets.UTF_8);
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength((long) content.length)
                        .build());
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> responseStream(content));

        Resource resource = objectStorage.load(OBJECT_KEY);

        assertThat(resource.contentLength()).isEqualTo(content.length);
        assertThat(resource.getFilename()).isEqualTo("file.jpg");
        try (InputStream first = resource.getInputStream();
             InputStream second = resource.getInputStream()) {
            assertThat(first.readAllBytes()).isEqualTo(content);
            assertThat(second.readAllBytes()).isEqualTo(content);
        }
        verify(s3Client, times(2)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void mapsMissingObjectToNotFoundReason() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertThatThrownBy(() -> objectStorage.load(OBJECT_KEY))
                .isInstanceOfSatisfying(
                        S3StorageException.class,
                        exception -> assertThat(exception.isObjectNotFound())
                                .isTrue()
                );
    }

    @Test
    void mapsSdkUploadFailureToOperationFailure() {
        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        )).thenThrow(SdkClientException.create("upload failed"));

        assertThatThrownBy(() -> objectStorage.upload(
                OBJECT_KEY,
                "image/jpeg",
                1,
                new ByteArrayInputStream(new byte[]{1})
        )).isInstanceOfSatisfying(
                S3StorageException.class,
                exception -> assertThat(exception.isObjectNotFound())
                        .isFalse()
        );
    }

    @Test
    void deletesUsingBucketAndObjectKey() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        objectStorage.delete(OBJECT_KEY);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(OBJECT_KEY);
    }

    private ResponseInputStream<GetObjectResponse> responseStream(
            byte[] content
    ) {
        return new ResponseInputStream<>(
                GetObjectResponse.builder()
                        .contentLength((long) content.length)
                        .build(),
                AbortableInputStream.create(new ByteArrayInputStream(content))
        );
    }
}
