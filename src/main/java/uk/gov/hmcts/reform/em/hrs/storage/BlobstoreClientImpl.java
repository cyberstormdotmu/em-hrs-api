package uk.gov.hmcts.reform.em.hrs.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidRangeRequestException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class BlobstoreClientImpl implements BlobstoreClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobstoreClientImpl.class);
    private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20KB.
    private final BlobContainerClient blobContainerClient;

    @Autowired
    public BlobstoreClientImpl(@Qualifier("HrsBlobContainerClient") final BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public void downloadFile(final String filename, HttpServletRequest request,
                             final HttpServletResponse response) {
        final BlockBlobClient blobClient = blobContainerClient.getBlobClient(filename).getBlockBlobClient();

        if (request.getHeader(HttpHeaders.RANGE) == null) {
            try {
                //                blobClient.download(response.getOutputStream());
                loadFullBlob(filename, blobClient, response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                final long fileSize = blobClient.getProperties().getBlobSize();
                loadPartialBlob(filename, fileSize, blobClient, request, response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    public BlobProperties getBlobProperties(final String filename) {
        return blobContainerClient.getBlobClient(filename).getProperties();
    }


    private void loadFullBlob(String filename, BlockBlobClient blobClient, HttpServletResponse response)
        throws IOException {
        LOGGER.info("No Range header provided; returning entire document {}", filename);

        blobClient.download(response.getOutputStream());

        LOGGER.info("Reading document version {} from Azure Blob Storage: OK", filename);
    }


    private void loadPartialBlob(String filename, Long fileSize, BlockBlobClient blobClient,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {

        LOGGER.debug("Range header provided {}", filename);

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        LOGGER.debug("Range requested: {}", rangeHeader);

        Long length = fileSize;

        String patternString = "^bytes=\\d*-\\d*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(rangeHeader);

        if (!matcher.matches()) {
            throw new InvalidRangeRequestException(response, length);
        }

        response.setBufferSize(DEFAULT_BUFFER_SIZE);

        // Range headers can request a multipart range but this is not to be supported yet
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
        // Take only first requested range and process it
        String part = rangeHeader.substring(6).split(",")[0].trim();

        BlobRange b = processPart(part, length, response);

        if (b.getOffset() == 0 && b.getCount() > length) {
            loadFullBlob(filename, blobClient, response);
            return;
        }

        response.setStatus(HttpStatus.PARTIAL_CONTENT.value());

        LOGGER.debug("Processing blob range: {}", b.toString());
        blockBlobClient(filename.toString())
            .downloadWithResponse(
                response.getOutputStream(),
                b,
                new DownloadRetryOptions().setMaxRetryRequests(5),
                null,
                false,
                null,
                null
            );
    }

    private BlobRange processPart(String part, Long length, HttpServletResponse response) {
        long start = subLong(part, 0, part.indexOf('-'));
        long end = subLong(part, part.indexOf('-') + 1, part.length());

        if (start == -1) {
            start = length - end;
            end = length;
        } else if (end == -1 || end > length) {
            end = length;
        }

        // Check if Range is syntactically valid. If not, then return 416.
        if (start > end) {
            throw new InvalidRangeRequestException(response, length);
        }

        long rangeByteCount = (end - start) + 2;
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeByteCount - 1));

        return new BlobRange(start, rangeByteCount);
    }

    private long subLong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    private BlockBlobClient blockBlobClient(String id) {
        return blobContainerClient.getBlobClient(id).getBlockBlobClient();
    }

}
