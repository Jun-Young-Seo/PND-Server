package com.server.pnd.project.service;

import com.server.pnd.classDiagram.repository.ClassDiagramRepository;
import com.server.pnd.domain.*;
import com.server.pnd.participation.repository.ParticipationRepository;
import com.server.pnd.project.dto.ProjectCreatedRequestDto;
import com.server.pnd.project.dto.ProjectCreatedResponseDto;
import com.server.pnd.project.dto.ProjectSearchDetailResponseDto;
import com.server.pnd.project.dto.ProjectSearchListResponseDto;
import com.server.pnd.project.repository.ProjectRepository;
import com.server.pnd.repository.repository.RepositoryRepository;
import com.server.pnd.util.jwt.JwtUtil;
import com.server.pnd.util.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService{
    private final JwtUtil jwtUtil;
    private final RepositoryRepository repositoryRepository;
    private final ProjectRepository projectRepository;
    private final ParticipationRepository participationRepository;
    private final ClassDiagramRepository classDiagramRepository;

    @Override
    public ResponseEntity<CustomApiResponse<?>> createProject(String authorizationHeader, ProjectCreatedRequestDto projectCreatedRequestDto) {
        Optional<User> foundUser = jwtUtil.findUserByJwtToken(authorizationHeader);
        // 토큰에 해당하는 유저가 없는 경우 : 404
        if (foundUser.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(404, "유효하지 않은 토큰이거나, 해당 ID에 해당하는 사용자가 존재하지 않습니다.");
            return ResponseEntity.status(404).body(res);
        }
        User user = foundUser.get();

        Optional<Repository> foundRepository = repositoryRepository.findById(projectCreatedRequestDto.getRepositoryId());
        // 해당 Id에 해당하는 레포가 없는 경우 : 404
        if (foundRepository.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(404, "해당 ID를 가진 레포지토리가 존재하지 않습니다.");
            return ResponseEntity.status(404).body(res);
        }
        Repository repository = foundRepository.get();

        // 프로젝트 생성
        Project project = Project.builder()
                .repository(repository)
                .period(projectCreatedRequestDto.getPeriod())
                .image(projectCreatedRequestDto.getImage())
                .part(projectCreatedRequestDto.getPart())
                .title(projectCreatedRequestDto.getTitle())
                .build();
        projectRepository.save(project);

        // participation 연관관계 테이블 생성
        Participation participation = Participation.builder()
                .user(user)
                .project(project)
                .build();
        participationRepository.save(participation);

        // data
        ProjectCreatedResponseDto data = ProjectCreatedResponseDto.builder()
                .projectId(project.getId())
                .build();
        // 프로젝트 생성 성공 : 201
        CustomApiResponse<?> res = CustomApiResponse.createSuccess(201, data, "프로젝트 생성 완료했습니다.");
        return ResponseEntity.status(201).body(res);
    }

    // 프로젝트 전체 조회
    @Override
    public ResponseEntity<CustomApiResponse<?>> searchProjectList(String authorizationHeader) {
        Optional<User> foundUser = jwtUtil.findUserByJwtToken(authorizationHeader);
        // 토큰에 해당하는 유저가 없는 경우 : 404
        if (foundUser.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createFailWithoutData(404, "유효하지 않은 토큰이거나, 해당 ID에 해당하는 사용자가 존재하지 않습니다.");
            return ResponseEntity.status(404).body(res);
        }
        User user = foundUser.get();

        List<Participation> participations = participationRepository.findByUserId(user.getId());

        // 성공 - 조회할 프로젝트가 없는 경우 : 200
        if (participations.isEmpty()) {
            CustomApiResponse<?> res = CustomApiResponse.createSuccess(200,  null,"아직 생성한 프로젝트가 없습니다.");
            return ResponseEntity.status(200).body(res);
        }

        // data
        List<ProjectSearchListResponseDto> data = new ArrayList<>();
        for (Participation participation : participations) {
            ProjectSearchListResponseDto projectSearchListResponseDto = ProjectSearchListResponseDto.builder()
                    .image(participation.getProject().getImage())
                    .title(participation.getProject().getTitle())
                    .build();
            data.add(projectSearchListResponseDto);
        }

        // 성공 - 조회할 프로젝트가 있는 경우 : 200
        CustomApiResponse<?> res = CustomApiResponse.createSuccess(200, data,"프로젝트 전체 조회가 완료되었습니다.");
        return ResponseEntity.status(200).body(res);
    }

    // 프로젝트 상세 조회
    @Override
    public ResponseEntity<CustomApiResponse<?>> searchProjectDetail(Long projectId) {
        Optional<Project> foundProject = projectRepository.findById(projectId);

        // 프로젝트 ID에 해당하는 프로젝트가 없는 경우 : 404
        if (foundProject.isEmpty()) {
            return ResponseEntity.status(404).body(CustomApiResponse.createFailWithoutData(404, "해당 ID를 가진 프로젝트가 존재하지 않습니다."));
        }
        Project project = foundProject.get();

        Optional<ClassDiagram> foundClassDiagram = classDiagramRepository.findByProjectId(project.getId());
        // 클래스다이어그램에 플로우차트 존재하지 않음 : 404
        if (foundClassDiagram.isEmpty()) {
            return ResponseEntity.status(404).body(CustomApiResponse.createFailWithoutData(404, "해당 클래스다이어그램에 flowChart가 존재하지 않습니다. (클래스다이어그램 생성시 flowchart 들어가지 않음)"));
        }
        ClassDiagram classDiagram = foundClassDiagram.get();

        // data
        ProjectSearchDetailResponseDto data = ProjectSearchDetailResponseDto.builder()
                .title(project.getTitle())
                .period(project.getPeriod())
                .createdAt(project.localDateTimeToString())
                .image(project.getImage())
                .classDiagram(classDiagram.getFlowchart())
                .build();

        // 프로젝트 조회 성공 (200)
        CustomApiResponse<?> res = CustomApiResponse.createSuccess(200, data,"프로젝트 상세조회에 성공했습니다.");
        return ResponseEntity.status(200).body(res);    }
}