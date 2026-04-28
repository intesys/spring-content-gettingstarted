package gettingstarted;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.springframework.content.commons.metadataextraction.MetadataExtractionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.WebApplicationContext;


import org.apache.commons.io.IOUtils;

@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GettingStartedTest {

	@Autowired private MetadataExtractionService metadataExtractionService;
	@Autowired private FileRepository fileRepo;
	@Autowired private FileContentStore fileContentStore;

	@Autowired
	private WebApplicationContext context;

    private File realFile;
	private gettingstarted.File fileEntity;

    {
        Describe("Metadata Extraction Tests", () -> {
        	BeforeEach(() -> {
				RestAssuredMockMvc.webAppContextSetup(context);
        	});

        	Context("given a real file content", () -> {
        		BeforeEach(() -> {
					ClassPathResource classPathResource = new ClassPathResource("/static/empty_pdf.pdf");
					realFile = classPathResource.getFile();
        		});

        		It("should be possible to extract metadata and save in a file entity", () -> {
					var metadata = metadataExtractionService.extractMetadata(realFile);
					assertThat(metadata, is(not(nullValue())));
					assertThat(metadata.size(), is(7));
					assertThat(metadata.get("fileName"), is("empty_pdf.pdf"));
					assertThat(metadata.get("lastModifiedTime"), is(not(nullValue())));
					assertThat(metadata.get("lastAccessTime"), is(not(nullValue())));
					assertThat(metadata.get("size"), is(realFile.length()));
					assertThat(metadata.get("creationTime"), is(not(nullValue())));
					assertThat(metadata.get("fileExtension"), is("pdf"));
					assertThat(metadata.get("mimeType"), is("application/pdf"));

					fileEntity = new gettingstarted.File();
					fileEntity.setContentMimeType((String) metadata.get("mimeType"));
					fileEntity.setName((String) metadata.get("fileName"));
					fileEntity = fileContentStore.setContent(fileEntity, new FileInputStream(realFile));
					fileEntity = fileRepo.save(fileEntity);

					assertThat(fileEntity.getContentLength(), is(metadata.get("size")));
				});

				It("should be possible to upload content via from-real-file API", () -> {
					long initialCount = fileRepo.count();

					given()
						.multiPart("file", realFile)
					.when()
						.post("/files/from-real-file")
					.then()
						.statusCode(HttpStatus.OK.value())
						.body(containsString("File successfully saved"))
						.body(containsString("empty_pdf.pdf"))
						.body(containsString("mimeType=application/pdf"));

					assertThat(fileRepo.count(), is(initialCount + 1));

					var savedFile = fileRepo.findAll().stream()
							.filter(f -> "empty_pdf.pdf".equals(f.getName()))
							.findFirst()
							.orElseThrow();

					assertThat(savedFile.getContentId(), is(not(nullValue())));
					assertThat(savedFile.getContentLength(), is(realFile.length()));

					try (var contentStream = fileContentStore.getContent(savedFile);
						 var originalStream = new FileInputStream(realFile)) {
						assertThat(IOUtils.contentEquals(contentStream, originalStream), is(true));
					}
				});
        	});
        });
    }

    @Test
    public void noop() {}
}
