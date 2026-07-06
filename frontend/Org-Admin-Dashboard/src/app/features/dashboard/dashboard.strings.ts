/** Static UI strings for the Dashboard features component. */
export const DASHBOARD_STRINGS = {
  pageTitle: 'Command Center',
  pageSubtitle: 'Real-time operational triage and AI insights.',
  actions: {
    exportReport: 'Export Report',
    newTask: 'New Task',
  },
  stats: {
    awaitingAiTitle: 'Awaiting AI Review',
    awaitingAiSubtitle: '+12% vs yesterday',
    dueTodayTitle: 'Due Today / Overdue',
    dueTodaySubtitle: 'Critical Attention Required',
    myDeptTitle: 'My Dept Assignments',
    myDeptSubtitle: 'Active workload',
  },
  triageTable: {
    title: 'Central Triage',
    searchPlaceholder: 'Search tasks, priorities, or IDs...',
    headers: {
      taskId: 'Task ID',
      titleContext: 'Title & Context',
      priority: 'Priority',
      assignment: 'Assignment',
      dueDate: 'Due Date',
      status: 'Status',
    },
    pagination: {
      showingLabelPrefix: 'Showing',
      showingLabelMiddle: 'to',
      showingLabelOf: 'of',
      showingLabelSuffix: 'items',
    },
    emptyState: 'No matching tasks found in Central Triage.',
  },
  footer: {
    brandCopy: 'Praja Disha Governance Suite © 2026',
    statusLabel: 'System Status: Operational',
    versionLabel: 'v4.2.1-stable',
  },
} as const;
