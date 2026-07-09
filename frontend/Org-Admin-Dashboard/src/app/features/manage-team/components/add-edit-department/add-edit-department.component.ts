import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DropdownModule } from 'primeng/dropdown';
import { MANAGE_TEAM_STRINGS } from '../../manage-team.strings';
import { Department } from '../../../../core/models/manage-team/manage-team.domain-models';
import { Officer } from '../../../../core/models/officer.model';
import { DepartmentService } from '../../../../core/services/department.service';
import { OfficerService } from '../../../../core/services/officer.service';
import { OrganizationService } from '../../../../core/services/organization.service';
import { OrgConstituency } from '../../../../core/models/organization.model';
import { environment } from '../../../../../environments/environment';

declare var google: any;


/** UI definition for hierarchy preview tree nodes */
interface HierarchyNode {
  name: string;
  isCurrent: boolean;
  icon: string;
}

@Component({
  selector: 'app-add-edit-department',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DropdownModule,
  ],
  templateUrl: './add-edit-department.component.html',
  styleUrls: ['./add-edit-department.component.scss'],
})
export class AddEditDepartmentComponent implements OnInit {
  readonly strings = MANAGE_TEAM_STRINGS;

  isEditMode = false;
  departmentId: string | null = null;

  // Form Fields
  name = '';
  id = '';
  description = '';
  parentDepartmentId: string | null = null;
  constituencyName = '';
  constituencyGeoJson = '';
  customPromptExtension = '';

  // Options & Selections
  allDepartments: Department[] = [];
  parentDepartmentOptions: { label: string; value: string | null }[] = [];
  allOfficers: Officer[] = [];
  assignedOfficers: Officer[] = [];
  departmentHeads: Officer[] = [];

  // Dropdown / Search State
  officerHeadSearchQuery = '';
  officerAssignSearchQuery = '';
  showHeadDropdown = false;
  showAssignDropdown = false;
  filteredHeads: Officer[] = [];
  filteredAssignees: Officer[] = [];

  // Map Drawing Modal State
  showMapModal = false;
  googleMapsApiKey = environment.googleMapsApiKey;
  map: any;
  drawingManager: any;
  googleMapPolygon: any;
  organizationBoundaryPolygon: any;
  organizationConstituency: OrgConstituency | null = null;
  activeOrgId = '';
  mapError = '';
  mapCoordinates: { lat: number; lng: number }[] = [];

