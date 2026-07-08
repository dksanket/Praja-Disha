/** Static UI strings for the Task Details (Deep-Dive) component. */
export const TASK_DETAILS_STRINGS = {
  backBtn: 'Back to Dashboard',
  metaLabels: {
    id: 'ID:',
    groupId: 'Group ID:',
    parentTask: 'Parent Task:',
    orgId: 'Org ID:',
    created: 'Created:',
    reportedBy: 'Reported by:',
    descAndAudio: 'Description & Audio',
    transcription: 'Transcription',
    metadataHeader: 'Metadata',
    category: 'Category',
    language: 'Language',
    location: 'Location'
  },
  snapshots: {
    incident: 'Incident Snapshot',
    mapping: 'Location Mapping'
  },
  rightPanel: {
    treeTitle: 'Sub-Assignment Tree',
    tabs: {
      comments: 'Comments',
      notes: 'Internal Notes',
      activity: 'Activity Log'
    },
    commentsForm: {
      placeholder: 'Write a public reply to the citizen...',
      btnText: 'Send Comment'
    },
    notesForm: {
      placeholder: 'Add an internal task update (officers only)...',
      btnText: 'Save Note'
    },
    activityHeaders: {
      action: 'Action',
      user: 'By',
      remarks: 'Remarks'
    }
  },
  headerActions: {
    updateStatus: 'Update Status',
    editAssignee: 'Edit Assignee',
    addSubtask: 'Add Subtask'
  },
  updateStatusModal: {
    title: 'Update Task Status',
    statusLabel: 'New Status',
    remarksLabel: 'Remarks / Comments',
    remarksPlaceholder: 'Enter any remarks or update details...',
    cancelBtn: 'Cancel',
    submitBtn: 'Update Status',
    statuses: {
      submitted: 'Submitted',
      aiAssigned: 'AI-Assigned',
      inProgress: 'In Progress',
      resolved: 'Resolved',
      rejected: 'Rejected'
    }
  },
  editAssigneeModal: {
    title: 'Edit Task Assignee',
    deptLabel: 'Assign to Department',
    deptPlaceholder: 'Select a department...',
    officerLabel: 'Assign to Officer',
    officerPlaceholder: 'Select an officer...',
    cancelBtn: 'Cancel',
    submitBtn: 'Save Assignment'
  },
  addSubtaskModal: {
    title: 'Add New Subtask',
    taskTitleLabel: 'Subtask Title',
    taskTitlePlaceholder: 'Enter subtask title...',
    descLabel: 'Description',
    descPlaceholder: 'Provide subtask description or instructions...',
    priorityLabel: 'Priority',
    cancelBtn: 'Cancel',
    submitBtn: 'Create Subtask'
  }
} as const;
