package zjc.knowledgemanager.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 文件上传控制器（头像等）
 */
@RestController
@RequestMapping("/api/v1/users/profile")
public class FileUploadController {

	@Value("${file.upload.base-dir:./uploads}")
	private String uploadBaseDir;

	@PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadAvatar(@RequestPart("file") MultipartFile file,
														HttpServletRequest request) throws IOException {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body("文件不能为空");
		}

		String dateDir = LocalDate.now().toString();
		Path dir = Paths.get(uploadBaseDir, "avatars").resolve(dateDir);
		Files.createDirectories(dir);

		String original = file.getOriginalFilename();
		String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
		String filename = UUID.randomUUID().toString().replaceAll("-", "") + ext;
		Path target = dir.resolve(filename);
		Files.createDirectories(target.getParent());
		file.transferTo(target.toFile());

		String relativeUrl = "/uploads/avatars/" + dateDir + "/" + filename;
		return ResponseEntity.ok(relativeUrl);
	}
}