  // Precomputed UI properties to avoid template function calls
  hierarchyPreviewList: HierarchyNode[] = [];
  isFormSubmitted = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private departmentService: DepartmentService,
    private officerService: OfficerService,
    private organizationService: OrganizationService
  ) {}

  ngOnInit(): void {
    this.departmentId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.departmentId;

    // Load active organization data for boundary reference
    this.organizationService.getActiveOrganization().subscribe({
      next: (org) => {
        if (org) {
          this.organizationConstituency = org.constituency;
          this.activeOrgId = org.id;
        }
      },
      error: (err) => console.error('Error fetching organization info:', err),
    });

    // Load static datasets
    this.officerService.getOfficers().subscribe({
      next: (officers) => {
        this.allOfficers = officers;
        this.loadDepartmentData();
      },
      error: (err) => console.error('Error fetching officers:', err),
    });
  }

  private loadDepartmentData(): void {
    this.departmentService.getDepartments().subscribe({
      next: (depts) => {
        this.allDepartments = depts;

        if (this.isEditMode && this.departmentId) {
          this.departmentService.getDepartmentById(this.departmentId).subscribe((dept) => {
            if (dept) {
              this.name = dept.name;
              this.id = dept.id;
              this.parentDepartmentId = dept.parentDepartmentId;
              this.description = dept.roleDescription || '';
              if (dept.orgId) {
                this.activeOrgId = dept.orgId;
              }

              // Retrieve head officer if assigned
              if (dept.headOfficerId) {
                const head = this.allOfficers.find((o) => o.id === dept.headOfficerId);
                if (head) {
                  this.departmentHeads = [head];
                }
              }

              // Retrieve assigned officers based on officer's department links
              this.assignedOfficers = this.allOfficers.filter((o) =>
                o.departmentIds.includes(dept.id)
              );

              // Pre-populate configuration values
              this.constituencyName = dept.constituency?.name || (dept.name + ' Constituency');
              this.constituencyGeoJson = dept.constituency?.coordinates
                ? JSON.stringify(dept.constituency.coordinates, null, 2)
                : JSON.stringify({
                    type: 'Polygon',
                    coordinates: [
                      [
                        [77.5946, 12.9716],
                        [77.6046, 12.9716],
                        [77.6046, 12.9816],
                        [77.5946, 12.9816],
                        [77.5946, 12.9716],
                      ],
                    ],
                  }, null, 2);
              this.customPromptExtension = dept.customPromptExtension || ('Prioritize service requests inside ' + dept.name);
            }
            this.buildParentDropdown();
            this.updateHierarchyPreview();
          });
        } else {
          // Add mode: Suggest a unique ID
          this.id = this.generateNextId(depts);
          this.buildParentDropdown();
          this.updateHierarchyPreview();
        }
      },
      error: (err) => console.error('Error loading departments:', err),
    });
  }

  /**
   * Generates a unique department ID following the DPT-XXX pattern
   */
  private generateNextId(depts: Department[]): string {
    const ids = depts
      .map((d) => {
        const match = d.id.match(/DPT-(\d+)/);
        return match ? parseInt(match[1], 10) : 0;
      })
      .filter((n) => n > 0);

    const max = ids.length > 0 ? Math.max(...ids) : 0;
    const nextNum = max + 1;
    return `DPT-${nextNum.toString().padStart(3, '0')}`;
  }

  /**
   * Builds the parent options and filters out current department and its children in Edit Mode
   */
  private buildParentDropdown(): void {
    const excludedIds = new Set<string>();
    if (this.isEditMode && this.departmentId) {
      excludedIds.add(this.departmentId);
      // Collect children recursively to prevent circular assignments
      let childrenAdded = true;
      while (childrenAdded) {
        childrenAdded = false;
        for (const d of this.allDepartments) {
          if (d.parentDepartmentId && excludedIds.has(d.parentDepartmentId) && !excludedIds.has(d.id)) {
            excludedIds.add(d.id);
            childrenAdded = true;
          }
        }
      }
    }

    const options: { label: string; value: string | null }[] = [
      { label: this.strings.addEditDepartment.fields.parentDeptPlaceholder, value: null },
    ];

    this.allDepartments
      .filter((d) => !excludedIds.has(d.id))
      .forEach((d) => {
        options.push({ label: d.name, value: d.id });
      });

    this.parentDepartmentOptions = options;
  }

  /**
   * Dynamically precomputes the parent hierarchy preview nodes list
   */
  updateHierarchyPreview(): void {
    const list: HierarchyNode[] = [];

    // Traverse parent lineage
    let currentParentId = this.parentDepartmentId;
    const chain: Department[] = [];

    while (currentParentId) {
      const parent = this.allDepartments.find((d) => d.id === currentParentId);
      if (parent) {
        chain.unshift(parent); // Prepend to show root first
        currentParentId = parent.parentDepartmentId;
      } else {
        break;
      }
    }

    // Add Federal Oversight as default root if chain is empty or first item is not already root
    list.push({
      name: this.strings.addEditDepartment.hierarchyPreview.defaultRoot,
      isCurrent: false,
      icon: 'domain',
    });

    chain.forEach((d) => {
      list.push({
        name: d.name,
        isCurrent: false,
        icon: 'account_balance',
      });
    });

    // Add current node representation
    list.push({
      name: this.name.trim() ? this.name : `[${this.isEditMode ? 'Editing Dept' : 'New Dept'}]`,
      isCurrent: true,
      icon: 'folder_special',
    });

    this.hierarchyPreviewList = list;
  }

  /**
   * Handles text input to filter department heads
   */
  onHeadSearchChange(): void {
    const query = this.officerHeadSearchQuery.trim().toLowerCase();
    if (!query) {
      this.filteredHeads = [];
      this.showHeadDropdown = false;
      return;
    }

    // Filter officers matching query and not already selected
    const selectedIds = new Set(this.departmentHeads.map((h) => h.id));
    this.filteredHeads = this.allOfficers.filter(
      (o) =>
        (o.name.toLowerCase().includes(query) || o.officerUserName.toLowerCase().includes(query)) &&
        !selectedIds.has(o.id)
    );
    this.showHeadDropdown = this.filteredHeads.length > 0;
  }

  selectHead(officer: Officer): void {
    // Only allow one department head for now (matching domain model)
    this.departmentHeads = [officer];
    this.officerHeadSearchQuery = '';
    this.showHeadDropdown = false;
  }

  removeHead(officer: Officer): void {
    this.departmentHeads = this.departmentHeads.filter((o) => o.id !== officer.id);
  }

  /**
   * Handles text input to filter assignable officers
   */
  onAssignSearchChange(): void {
    const query = this.officerAssignSearchQuery.trim().toLowerCase();
    if (!query) {
      this.filteredAssignees = [];
      this.showAssignDropdown = false;
      return;
    }

    const assignedIds = new Set(this.assignedOfficers.map((a) => a.id));
    this.filteredAssignees = this.allOfficers.filter(
      (o) =>
        (o.name.toLowerCase().includes(query) || o.officerUserName.toLowerCase().includes(query)) &&
        !assignedIds.has(o.id)
    );
    this.showAssignDropdown = this.filteredAssignees.length > 0;
  }

  selectAssignee(officer: Officer): void {
    this.assignedOfficers = [...this.assignedOfficers, officer];
    this.officerAssignSearchQuery = '';
    this.showAssignDropdown = false;
  }

  removeAssignee(officer: Officer): void {
    this.assignedOfficers = this.assignedOfficers.filter((o) => o.id !== officer.id);
  }

  /**
   * Helper to dynamically load the Google Maps API script inside the component
   */
  private loadGoogleMapsScript(): Promise<void> {
    const key = this.googleMapsApiKey || (window as any).GOOGLE_MAPS_API_KEY || '';
    if (!key) {
      return Promise.reject('No API Key configured.');
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.drawing) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const existingScript = document.getElementById('googleMapsScript');
      if (existingScript) {
        let attempts = 0;
        const interval = setInterval(() => {
          attempts++;
          if (typeof google !== 'undefined' && google.maps && google.maps.drawing) {
            clearInterval(interval);
            resolve();
          }
          if (attempts > 50) {
            clearInterval(interval);
            reject('Timeout loading Google Maps script.');
          }
        }, 100);
        return;
      }

      const script = document.createElement('script');
      script.id = 'googleMapsScript';
      script.src = `https://maps.googleapis.com/maps/api/js?key=${key}&libraries=drawing`;
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = (err) => reject(err);
      document.head.appendChild(script);
    });
  }

  openMapModal(): void {
    this.showMapModal = true;
    this.mapError = '';
    this.mapCoordinates = [];
    this.googleMapPolygon = null;
    this.organizationBoundaryPolygon = null;

    // Load Maps dynamically and initialize
    this.loadGoogleMapsScript()
      .then(() => {
        setTimeout(() => this.initMap(), 50);
      })
      .catch((err) => {
        console.error('Error loading Google Maps:', err);
        this.mapError = this.strings.addEditDepartment.mapModal.apiKeyRequired;
      });
  }

  /**
   * Initializes the Google Map canvas and renders the organization boundary and department polygon
   */
  private initMap(): void {
    const mapEl = document.getElementById('googleMap');
    if (!mapEl) {
      return;
    }

    // Default center around Bangalore if no organization coordinates are loaded
    let center = { lat: 12.9716, lng: 77.5946 };
    let zoom = 12;

    this.map = new google.maps.Map(mapEl, {
      center: center,
      zoom: zoom,
      mapTypeId: 'roadmap',
      disableDefaultUI: false,
      zoomControl: true,
    });

    // 1. Draw organization constituency boundary (read-only overlay)
    const orgCoords = this.getOrgPolygonCoords();
    if (orgCoords.length > 0) {
      const bounds = new google.maps.LatLngBounds();
      orgCoords.forEach((pt) => bounds.extend(pt));

      this.organizationBoundaryPolygon = new google.maps.Polygon({
        paths: orgCoords,
        strokeColor: '#737686', // Slate outline
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#faf8ff',
        fillOpacity: 0.15,
        map: this.map,
        clickable: false,
      });

      // Autofocus the map on the organization area
      this.map.fitBounds(bounds);
    }

    // 2. Parse and render existing department polygon if loaded
    let hasLoadedPolygon = false;
    if (this.constituencyGeoJson.trim()) {
      try {
        const parsed = JSON.parse(this.constituencyGeoJson);
        if (parsed && parsed.type === 'Polygon' && Array.isArray(parsed.coordinates) && parsed.coordinates[0]) {
          const deptCoords = parsed.coordinates[0].map((c: any) => ({
            lng: c[0],
            lat: c[1],
          }));

          // Filter out closing coordinate if repeated to avoid Google Maps rendering anomalies
          if (deptCoords.length > 1) {
            const first = deptCoords[0];
            const last = deptCoords[deptCoords.length - 1];
            if (first.lat === last.lat && first.lng === last.lng) {
              deptCoords.pop();
            }
          }

          if (deptCoords.length > 0) {
            this.googleMapPolygon = new google.maps.Polygon({
              paths: deptCoords,
              strokeColor: '#004ac6', // Primary brand color
              strokeOpacity: 1.0,
              strokeWeight: 3,
              fillColor: '#004ac6',
              fillOpacity: 0.35,
              editable: true,
              draggable: true,
              map: this.map,
            });

            this.setupPolygonListeners(this.googleMapPolygon);
            this.syncCoordinatesFromPolygon();
            hasLoadedPolygon = true;

            // Fit map bounds to the department polygon
            const deptBounds = new google.maps.LatLngBounds();
            deptCoords.forEach((pt: any) => deptBounds.extend(pt));
            this.map.fitBounds(deptBounds);
          }
        }
      } catch (e) {
        console.warn('Could not parse existing department GeoJSON coordinates:', e);
      }
    }

    // 3. Setup Drawing Manager if no department polygon exists
    if (!hasLoadedPolygon) {
      this.drawingManager = new google.maps.drawing.DrawingManager({
        drawingMode: google.maps.drawing.OverlayType.POLYGON,
        drawingControl: true,
        drawingControlOptions: {
          position: google.maps.ControlPosition.TOP_CENTER,
          drawingModes: [google.maps.drawing.OverlayType.POLYGON],
        },
        polygonOptions: {
          strokeColor: '#004ac6',
          strokeOpacity: 1.0,
          strokeWeight: 3,
          fillColor: '#004ac6',
          fillOpacity: 0.35,
          editable: true,
          draggable: true,
        },
      });
      this.drawingManager.setMap(this.map);

      google.maps.event.addListener(this.drawingManager, 'polygoncomplete', (polygon: any) => {
        this.googleMapPolygon = polygon;
        // Stop drawing mode once one polygon is completed
        this.drawingManager.setDrawingMode(null);
        this.drawingManager.setOptions({
          drawingControl: false,
        });
        this.setupPolygonListeners(polygon);
        this.syncCoordinatesFromPolygon();
      });
    }
  }

  private setupPolygonListeners(polygon: any): void {
    const updateHandler = () => this.syncCoordinatesFromPolygon();
    const path = polygon.getPath();
    google.maps.event.addListener(path, 'insert_at', updateHandler);
    google.maps.event.addListener(path, 'remove_at', updateHandler);
    google.maps.event.addListener(path, 'set_at', updateHandler);
    google.maps.event.addListener(polygon, 'dragend', updateHandler);
  }

  private syncCoordinatesFromPolygon(): void {
    if (!this.googleMapPolygon) {
      this.mapCoordinates = [];
      return;
    }
    const path = this.googleMapPolygon.getPath();
    const coords: { lat: number; lng: number }[] = [];
    path.forEach((latLng: any) => {
      coords.push({
        lat: parseFloat(latLng.lat().toFixed(6)),
        lng: parseFloat(latLng.lng().toFixed(6)),
      });
    });
    this.mapCoordinates = coords;
    this.mapError = ''; // Clear error on changes
  }

  /**
   * Ray-Casting algorithm to check point-in-polygon containment
   */
  private isPointInPolygon(point: { lat: number; lng: number }, polygonCoords: { lat: number; lng: number }[]): boolean {
    const x = point.lng;
    const y = point.lat;
    let inside = false;
    for (let i = 0, j = polygonCoords.length - 1; i < polygonCoords.length; j = i++) {
      const xi = polygonCoords[i].lng;
      const yi = polygonCoords[i].lat;
      const xj = polygonCoords[j].lng;
      const yj = polygonCoords[j].lat;

      const intersect = ((yi > y) !== (yj > y)) &&
        (x < ((xj - xi) * (y - yi)) / (yj - yi) + xi);
      if (intersect) {
        inside = !inside;
      }
    }
    return inside;
  }

  private getOrgPolygonCoords(): { lat: number; lng: number }[] {
    if (!this.organizationConstituency || !this.organizationConstituency.coordinates) {
      return [];
    }
    const coords = this.organizationConstituency.coordinates.coordinates[0];
    if (!coords) {
      return [];
    }
    return coords.map((c) => ({
      lng: c[0],
      lat: c[1],
    }));
  }

  /**
   * Enforces that the entire department boundary is within the organization boundaries
   */
  private validatePolygonInsideOrg(): boolean {
    const orgCoords = this.getOrgPolygonCoords();
    if (orgCoords.length === 0) {
      return true; // No boundary to validate against
    }
    if (this.mapCoordinates.length === 0) {
      return true;
    }
    for (const point of this.mapCoordinates) {
      if (!this.isPointInPolygon(point, orgCoords)) {
        return false;
      }
    }
    return true;
  }

  clearPlottedPoints(): void {
    if (this.googleMapPolygon) {
      this.googleMapPolygon.setMap(null);
      this.googleMapPolygon = null;
    }
    if (this.drawingManager) {
      this.drawingManager.setOptions({
        drawingControl: true,
      });
      this.drawingManager.setDrawingMode(google.maps.drawing.OverlayType.POLYGON);
    }
    this.mapCoordinates = [];
    this.mapError = '';
  }

  applyBoundary(): void {
    if (this.mapCoordinates.length < 3) {
      return;
    }

    if (!this.validatePolygonInsideOrg()) {
      this.mapError = this.strings.addEditDepartment.validation.outOfOrgBoundary;
      return;
    }

    // Convert coordinates to GeoJSON format [lng, lat]
    const geoJsonCoords = this.mapCoordinates.map((c) => [c.lng, c.lat]);
    // GeoJSON Polygon coordinates must close by duplicating the first point at the end
    if (geoJsonCoords.length > 0) {
      const first = geoJsonCoords[0];
      const last = geoJsonCoords[geoJsonCoords.length - 1];
      if (first[0] !== last[0] || first[1] !== last[1]) {
        geoJsonCoords.push([first[0], first[1]]);
      }
    }

    const geoJson = {
      type: 'Polygon',
      coordinates: [geoJsonCoords],
    };

    this.constituencyGeoJson = JSON.stringify(geoJson, null, 2);
    this.showMapModal = false;
  }

  closeMapModal(): void {
    this.showMapModal = false;
  }

  /**
   * Save action validating fields and updating/creating
   */
  save(): void {
    this.isFormSubmitted = true;
    this.errorMessage = '';

    if (!this.name.trim()) {
      this.errorMessage = this.strings.addEditDepartment.validation.nameRequired;
      return;
    }

    if (!this.id.trim()) {
      this.errorMessage = this.strings.addEditDepartment.validation.idRequired;
      return;
    }

    const idPattern = /^DPT-\d+$/;
    if (!idPattern.test(this.id)) {
      this.errorMessage = this.strings.addEditDepartment.validation.idPattern;
      return;
    }

    if (this.constituencyGeoJson.trim()) {
      try {
        const parsed = JSON.parse(this.constituencyGeoJson);
        if (parsed.type !== 'Polygon' || !Array.isArray(parsed.coordinates)) {
          this.errorMessage = this.strings.addEditDepartment.validation.invalidGeoJson;
          return;
        }
      } catch (e) {
        this.errorMessage = this.strings.addEditDepartment.validation.invalidGeoJson;
        return;
      }
    }

    // Lookup parent details
    const parentDept = this.allDepartments.find((d) => d.id === this.parentDepartmentId);
    const parentName = parentDept ? parentDept.name : null;
    const parentDepth = parentDept?.depth ?? -1;

    // Head officer details
    const head = this.departmentHeads[0] || null;

    const departmentPayload: Department = {
      id: this.id.trim(),
      orgId: this.activeOrgId,
      name: this.name.trim(),
      parentDepartmentId: this.parentDepartmentId,
      parentDepartmentName: parentName,
      headOfficerId: head ? head.id : null,
      headOfficerName: head ? head.name : null,
      headOfficerAvatarUrl: head ? head.email : null, // Uses email or default initials placeholder in table
      officerCount: this.assignedOfficers.length,
      depth: parentDepth + 1,
      roleDescription: this.description.trim(),
      constituency: {
        name: this.constituencyName.trim() || (this.name.trim() + ' Constituency'),
        coordinates: this.constituencyGeoJson.trim() ? JSON.parse(this.constituencyGeoJson.trim()) : {
          type: 'Polygon',
          coordinates: [
            [
              [77.2090, 28.6139],
              [77.2190, 28.6139],
              [77.2190, 28.6239],
              [77.2090, 28.6239],
              [77.2090, 28.6139],
            ]
          ]
        }
      },
      customPromptExtension: this.customPromptExtension.trim(),
    };

    // Update officers in-memory reference to hold department membership
    this.allOfficers.forEach((o) => {
      const isAssigned = this.assignedOfficers.some((a) => a.id === o.id);
      if (isAssigned) {
        if (!o.departmentIds.includes(this.id)) {
          o.departmentIds.push(this.id);
        }
      } else {
        o.departmentIds = o.departmentIds.filter((id) => id !== this.id);
      }
    });

    if (this.isEditMode && this.departmentId) {
      this.departmentService.updateDepartment(this.departmentId, departmentPayload).subscribe({
        next: () => this.router.navigate(['/manage-team']),
        error: (err) => {
          this.errorMessage = 'Failed to save changes. Please try again.';
          console.error(err);
        },
      });
    } else {
      this.departmentService.createDepartment(departmentPayload).subscribe({
        next: () => this.router.navigate(['/manage-team']),
        error: (err) => {
          this.errorMessage = 'Failed to create department. Please try again.';
          console.error(err);
        },
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/manage-team']);
  }
}
