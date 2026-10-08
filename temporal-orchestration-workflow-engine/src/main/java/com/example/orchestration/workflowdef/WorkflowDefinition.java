package com.example.orchestration.workflowdef;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A saved Workflow Editor diagram - the raw JSON the SDK's canvas produces (nodes, edges,
 * layoutDirection, ...), stored verbatim rather than parsed into columns. This is a design-time
 * artifact only; it does not drive Temporal execution (see WorkflowExecutionService for that).
 */
@Entity
@Table(name = "workflow_definitions", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Plain TEXT, not @Lob - @Lob maps a String to Postgres's "oid" large-object type (a separate
    // system table referenced by an id, meant for multi-MB+ blobs with special vacuum/lifecycle
    // handling), which is the wrong fit for JSON this size and makes it painful to just inspect
    // the column directly (e.g. `psql`'s length()/substring() don't work on an oid).
    @Column(name = "definition_json", nullable = false, columnDefinition = "text")
    private String definitionJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    protected WorkflowDefinition() {
        // JPA
    }

    public WorkflowDefinition(String name, String definitionJson, String updatedBy) {
        this.name = name;
        this.definitionJson = definitionJson;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void update(String definitionJson, String updatedBy) {
        this.definitionJson = definitionJson;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDefinitionJson() {
        return definitionJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
