package com.app.civify.usecase.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cv.CvEnhancementService;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of CV use case handling upload and tailor operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvUseCaseImpl implements CvUseCase {

    private final CvParsingService cvParsingService;
    private final CvEnhancementService cvEnhancementService;
    private final CvOperationLogService cvOperationLogService;
    private final UserRepository userRepository;

    @Override
    public CvParseResponse uploadCv(MultipartFile file, CvParseRequest request, Long userId,
                                     String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        log.info("Uploading CV: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CvParseResponse response = cvParsingService.parseCv(file, request, userId);

        long processingTime = System.currentTimeMillis() - startTime;

        cvOperationLogService.logUpload(
                user,
                response.getResumeData(),
                "/api/v1/cvs/upload",
                processingTime,
                response.isSuccess(),
                response.isSuccess() ? null : response.getMessage(),
                clientIp,
                userAgent,
                file.getOriginalFilename(),
                request.getLanguage()
        );

        return response;
    }

    @Override
    public CvTailorResponse tailorCv(CvTailorRequest request, Long userId,
                                      String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        log.info("Tailoring CV for job: {}",
                request.getJobTitle() != null ? request.getJobTitle() : "unspecified");

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CvTailorResponse response = cvEnhancementService.tailorCv(request);

        long processingTime = System.currentTimeMillis() - startTime;

        cvOperationLogService.logTailor(
                user,
                request.getResumeData(),
                "/api/v1/cvs/tailor",
                processingTime,
                true,
                null,
                clientIp,
                userAgent,
                request.getJobTitle()
        );

        return response;
    }
}
