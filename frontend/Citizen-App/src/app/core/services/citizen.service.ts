import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, timer } from 'rxjs';
import { map } from 'rxjs/operators';

export interface CitizenProfile {
  username: string;
  name: string;
  email: string;
  phone: string;
  points: number;
  tier: string;
  language: string;
}

export interface ChatComment {
  userName: string;
  initials: string;
  isOfficer: boolean;
  text: string;
  timestamp: string;
  imageUrl?: string;
  department?: string;
}

export interface Ticket {
  id: string;
  category: string;
  title: string;
  description: string;
  date: string;
  status: 'Submitted' | 'AI-Assigned' | 'In Progress' | 'Resolved';
  lastUpdate: string;
  location: string;
  imageUrl?: string;
  comments: ChatComment[];
  rating?: number;
  feedbackComment?: string;
  isCustomCreated?: boolean;
}

export interface TransitPass {
  id: string;
  title: string;
  pointsCost: number;
  expiresAt: string;
  fareType: string;
  qrCodeData: string;
  isActive: boolean;
}

export interface PointActivity {
  id: string;
  title: string;
  source: string;
  date: string;
  points: number;
}

@Injectable({
  providedIn: 'root'
})
export class CitizenService {
  // Current user state
  private readonly currentUserSubject = new BehaviorSubject<CitizenProfile | null>({
    username: 'aisha_patel',
    name: 'Aisha Patel',
    email: 'aisha.patel@example.com',
    phone: '9876543210',
    points: 1250,
    tier: 'Silver Citizen',
    language: 'en'
  });
  currentUser$ = this.currentUserSubject.asObservable();

  // Tickets state
  private readonly ticketsSubject = new BehaviorSubject<Ticket[]>([
    {
      id: 'PD-8821',
      category: 'Infrastructure',
      title: 'Broken Streetlight',
      description: 'The streetlight at the junction of 4th Main and 12th Cross has been flickering and eventually stopped working. Reported 3 days ago.',
      date: 'Oct 22, 2026',
      status: 'Resolved',
      lastUpdate: '3 days ago',
      location: 'Junction of 4th Main and 12th Cross',
      comments: [
        {
          userName: 'Officer Rajesh',
          initials: 'OR',
          isOfficer: true,
          department: 'Maintenance Dept.',
          text: 'Replacement bulb scheduled for tonight. Our team will be on-site after 8:00 PM.',
          timestamp: 'Oct 22, 2026, 02:30 PM'
        },
        {
          userName: 'Me',
          initials: 'AP',
          isOfficer: false,
          text: 'Thank you, Rajesh. Please ensure the light casing is also checked as it looked loose.',
          timestamp: 'Oct 22, 2026, 03:15 PM'
        },
        {
          userName: 'System',
          initials: 'SYS',
          isOfficer: true,
          department: 'Praja Disha AI',
          text: 'Ticket marked as "Resolved" by Officer Rajesh',
          timestamp: 'Oct 22, 2026, 08:50 PM'
        },
        {
          userName: 'Officer Rajesh',
          initials: 'OR',
          isOfficer: true,
          department: 'Maintenance Dept.',
          text: 'Work completed at 8:45 PM. Streetlight is now fully operational. Casing secured.',
          timestamp: 'Oct 22, 2026, 09:00 PM',
          imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCcs93ChFVUtOiLsi9EsEdHQ4Hb1CqjemGt28Kcq2s9hU08wnaxaU2n5lxmU6pUsLWmsCswltCWyZui-xhwDqXivCugsFdxLQcqFcw7jRl7Tr61Tu92g5jeOQrWRlzbx9X2iYv03OPbzNU34BKvICemvYbEkPMfY2REMohL8s210SOXfbvXCDpLpddtINhbstgXetj6eFFhboJgHTaZ2QXwQFu8VhAc6bmNaYg15RBH6Rtgjg88fzBgESz3jvKI84umw4ylhuIAwbs'
        }
      ]
    },
    {
      id: 'PD-7740',
      category: 'Grievance',
      title: 'Pothole on 5th Ave',
      description: 'A large, deep pothole has opened up near the bus stop on 5th Ave. It is extremely hazardous for cyclists and two-wheelers, especially after dark.',
      date: 'Oct 23, 2026',
      status: 'AI-Assigned',
      lastUpdate: '2 hours ago',
      location: '5th Ave, Near Bus Stop',
      comments: [
        {
          userName: 'Praja Disha AI',
          initials: 'AI',
          isOfficer: true,
          department: 'System Router',
          text: 'Ticket classified as "Road Infrastructure" and routed to Ward 150 Road Maintenance team.',
          timestamp: 'Oct 23, 2026, 10:05 AM'
        }
      ]
    },
    {
      id: 'PD-6012',
      category: 'Grievance',
      title: 'Park Bench Graffiti',
      description: 'Graffiti painted on the park benches in the children\'s play area. Need cleaning and repaint.',
      date: 'Oct 15, 2026',
      status: 'Resolved',
      lastUpdate: 'Completed on June 12',
      location: 'Community Park, Block B',
      rating: 5,
      feedbackComment: 'Cleaned up quickly! Children can use the playground happily now.',
      comments: [
        {
          userName: 'Officer Swetha',
          initials: 'OS',
          isOfficer: true,
          department: 'Horticulture Dept.',
          text: 'Graffiti cleaning team dispatched. Benches repainted.',
          timestamp: 'Oct 16, 2026, 11:30 AM'
        }
      ]
    }
  ]);
  tickets$ = this.ticketsSubject.asObservable();

