import { useEffect, useState } from 'react';
import { NodeType, WorkflowBuilder } from '@workflowbuilder/sdk';
import type {
  DidSaveStatus,
  OnSaveExternal,
  PaletteItem,
  PaletteItemOrGroup,
  UISchema,
  WBIcon,
  WorkflowBuilderEdge,
  WorkflowBuilderNode,
} from '@workflowbuilder/sdk';
import '@workflowbuilder/sdk/style.css';
import { ApiError, getWorkflowDefinition, saveWorkflowDefinition } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const WORKFLOW_NAME = 'account-creation-workflow';

/** A custom field on a built-in node: its JSON Schema type plus the caption shown above its input. */
type ExtraField = { type: 'string' | 'number'; caption: string };

/**
 * The SDK's Properties panel (JsonForms) only ships renderers for its own typed control
 * variants (Text/TextArea/Switch/Select/...), each keyed by a literal `type` string. Omitting
 * `uischema` makes JsonForms fall back to its generic `{type:'Control'}` auto-layout, which none
 * of the SDK's renderers match - hence "No renderer provided for type: Control". Building an
 * explicit `VerticalLayout` of the SDK's own control elements avoids the fallback entirely.
 *
 * Every control gets an explicit `label` (JsonForms' `Labelable.label`) rather than leaving it to
 * auto-derive one from the schema key - workflowbuilder.io's own product page describes its
 * properties panel as showing "field labels matching node configuration requirements", and a bare
 * `scope` with no `label` renders no caption at all under this SDK's renderers.
 */
function buildUiSchema(extraFields: Record<string, ExtraField>): UISchema {
  return {
    type: 'VerticalLayout',
    elements: [
      { type: 'Text', scope: '#/properties/label', label: 'Label' },
      { type: 'TextArea', scope: '#/properties/description', label: 'Description' },
      ...Object.entries(extraFields).map(([key, field]) => ({
        type: 'Text' as const,
        scope: `#/properties/${key}`,
        label: field.caption,
        ...(field.type === 'number' ? { inputType: 'number' } : {}),
      })),
    ],
  };
}

/**
 * The 7 built-in node types documented at https://www.workflowbuilder.io/docs/nodes/
 * (Trigger, Action, Conditional, Decision, Delay, Notification, AI Agent). The SDK ships the
 * *types* to define these (PaletteItem/NodeSchema/NodeType), not pre-built objects - confirmed
 * against the published package's exports, which expose no `defaultNodeTypes`/`builtInNodes`
 * array. This reconstructs the documented reference set field-for-field.
 */
function extraFieldsItem(
  type: string,
  label: string,
  description: string,
  icon: WBIcon,
  templateType: NodeType,
  extraFields: Record<string, ExtraField>,
  extraDefaults: Record<string, string | number>,
): PaletteItem {
  return {
    type,
    label,
    description,
    icon,
    templateType,
    schema: {
      type: 'object',
      properties: {
        // label/description are the SDK's fixed BaseNodePropertiesSchema - typed to exactly
        // `{ type: 'string' }`, no room for a caption override; its panel captions these two
        // itself (see the uischema's own `label` on their Text/TextArea controls below).
        label: { type: 'string' },
        description: { type: 'string' },
        ...Object.fromEntries(
          Object.entries(extraFields).map(([key, field]) => [key, { type: field.type, label: field.caption }]),
        ),
      },
    },
    uischema: buildUiSchema(extraFields),
    defaultPropertiesData: { label, description, ...extraDefaults },
  };
}

