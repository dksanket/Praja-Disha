/** Static UI strings for the Manage Team feature. */
export const MANAGE_TEAM_STRINGS = {
  pageTitle: 'Team Management',
  pageSubtitle: 'Manage departmental structures and personnel assignments.',

  tabs: {
    officers: 'Officers',
    departments: 'Departments',
  },

  departments: {
    addButton: 'Add Department',
    searchPlaceholder: 'Search departments by name or ID...',
    filterButton: 'Filter',
    columnsButton: 'Columns',
    pageReportTemplate: 'Showing {first} to {last} of {totalRecords} departments',

    tableHeaders: {
      deptId: 'Dept ID',
      name: 'Department Name',
      parent: 'Parent Department',
      head: 'Head of Department',
      officers: 'Officers',
    },

    noneParent: 'None',
    vacantHead: 'Vacant',
  },

  officers: {
    addButton: 'Add Officer',
    searchPlaceholder: 'Search officers by name, email or ID...',
    filterButton: 'Filter',
    editTooltip: 'Edit Officer',
    pageReportTemplate: 'Showing {first} to {last} of {totalRecords} officers',

    tableHeaders: {
      nameId: 'Officer Name & ID',
      email: 'Email',
      phone: 'Phone',
      departments: 'Departments',
      status: 'Status',
    },

    statusActive: 'Active',
    statusInactive: 'Inactive',
  },

  addEditDepartment: {
    backButton: 'Back to Manage Team',
    pageTitleAdd: 'Add Department',
    pageTitleEdit: 'Edit Department',
    pageSubtitleAdd: 'Create a new organizational unit within the governance structure.',
    pageSubtitleEdit: 'Modify the details and configuration of the organizational unit.',
    sections: {
      basicDetails: 'Basic Details',
      hierarchy: 'Hierarchy & Leadership',
      geoAiConfig: 'Geographic & AI Configuration',
      assignedOfficers: 'Assigned Officers',
    },
    fields: {
      name: 'Department Name',
      namePlaceholder: 'e.g. Water Board',
      id: 'Department ID',
      idPlaceholder: 'e.g. DEP-WB-001',
      description: 'Description',
      descriptionPlaceholder: "Brief description of the department's mandate...",
      parentDept: 'Parent Department',
      parentDeptPlaceholder: 'Select Parent Department...',
      deptHeads: 'Department Head(s)',
      deptHeadsSearchPlaceholder: 'Search for an officer to add as head...',
      constituencyName: 'Constituency Name',
      constituencyNamePlaceholder: 'e.g. Ward 12 - Central District',
      constituencyBoundary: 'Constituency Boundary (GeoJSON)',
      constituencyBoundaryUpload: 'Click to draw or upload GeoJSON coordinates.',
      defineBoundaryBtn: 'Define Boundary on Map',
      aiRoutingRules: 'Custom AI Routing Rules (Prompt Extension)',
      aiRoutingRulesSubtext: 'Define department-specific logic for AI triage and response generation.',
      aiRoutingRulesPlaceholder: 'e.g., Prioritize emergency water logging requests in low-lying areas during monsoon...',
    },
    officersSection: {
      addBtn: 'Add Officer',
      emptyText: 'No officers assigned yet. Click "Add Officer" to begin building this team.',
      searchPlaceholder: 'Search for an officer to assign...',
    },
    hierarchyPreview: {
      title: 'Hierarchy Preview',
      policyTip: "Setting a Parent Department automatically inherits the parent's compliance policies unless overridden.",
      defaultRoot: 'Federal Oversight',
    },
    actions: {
      cancel: 'Cancel',
      save: 'Save Changes',
    },
    validation: {
      nameRequired: 'Department Name is required.',
      idRequired: 'Department ID is required.',
      idPattern: 'Department ID must be in format DPT-XXX (e.g. DPT-001).',
      invalidGeoJson: 'Invalid GeoJSON coordinates. Coordinates must be a valid GeoJSON polygon.',
      outOfOrgBoundary: 'The department boundary must be entirely within the organization\'s constituency boundary.',
    },
    mapModal: {
      title: 'Define Boundary on Map',
      subtitle: 'Use the drawing toolbar at the top of the map to draw a polygon. The boundary must be entirely within the organization\'s boundary.',
      clearBtn: 'Clear Points',
      closeBtn: 'Apply Boundary',
      coordsLabel: 'Plotted Coordinates:',
      emptyCoords: 'No coordinates drawn yet. Draw a polygon inside the organization\'s boundary.',
      apiKeyRequired: 'Google Maps API Key is required to use the interactive map drawing tool.',
    }
  },

  addEditOfficer: {
    backButton: 'Back to Manage Team',
    pageTitleAdd: 'Add Officer',
    pageTitleEditPrefix: 'Officer Profile: ',
    pageSubtitleAdd: 'Create a new officer profile within the governance structure.',
    pageSubtitleEdit: 'Modify the details and organizational associations of the officer.',
    sections: {
      basicInfo: 'Basic Information',
      deptAssignments: 'Department Assignments',
      reportingHierarchy: 'Reporting Hierarchy',
    },
    fields: {
      fullName: 'Full Name',
      fullNamePlaceholder: 'e.g. Kiran Kumar',
      username: 'Username',
      usernamePlaceholder: 'e.g. kkumar_admin',
      email: 'Email Address',
      emailPlaceholder: 'e.g. kiran.kumar@gov.in',
      phone: 'Phone Number',
      phonePlaceholder: 'e.g. 98765 43210',
      phonePrefix: '+91',
      status: 'Account Status',
      statusActive: 'Active',
      statusInactive: 'Inactive',
      statusHelp: 'Inactive officers cannot access the platform but retain historical data records.',
      deptAssignDesc: 'Select the departments this officer is authorized to manage.',
      deptSearchPlaceholder: 'Search departments to add...',
      directManagers: 'Direct Managers',
      managersSearchPlaceholder: 'Search managers to add...',
    },
    actions: {
      cancel: 'Cancel',
      save: 'Save Changes',
    },
    validation: {
      nameRequired: 'Full Name is required.',
      usernameRequired: 'Username is required.',
      usernamePattern: 'Username must contain only letters, numbers, and underscores.',
      usernameLength: 'Username must be at least 3 characters long.',
      emailPattern: 'Please enter a valid email address.',
      phonePattern: 'Please enter a valid 10-digit phone number.',
      duplicateUsername: 'This username is already taken.',
      saveFailed: 'Failed to save changes. Please try again.',
      createFailed: 'Failed to create officer. Please try again.',
    },
  },
} as const;

