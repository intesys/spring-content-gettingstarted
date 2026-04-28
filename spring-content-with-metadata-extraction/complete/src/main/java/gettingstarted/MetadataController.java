package gettingstarted;

import org.springframework.content.commons.metadataextraction.MetadataExtractionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class MetadataController {

    private final MetadataExtractionService metadataExtractionService;
    private final FileContentStore contentStore;
    private final FileRepository fileRepo;

    public MetadataController(MetadataExtractionService metadataExtractionService, FileContentStore contentStore, FileRepository fileRepo) {

        this.metadataExtractionService = metadataExtractionService;
        this.contentStore = contentStore;
        this.fileRepo = fileRepo;
    }

    @PostMapping("/from-real-file")
    public String uploadContent(@RequestParam("file") MultipartFile multipart)
        throws IOException {

        var fileEntity = new File();
        fileEntity.setName(multipart.getOriginalFilename());
        fileEntity.setSummary("From real file");

        Map<String, Object> metadata;

        var convFile = new java.io.File(Paths.get(System.getProperty("java.io.tmpdir"), fileEntity.getName()).toUri());
        multipart.transferTo(convFile);

        metadata = metadataExtractionService.extractMetadata(convFile);

        contentStore.setContent(fileEntity, new FileInputStream(convFile));
        fileRepo.save(fileEntity);

        return "File successfully saved. Metadata: " + metadata.toString();
    }
}
