package com.example.orchestration.workflowdef;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository repository;
    private final ObjectMapper objectMapper;

    public WorkflowDefinitionService(WorkflowDefinitionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Upserts by name - saving the editor's export for an existing name overwrites it. */
    @Transactional
    public WorkflowDefinition save(String name, JsonNode definition, String updatedBy) {
        String json = writeJson(definition);
        return repository.findByName(name)
                .map(existing -> {
                    existing.update(json, updatedBy);
                    return existing;
                })
                .orElseGet(() -> repository.save(new WorkflowDefinition(name, json, updatedBy)));
    }

    public WorkflowDefinition getByName(String name) {
        return repository.findByName(name).orElseThrow(() -> new WorkflowDefinitionNotFoundException(name));
    }

    public List<WorkflowDefinition> listAll() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    public JsonNode readDefinition(WorkflowDefinition definition) {
        try {
            return objectMapper.readTree(definition.getDefinitionJson());
        } catch (JsonProcessingException e) {
            // Can't happen for a value this service itself wrote via writeJson().
            throw new IllegalStateException("Stored workflow definition is not valid JSON", e);
        }
    }

    private String writeJson(JsonNode definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Workflow definition could not be serialized", e);
        }
    }
}