  // Transit passes state
  private readonly passesSubject = new BehaviorSubject<TransitPass[]>([
    {
      id: 'pass-active',
      title: 'Active Transit Pass',
      pointsCost: 200,
      expiresAt: '14:20 PM',
      fareType: 'Single Trip',
      qrCodeData: 'https://praja-disha.gov.in/pass/verify/active',
      isActive: true
    }
  ]);
  passes$ = this.passesSubject.asObservable();

  // Point activities state
  private readonly pointActivitiesSubject = new BehaviorSubject<PointActivity[]>([
    {
      id: 'act-1',
      title: 'Pothole fixed on 5th Ave',
      source: 'Verified by City Council',
      date: 'Oct 24',
      points: 50
    },
    {
      id: 'act-2',
      title: 'Illegal dumping reported',
      source: 'Case resolved',
      date: 'Oct 22',
      points: 120
    },
    {
      id: 'act-3',
      title: 'Community garden cleanup',
      source: 'Event participation',
      date: 'Oct 15',
      points: 200
    }
  ]);
  pointActivities$ = this.pointActivitiesSubject.asObservable();

  constructor() {}

  // Login simulating
  login(identifier: string): Observable<boolean> {
    const user = this.currentUserSubject.value;
    const isEmail = identifier.includes('@');
    const isNew = isEmail ? identifier !== 'aisha.patel@example.com' : identifier !== '9876543210';
    
    const subject = new BehaviorSubject<boolean>(!isNew);
    
    if (isNew) {
      // Setup temporary profile for registration
      this.currentUserSubject.next({
        username: isEmail ? identifier.split('@')[0] : identifier,
        name: '',
        email: isEmail ? identifier : '',
        phone: isEmail ? '' : identifier,
        points: 0,
        tier: 'Bronze Citizen',
        language: 'en'
      });
    } else {
      // Revert to standard mock profile
      this.currentUserSubject.next({
        username: 'aisha_patel',
        name: 'Aisha Patel',
        email: 'aisha.patel@example.com',
        phone: '9876543210',
        points: 1250,
        tier: 'Silver Citizen',
        language: user ? user.language : 'en'
      });
    }
    
    return subject.asObservable();
  }

  // Register simulating
  register(name: string): void {
    const user = this.currentUserSubject.value;
    if (user) {
      this.currentUserSubject.next({
        ...user,
        name: name,
        points: 200, // starting bonus
        tier: 'Bronze Citizen'
      });
    }
  }

  // Create new ticket
  submitTicket(title: string, description: string, location: string, imageUrl?: string): string {
    const randomIdNum = Math.floor(1000 + Math.random() * 9000);
    const newId = `PD-${randomIdNum}`;
    const newTicket: Ticket = {
      id: newId,
      category: 'Grievance',
      title: title || 'Civic Issue',
      description: description || 'No description provided.',
      date: 'Today',
      status: 'Submitted',
      lastUpdate: 'Just now',
      location: location || 'Detected Location',
      imageUrl: imageUrl,
      comments: [],
      isCustomCreated: true
    };

    const currentTickets = this.ticketsSubject.value;
    this.ticketsSubject.next([newTicket, ...currentTickets]);

    // AI Routing automation: transition to 'AI-Assigned' after 3 seconds
    timer(3000).subscribe(() => {
      this.updateTicketStatus(newId, 'AI-Assigned', 'Praja Disha AI', 'AI Router', 'Ticket classified by AI and routed to the corresponding department.');
    });

    return newId;
  }

