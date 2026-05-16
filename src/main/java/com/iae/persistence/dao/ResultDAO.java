package com.iae.persistence.dao;

import com.iae.domain.EvaluationResult;

import java.util.ArrayList;
import java.util.List;

public class ResultDAO extends BaseDAO {

    public List<EvaluationResult> findByProjectId(String projectId) {
        return new ArrayList<>();
    }

    public EvaluationResult findByProjectAndStudent(String projectId, String studentId) {
        return null;
    }

    public void deleteByProjectId(String projectId) {
    }

    public void save(String projectId, EvaluationResult result) {
    }
}