const BUILT_IN_NODES: PaletteItemOrGroup[] = [
  {
    label: 'Built-in Nodes',
    isOpen: true,
    groupItems: [
      extraFieldsItem(
        'trigger',
        'Trigger',
        'Starts a workflow in response to an event, schedule, or condition.',
        'Lightning',
        NodeType.StartNode,
        { triggerType: { type: 'string', caption: 'Trigger Type' } },
        { triggerType: 'event | schedule | api' },
      ),
      extraFieldsItem(
        'action',
        'Action',
        'Executes a task such as sending email, calling an API, or running a script.',
        'Play',
        NodeType.Node,
        {
          actionType: { type: 'string', caption: 'Action Type' },
          target: { type: 'string', caption: 'Target' },
        },
        { actionType: 'http-request | email | script', target: '' },
      ),
      extraFieldsItem(
        'conditional',
        'Conditional',
        'Splits the workflow into two branches based on a true/false condition.',
        'ArrowsSplit',
        NodeType.DecisionNode,
        { condition: { type: 'string', caption: 'Condition' } },
        { condition: 'e.g. amount > 1000' },
      ),
      extraFieldsItem(
        'decision',
        'Decision',
        'Routes the workflow to one of several named branches.',
        'Signpost',
        NodeType.DecisionNode,
        { branches: { type: 'string', caption: 'Branches' } },
        { branches: 'e.g. approve, reject, escalate' },
      ),
      extraFieldsItem(
        'delay',
        'Delay',
        'Pauses the workflow for a fixed, dynamic, or conditional duration.',
        'Hourglass',
        NodeType.Node,
        {
          durationType: { type: 'string', caption: 'Duration Type' },
          durationSeconds: { type: 'number', caption: 'Duration (seconds)' },
        },
        { durationType: 'fixed | dynamic | conditional', durationSeconds: 0 },
      ),
      extraFieldsItem(
        'notification',
        'Notification',
        'Sends a notification through email, SMS, push, webhook, or Slack.',
        'BellRinging',
        NodeType.Node,
        {
          channel: { type: 'string', caption: 'Channel' },
          message: { type: 'string', caption: 'Message' },
        },
        { channel: 'email | sms | push | webhook | slack', message: '' },
      ),
      extraFieldsItem(
        'ai-agent',
        'AI Agent',
        'Delegates a step to an AI model with tool access.',
        'AiAgent',
        NodeType.AiNode,
        {
          model: { type: 'string', caption: 'Model' },
          tools: { type: 'string', caption: 'Tools' },
        },
        { model: 'e.g. gpt-4, claude-sonnet', tools: '' },
      ),
     extraFieldsItem(
      'user-task',
      'User Task',
      'Pauses the workflow for a human to complete an assigned task, such as a manual review or approval.',
      'UserCheck',
      NodeType.Node,
      {
        assigneeRole: { type: 'string', caption: 'Assignee Role' },
        taskName: { type: 'string', caption: 'Task Name' },
        instructions: { type: 'string', caption: 'Instructions' },
      },
      { assigneeRole: 'REVIEWER', taskName: 'Manual review', instructions: '' },
    )
    ],
  },
];

type InitialDiagram = { nodes: WorkflowBuilderNode[]; edges: WorkflowBuilderEdge[] };

/**
 * Standalone visual workflow editor (workflowbuilder.io's embeddable React SDK), kept separate
 * from the KYC signup/reviewer flows. This is a design-time tool only - it does not drive the
 * actual Temporal workflow execution (see temporal-orchestration-workflow-engine); dragging these
 * nodes onto the canvas does not change what the deployed workflow does.
 *
 * Persistence follows the SDK's documented "callback" strategy
 * (https://www.workflowbuilder.io/docs/get-started/persistence/callback/) rather than reading its
 * private 'localStorage' persistence key: `integration={{ strategy: 'props', onDataSave }}` hands
 * every save straight to temporal-orchestration-workflow-engine's `/api/workflow-definitions`
 * (backed by Postgres), and `initialNodes`/`initialEdges` are populated from that same endpoint
 * before the editor ever mounts, so a diagram loads for whichever reviewer/browser opens this page
 * next instead of being stuck in the one that drew it.
 */
export default function WorkflowEditor() {
  const { token } = useAuth();
  const [initialDiagram, setInitialDiagram] = useState<InitialDiagram | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    getWorkflowDefinition(WORKFLOW_NAME, token)
      .then((found) => {
        if (cancelled) return;
        const definition = found.definition as { nodes?: WorkflowBuilderNode[]; edges?: WorkflowBuilderEdge[] };
        setInitialDiagram({ nodes: definition.nodes ?? [], edges: definition.edges ?? [] });
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setInitialDiagram({ nodes: [], edges: [] }); // nothing saved yet - start blank
        } else {
          setLoadError(err instanceof Error ? err.message : 'Could not load the saved workflow.');
          setInitialDiagram({ nodes: [], edges: [] });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const handleDataSave: OnSaveExternal = async (data): Promise<DidSaveStatus> => {
    if (!token) return 'error';
    try {
      await saveWorkflowDefinition(data, token);
      return 'success';
    } catch {
      return 'error';
    }
  };

  if (!initialDiagram) {
    return <div style={{ padding: '24px' }}>Loading saved workflow…</div>;
  }

  return (
    <div style={{ width: '100%', height: 'calc(100vh - 60px)', display: 'flex', flexDirection: 'column' }}>
      {loadError && (
        <div style={{ padding: '8px 16px', background: '#fef2f2', color: '#b91c1c', fontSize: '0.85rem', flexShrink: 0 }}>
          Could not load the previously saved workflow ({loadError}) - starting from a blank canvas.
        </div>
      )}
      {/* Deliberately NOT `.page`/`.page-wide` - those are centered, max-width, padded containers
          built for content pages (forms/cards) and clip the editor's own full-width toolbar (its
          right-aligned controls, including the overflow menu that holds Export/Import, render past
          a constrained container's edge). This tool needs a plain, edge-to-edge box instead. */}
      <div style={{ width: '100%', flex: 1, minHeight: 0 }}>
        <WorkflowBuilder.Root
          name={WORKFLOW_NAME}
          layoutDirection="DOWN"
          nodeTypes={BUILT_IN_NODES}
          initialNodes={initialDiagram.nodes}
          initialEdges={initialDiagram.edges}
          integration={{ strategy: 'props', onDataSave: handleDataSave }}
        />
      </div>
    </div>
  );
}
