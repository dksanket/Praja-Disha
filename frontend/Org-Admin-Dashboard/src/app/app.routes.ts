import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

/**
 * Application-level routes.
 * Authenticated routes are nested under DashboardLayoutComponent which
 * provides the persistent sidebar + top-nav shell.
 */
export const APP_ROUTES: Routes = [
  {
    path: 'login',
    // Lazy-loaded standalone component — no unnecessary bundle weight at startup
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
    title: 'Sign In | Praja Disha AI - Org Dashboard'
  },

  {
    // Authenticated shell — renders sidebar + topbar; children fill the content area
    path: '',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./features/dashboard-layout/dashboard-layout.component').then(
        m => m.DashboardLayoutComponent
      ),
    children: [
      {
        path: 'manage-team',
        loadComponent: () =>
          import('./features/manage-team/manage-team.component').then(
            m => m.ManageTeamComponent
          ),
        title: 'Team Management | Praja Disha AI - Org Dashboard'
      },
      {
        path: 'ai-chat',
        loadComponent: () =>
          import('./features/ai-chat/ai-chat.component').then(
            m => m.AiChatComponent
          ),
        title: 'AI Assistant | Praja Disha AI - Org Dashboard'
      },

      {
        path: 'manage-team/add-department',
        loadComponent: () =>
          import(
            './features/manage-team/components/add-edit-department/add-edit-department.component'
          ).then(m => m.AddEditDepartmentComponent),
        title: 'Add Department | Praja Disha AI - Org Dashboard'
      },
      {
        path: 'manage-team/edit-department/:id',
        loadComponent: () =>
          import(
            './features/manage-team/components/add-edit-department/add-edit-department.component'
          ).then(m => m.AddEditDepartmentComponent),
        title: 'Edit Department | Praja Disha AI - Org Dashboard'
      },
      {
        path: 'manage-team/add-officer',
        loadComponent: () =>
          import(
            './features/manage-team/components/add-edit-officer/add-edit-officer.component'
          ).then(m => m.AddEditOfficerComponent),
        title: 'Add Officer | Praja Disha AI - Org Dashboard'
      },
      {
        path: 'manage-team/edit-officer/:id',
        loadComponent: () =>
          import(
            './features/manage-team/components/add-edit-officer/add-edit-officer.component'
          ).then(m => m.AddEditOfficerComponent),
        title: 'Edit Officer | Praja Disha AI - Org Dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            m => m.DashboardComponent
          ),
        title: 'Command Center | Praja Disha AI - Org Dashboard'
      },
      {
        path: 'task-details/:id',
        loadComponent: () =>
          import('./features/task-details/task-details.component').then(
            m => m.TaskDetailsComponent
          ),
        title: 'Task Deep-Dive | Praja Disha AI - Org Dashboard'
      },
      {
        // Default authenticated landing
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  {
    // Catch-all — redirects to login for unauthenticated or unknown paths
    path: '**',
    redirectTo: 'login'
  }
];
