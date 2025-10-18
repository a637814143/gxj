package com.gxj.cropyield.datamanagement;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.datamanagement.dto.DataImportJobDetailView;
import com.gxj.cropyield.datamanagement.dto.DataImportJobPageResponse;
import com.gxj.cropyield.datamanagement.dto.DataImportJobView;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data-import")
public class DataImportController {

    private final DataImportService dataImportService;

    public DataImportController(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DataImportJobView> upload(@RequestPart("file") MultipartFile file,
                                                 @RequestParam(required = false) DatasetType type,
                                                 @RequestParam(required = false) String name,
                                                 @RequestParam(required = false) String description) {
        DataImportJobView view = dataImportService.submitImport(file, type, name, description);
        return ApiResponse.success(view);
    }

    @GetMapping("/tasks")
    public ApiResponse<DataImportJobPageResponse> listTasks(@RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(dataImportService.listJobs(status, keyword, page, size));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<DataImportJobDetailView> getTask(@PathVariable String taskId) {
        return ApiResponse.success(dataImportService.getJobDetail(taskId));
    }

    @DeleteMapping("/tasks")
    public ApiResponse<Void> deleteTasks(@RequestBody List<String> taskIds) {
        dataImportService.deleteTasks(taskIds);
        return ApiResponse.success();
    }
}