  // Update ticket status
  private updateTicketStatus(id: string, status: 'Submitted' | 'AI-Assigned' | 'In Progress' | 'Resolved', officerName: string, dept: string, remarks: string): void {
    const currentTickets = this.ticketsSubject.value.map(t => {
      if (t.id === id) {
        const updatedComments = [...t.comments, {
          userName: officerName,
          initials: 'AI',
          isOfficer: true,
          department: dept,
          text: remarks,
          timestamp: 'Just now'
        }];
        return {
          ...t,
          status,
          lastUpdate: 'Just now',
          comments: updatedComments
        };
      }
      return t;
    });
    this.ticketsSubject.next(currentTickets);
  }

  // Rate a resolved ticket and earn points
  submitFeedback(id: string, rating: number, comment: string): void {
    const currentTickets = this.ticketsSubject.value.map(t => {
      if (t.id === id) {
        return {
          ...t,
          rating,
          feedbackComment: comment
        };
      }
      return t;
    });
    this.ticketsSubject.next(currentTickets);

    // Award user points for giving feedback
    const user = this.currentUserSubject.value;
    if (user) {
      const bonusPoints = 50;
      const updatedUser = {
        ...user,
        points: user.points + bonusPoints
      };
      
      // Determine if they tier up
      if (updatedUser.points >= 1500) {
        updatedUser.tier = 'Gold Citizen';
      } else if (updatedUser.points >= 1000) {
        updatedUser.tier = 'Silver Citizen';
      }

      this.currentUserSubject.next(updatedUser);

      // Add to activities list
      const activities = this.pointActivitiesSubject.value;
      const newActivity: PointActivity = {
        id: `act-${Math.random()}`,
        title: `Feedback on Ticket ${id}`,
        source: 'Citizen Review Bonus',
        date: 'Today',
        points: bonusPoints
      };
      this.pointActivitiesSubject.next([newActivity, ...activities]);
    }
  }

  // Reopen a ticket
  reopenTicket(id: string): void {
    const currentTickets = this.ticketsSubject.value.map(t => {
      if (t.id === id) {
        const updatedComments = [...t.comments, {
          userName: 'Me',
          initials: 'AP',
          isOfficer: false,
          text: 'Ticket reopened due to incomplete resolution.',
          timestamp: 'Just now'
        }, {
          userName: 'Officer Rajesh',
          initials: 'OR',
          isOfficer: true,
          department: 'Maintenance Dept.',
          text: 'We have received your reopen request and are scheduling a technician to review the resolution.',
          timestamp: 'Just now'
        }];
        return {
          ...t,
          status: 'In Progress' as const,
          lastUpdate: 'Reopened just now',
          rating: undefined,
          feedbackComment: undefined,
          comments: updatedComments
        };
      }
      return t;
    });
    this.ticketsSubject.next(currentTickets);
  }

  // Update profile settings
  updateLanguage(lang: string): void {
    const user = this.currentUserSubject.value;
    if (user) {
      this.currentUserSubject.next({
        ...user,
        language: lang
      });
    }
  }

  // Redeem a transit pass
  redeemPass(pointsCost: number, title: string): boolean {
    const user = this.currentUserSubject.value;
    if (user && user.points >= pointsCost) {
      // Deduct points
      const updatedUser = {
        ...user,
        points: user.points - pointsCost
      };
      
      // Adjust tier if necessary
      if (updatedUser.points < 1000) {
        updatedUser.tier = 'Bronze Citizen';
      }
      this.currentUserSubject.next(updatedUser);

      // Add pass
      const currentPasses = this.passesSubject.value;
      const expiresAtDate = new Date();
      expiresAtDate.setHours(expiresAtDate.getHours() + 2); // expires in 2 hours
      const expiresStr = expiresAtDate.toTimeString().substring(0, 5);

      const newPass: TransitPass = {
        id: `pass-${Math.random()}`,
        title: title,
        pointsCost: pointsCost,
        expiresAt: `${expiresStr} PM`,
        fareType: 'Single Trip',
        qrCodeData: `https://praja-disha.gov.in/pass/verify/${Math.random()}`,
        isActive: true
      };
      this.passesSubject.next([newPass, ...currentPasses]);

      // Add debit point activity
      const activities = this.pointActivitiesSubject.value;
      const newActivity: PointActivity = {
        id: `act-${Math.random()}`,
        title: `Redeemed ${title}`,
        source: 'Transit rewards store',
        date: 'Today',
        points: -pointsCost
      };
      this.pointActivitiesSubject.next([newActivity, ...activities]);

      return true;
    }
    return false;
  }

  logout(): void {
    // Reset to default on logout
    this.currentUserSubject.next({
      username: 'aisha_patel',
      name: 'Aisha Patel',
      email: 'aisha.patel@example.com',
      phone: '9876543210',
      points: 1250,
      tier: 'Silver Citizen',
      language: 'en'
    });
  }
}
