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
  }
} as const;
