package com.server.pnd.repo.service;

import com.server.pnd.diagram.repository.DiagramRepository;
import com.server.pnd.domain.*;
import com.server.pnd.readme.repository.ReadmeRepository;
import com.server.pnd.repo.dto.*;
import com.server.pnd.repo.repository.RepoRepository;
import com.server.pnd.report.repository.ReportRepository;
import com.server.pnd.util.jwt.JwtUtil;
import com.server.pnd.util.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepoServiceImpl implements RepoService {
    private final JwtUtil jwtUtil;
    private final RepoRepository repoRepository;
    private final DiagramRepository diagramRepository;
    private final ReadmeRepository readmeRepository;
    private final ReportRepository reportRepository;

    // 레포 전체 조회
    @Override
    public ResponseEntity<CustomApiResponse<?>> getAllRepository(String authorizationHeader) {
        Optional<User> foundUser = jwtUtil.findUserByJwtToken(authorizationHeader);
        // 토큰에 해당하는 유저가 없는 경우 : 404
        if (foundUser.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(404, "유효하지 않은 토큰이거나, 해당 ID에 해당하는 사용자가 존재하지 않습니다.");
            return ResponseEntity.status(404).body(res);
        }
        User user = foundUser.get();

        List<Repo> repositories = repoRepository.findByUserId(user.getId());
        
        // 조회 성공 - 해당 회원의 깃허브 레포지토리가 존재하지 않는 경우 : 200
        if (repositories.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createSuccess(200, null, "문서를 생성한 레포가 존재하지 않습니다.");
            return ResponseEntity.status(200).body(res);
        }

        // 조회 성공 - 해당 회원의 깃허브 레포지토리가 존재하는 경우 : 200
        List<SearchRepositoryResponseDto> responseDtos = new ArrayList<>();
        for (Repo repo : repositories) {
            SearchRepositoryResponseDto responseDto = SearchRepositoryResponseDto.builder()
                    .id(repo.getId())
                    .repoName(repo.getRepoName())
                    .repoDescription(repo.getRepoDescription())
                    .repoStars(repo.getRepoStars())
                    .repoForksCount(repo.getRepoForksCount())
                    .repoOpenIssues(repo.getRepoOpenIssues())
                    .repoWatcher(repo.getRepoWatcher())
                    .repoLanguage(repo.getRepoLanguage())
                    .repoDisclosure(repo.getRepoDisclosure())
                    .createdAt(repo.getFormattedCreatedAt()).build();
            responseDtos.add(responseDto);
        }
        CustomApiResponse<?> res = CustomApiResponse.createSuccess(200, responseDtos, "레포지토리 전체 조회 성공했습니다.");
        return ResponseEntity.status(200).body(res);
    }

    // 생성된 레포 전체 조회
    @Override
    public ResponseEntity<CustomApiResponse<?>> findReposWithExistingDocuments(String authorizationHeader) {
        Optional<User> foundUser = jwtUtil.findUserByJwtToken(authorizationHeader);
        // 토큰에 해당하는 유저가 없는 경우 : 404
        if (foundUser.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(404, "유효하지 않은 토큰이거나, 해당 ID에 해당하는 사용자가 존재하지 않습니다.");
            return ResponseEntity.status(404).body(res);
        }
        User user = foundUser.get();

        // UserId로 생성된 문서가 하나라도 있는 Repo들 리턴
        List<Repo> repos = repoRepository.findReposWithAnyDocumentByUserId(user.getId());

        // 조회 성공 - 문서를 생성한 레포 존재하지 않음 : 200
        if (repos.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(200, "문서를 생성한 레포가 존재하지 않습니다.");
            return ResponseEntity.status(200).body(res);
        }

        // data
        List<ExistRepoResponseDto> data = new ArrayList<>();
        for (Repo repo : repos) {
            // repoId를 가진 readme가 있는지 검색
            boolean isExistReadme = readmeRepository.existsByRepoId(repo.getId());
            // repoId를 가지고 class_script필드가 null이 아닌 Diagram이 있는지 검색
            boolean isExistClassDiagram = diagramRepository.existsByRepoIdAndClassScriptIsNotNull(repo.getId());
            // repoId를 가지고 sequence_script필드가 null이 아닌 Diagram이 있는지 검색
            boolean isExistSequenceDiagram = diagramRepository.existsByRepoIdAndSequenceScriptIsNotNull(repo.getId());
            // repoId를 가지고 erd_script필드가 null이 아닌 Diagram이 있는지 검색
            boolean isExistErDiagram = diagramRepository.existsByRepoIdAndErdScriptIsNotNull(repo.getId());
            // repoId를 가진 report가 있는지 검색
            boolean isExistReport = reportRepository.existsByRepoId(repo.getId());

            ExistRepoResponseDto existRepoResponseDto = ExistRepoResponseDto.builder()
                    .id(repo.getId())
                    .title(repo.getTitle())
                    .period(repo.getPeriod())
                    .image(repo.getImage())
                    .isExistReadme(isExistReadme)
                    .isExistClassDiagram(isExistClassDiagram)
                    .isExistSequenceDiagram(isExistSequenceDiagram)
                    .isExistErDiagram(isExistErDiagram)
                    .isExistReport(isExistReport)
                    .build();
            data.add(existRepoResponseDto);
        }

        // 성공 - 조회할 프로젝트가 있는 경우 : 200
        CustomApiResponse<?> res = CustomApiResponse.createSuccess(200, data,"문서를 생성한 레포 전체 조회 성공했습니다.");
        return ResponseEntity.status(200).body(res);
    }


    // 레포 기본 정보 세팅
    @Override
    public ResponseEntity<CustomApiResponse<?>> settingRepo(Long repoId, RepoSettingRequestDto projectCreatedRequestDto, MultipartFile image) {
        Optional<Repo> foundRepository = repoRepository.findById(repoId);
        // 해당 Id에 해당하는 레포가 없는 경우 : 404
        if (foundRepository.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(404, "해당 ID에 해당하는 레포가 없습니다.");
            return ResponseEntity.status(404).body(res);
        }
        Repo repo = foundRepository.get();

        // 레포 기본 정보 세팅 - 이미지 제외
        String title = projectCreatedRequestDto.getTitle();
        String period = projectCreatedRequestDto.getPeriod();
        repo.editRepoWithoutImage(title, period);

        // 프로필 이미지 수정 있을 시
        if (image != null && !image.isEmpty()) {
            String imageName = String.valueOf(repo.getId()); // 레포 사진의 이름은 repo의 pk를 이용(한 Repo당 하나의 썸네일 사진)
            String imageUrl = // s3Service.createRepoImage(image, imageName); 추후 S3 설계 후 설정
            "https~~";
            repo.editRepoImage(imageUrl);
        }

        // 저장
        repoRepository.save(repo);

        // data (save 한 repo 정보 가져오기)
        RepoSettingResponseDto data = RepoSettingResponseDto.builder()
                .repoId(repo.getId())
                .title(repo.getTitle())
                .period(repo.getPeriod())
                .image(repo.getImage())
                .build();

        // 레포 세팅 성공 : 200
        CustomApiResponse<?> res = CustomApiResponse.createSuccess(200, data, "레포 기본 정보 세팅에 성공하셨습니다.");
        return ResponseEntity.status(200).body(res);
    }


}
