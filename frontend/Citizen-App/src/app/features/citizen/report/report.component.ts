import { Component, OnInit, OnDestroy, ChangeDetectorRef, AfterViewInit, ViewChild, ElementRef, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, forkJoin, of, Observable } from 'rxjs';
import { switchMap, catchError, map } from 'rxjs/operators';
import { CitizenService } from '../../../core/services/citizen.service';
import { REPORT_STRINGS, SPEECH_LANGUAGES } from './report.strings';
import { environment } from '../../../../environments/environment';

declare var google: any;

@Component({
  selector: 'app-report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.scss']
})
export class ReportComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('locationInput') locationInputEl!: ElementRef<HTMLInputElement>;
  strings = REPORT_STRINGS['en'];
  
  description = '';
  location = '';
  imageUrl = '';
  latitude: number | null = null;
  longitude: number | null = null;

  predictions: any[] = [];
  private autocompleteTimeout: any = null;
  private sessionToken: any = null;

  // Multi-media state
  selectedMedia: { file: File; previewUrl: string; isVideo: boolean }[] = [];
  
  // Voice Recording state
  voiceBlob: Blob | null = null;
  voicePreviewUrl: string | null = null;
  voiceDurationSeconds = 0;
  formattedVoiceDuration = '';
  formattedRecordingTime = '';
  
  // Voice Playback state
  isVoicePlaying = false;
  voicePlaybackPercent = 0;
  formattedVoicePlayTime = '0:00';
  
  // Speech Language selection
  speechLanguages = SPEECH_LANGUAGES;
  selectedSpeechCode = 'en-IN';

  isListening = false;
  isDetectingLocation = false;
  isSubmitting = false;
  
  private subscription = new Subscription();

  // Audio / Recording references
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private recordingStartTime = 0;
  private recordingTimer: any = null;
  private voiceAudio: HTMLAudioElement | null = null;
  private voiceAudioProgressTimer: any = null;
  
  constructor(
    private readonly router: Router,
    private readonly citizenService: CitizenService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.citizenService.currentUser$.subscribe({
        next: (profile) => {
          if (profile) {
            const lang = profile.language || 'en';
            this.strings = REPORT_STRINGS[lang] || REPORT_STRINGS['en'];
            
            // Map preferred profile language to speech code prefix
            const speechMap: Record<string, string> = {
              'en': 'en-IN',
              'hi': 'hi-IN',
              'kn': 'kn-IN',
              'ta': 'ta-IN',
              'te': 'te-IN',
              'bn': 'bn-IN',
              'mr': 'mr-IN',
              'gu': 'gu-IN',
              'ml': 'ml-IN',
              'pa': 'pa-IN',
              'ur': 'ur-PK',
              'as': 'as-IN',
              'or': 'or-IN'
            };
            this.selectedSpeechCode = speechMap[lang] || 'en-IN';
          }
        }
      })
    );
  }

  ngAfterViewInit(): void {
    // Dynamically load Google Maps script
    this.loadGoogleMapsScript().then(() => {
      // Script is loaded, ready to use Places library when user types
    }).catch(err => {
      console.error('Failed to load Google Maps script:', err);
    });
  }

  /**
   * Helper to dynamically load the Google Maps API script inside the component.
   * If the script exists, waits for the google object to populate before resolving.
   */
  private loadGoogleMapsScript(): Promise<void> {
    const key = environment.googleMapsApiKey || (window as any).GOOGLE_MAPS_API_KEY || '';
    if (!key) {
      return Promise.reject('No API Key configured.');
    }

    if (typeof google !== 'undefined' && google.maps && google.maps.places) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const existingScript = document.getElementById('googleMapsScript');
      if (existingScript) {
        let attempts = 0;
        const interval = setInterval(() => {
          attempts++;
          if (typeof google !== 'undefined' && google.maps && google.maps.places) {
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
      script.src = `https://maps.googleapis.com/maps/api/js?key=${key}&libraries=places`;
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = (err) => reject(err);
      document.head.appendChild(script);
    });
  }

  /**
   * Listen for document clicks and close the autocomplete dropdown when clicking outside
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target && !target.closest('.location-input-container')) {
      this.predictions = [];
    }
  }

  /**
   * Create or return the current session token for place autocomplete requests to optimize billing
   */
  private getSessionToken(): any {
    if (!this.sessionToken && typeof google !== 'undefined' && google.maps && google.maps.places) {
      this.sessionToken = new google.maps.places.AutocompleteSessionToken();
    }
    return this.sessionToken;
  }

  /**
   * Clear session token when a suggestion is selected or input is cleared
   */
  private clearSessionToken(): void {
    this.sessionToken = null;
  }

  /**
   * Input event handler with a 300ms debounce
   */
  onLocationInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.location = value;

    if (this.autocompleteTimeout) {
      clearTimeout(this.autocompleteTimeout);
    }

    if (!value.trim()) {
      this.predictions = [];
      this.clearSessionToken();
      return;
    }

    this.autocompleteTimeout = setTimeout(() => {
      this.fetchPredictions(value);
    }, 300);
  }

  /**
   * Query the programmatic Places API (New) for auto-suggestions
   */
  private fetchPredictions(input: string): void {
    if (typeof google === 'undefined' || !google.maps) {
      return;
    }

    google.maps.importLibrary('places').then((library: any) => {
      const { AutocompleteSuggestion } = library;
      const request = {
        input,
        sessionToken: this.getSessionToken(),
        region: 'IN' // Optimize results for India
      };

      AutocompleteSuggestion.fetchAutocompleteSuggestions(request)
        .then(({ suggestions }: any) => {
          this.predictions = suggestions || [];
          this.cdr.detectChanges();
        })
        .catch((err: any) => {
          console.error('Error fetching autocomplete suggestions:', err);
          this.predictions = [];
          this.cdr.detectChanges();
        });
    }).catch((err: any) => {
      console.error('Failed to import places library:', err);
    });
  }

  /**
   * Handle prediction selection, fetch place details, and update form states
   */
  selectPrediction(suggestion: any): void {
    this.predictions = [];
    if (!suggestion || !suggestion.placePrediction) {
      return;
    }

    const placePrediction = suggestion.placePrediction;
    this.location = placePrediction.text.text;

    // Convert prediction to place and fetch fields programmatically
    const place = placePrediction.toPlace();
    place.fetchFields({
      fields: ['formattedAddress', 'location']
    }).then(() => {
      if (place.formattedAddress) {
        this.location = place.formattedAddress;
      }
      if (place.location) {
        this.latitude = place.location.lat();
        this.longitude = place.location.lng();
      }
      this.clearSessionToken();
      this.cdr.detectChanges();
    }).catch((err: any) => {
      console.error('Error fetching place details:', err);
      this.clearSessionToken();
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.cleanupRecording();
    this.cleanupAudioPlayback();

    // Revoke object URLs to avoid memory leaks
    if (this.voicePreviewUrl) {
      URL.revokeObjectURL(this.voicePreviewUrl);
    }
    for (const item of this.selectedMedia) {
      URL.revokeObjectURL(item.previewUrl);
    }
  }

  toggleMic(): void {
    if (this.isListening) {
      this.stopRecording();
      return;
    }

    this.startRecording();
  }

  startRecording(): void {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      console.warn('Audio recording is not supported in this browser.');
      return;
    }

    navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
      this.isListening = true;
      this.audioChunks = [];
      
      // Stop existing playback
      this.cleanupAudioPlayback();

      // Clear any previous voice message
      if (this.voicePreviewUrl) {
        URL.revokeObjectURL(this.voicePreviewUrl);
        this.voicePreviewUrl = null;
        this.voiceBlob = null;
      }

      this.mediaRecorder = new MediaRecorder(stream);
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        this.voiceBlob = new Blob(this.audioChunks, { type: 'audio/wav' });
        this.voicePreviewUrl = URL.createObjectURL(this.voiceBlob);
        
        // Compute duration
        const durationMs = Date.now() - this.recordingStartTime;
        this.voiceDurationSeconds = Math.round(durationMs / 1000);
        this.formattedVoiceDuration = this.formatTime(this.voiceDurationSeconds);
        
        // Release mic resources
        stream.getTracks().forEach(track => track.stop());

        // Manually trigger change detection as onstop callback runs outside Angular zone
        this.cdr.detectChanges();
      };

      this.recordingStartTime = Date.now();
      this.mediaRecorder.start();
      
      // Update visual timer ticking
      this.formattedRecordingTime = '0:00';
      this.recordingTimer = setInterval(() => {
        const elapsed = Math.round((Date.now() - this.recordingStartTime) / 1000);
        this.formattedRecordingTime = this.formatTime(elapsed);
        
        // Manually trigger change detection for real-time timer update
        this.cdr.detectChanges();
      }, 1000);

      // Force change detection to switch to the listening UI card immediately
      this.cdr.detectChanges();

    }).catch(err => {
      console.error('Failed to access microphone:', err);
    });
  }

  stopRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
    }
    
    this.isListening = false;
    if (this.recordingTimer) {
      clearInterval(this.recordingTimer);
      this.recordingTimer = null;
    }
    this.formattedRecordingTime = '';
  }

  cleanupRecording(): void {
    if (this.recordingTimer) {
      clearInterval(this.recordingTimer);
      this.recordingTimer = null;
    }
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
    }
  }

  detectLocation(): void {
    if (this.isDetectingLocation) {
      return;
    }

    this.isDetectingLocation = true;
    
    if (!navigator.geolocation) {
      this.isDetectingLocation = false;
      console.warn('Geolocation is not supported by this browser.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`)
          .then(res => res.json())
          .then(data => {
            this.isDetectingLocation = false;
            if (data && data.display_name) {
              this.location = data.display_name;
            } else {
              this.location = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
            }
            this.latitude = lat;
            this.longitude = lng;
            this.cdr.detectChanges();
          })
          .catch(err => {
            console.error('Geocoding error:', err);
            this.isDetectingLocation = false;
            this.location = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
            this.latitude = lat;
            this.longitude = lng;
            this.cdr.detectChanges();
          });
      },
      (error) => {
        console.error('Geolocation error:', error);
        this.isDetectingLocation = false;
        this.cdr.detectChanges();
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const files = Array.from(input.files);
    const limit = 5;
    const remaining = limit - this.selectedMedia.length;
    const filesToAdd = files.slice(0, remaining);

    for (const file of filesToAdd) {
      const isVideo = file.type.startsWith('video/');
      const previewUrl = URL.createObjectURL(file);
      this.selectedMedia.push({ file, previewUrl, isVideo });
    }

    input.value = '';
  }

  removeMedia(index: number): void {
    const removed = this.selectedMedia.splice(index, 1)[0];
    if (removed && removed.previewUrl) {
      URL.revokeObjectURL(removed.previewUrl);
    }
  }

  toggleVoicePlayback(): void {
    if (!this.voicePreviewUrl) return;

    if (this.isVoicePlaying) {
      this.pauseVoicePlayback();
    } else {
      this.playVoicePlayback();
    }
  }

  playVoicePlayback(): void {
    this.cleanupAudioPlayback();

    this.voiceAudio = new Audio(this.voicePreviewUrl!);
    this.voiceAudio.play().then(() => {
      this.isVoicePlaying = true;
      this.voiceAudioProgressTimer = setInterval(() => {
        if (this.voiceAudio) {
          const currentTime = this.voiceAudio.currentTime;
          const duration = this.voiceAudio.duration || this.voiceDurationSeconds;
          this.voicePlaybackPercent = duration > 0 ? (currentTime / duration) * 100 : 0;
          this.formattedVoicePlayTime = this.formatTime(Math.floor(currentTime));
          
          // Trigger change detection for progress bar updates
          this.cdr.detectChanges();
        }
      }, 100);
      
      if (this.voiceAudio) {
        this.voiceAudio.onended = () => {
          this.resetVoicePlaybackState();
          
          // Trigger change detection on playback end
          this.cdr.detectChanges();
        };
      }
      
      // Initial trigger on play start
      this.cdr.detectChanges();
    }).catch(err => {
      console.error('Failed to play voice message:', err);
    });
  }

  pauseVoicePlayback(): void {
    if (this.voiceAudio) {
      this.voiceAudio.pause();
    }
    this.isVoicePlaying = false;
    if (this.voiceAudioProgressTimer) {
      clearInterval(this.voiceAudioProgressTimer);
      this.voiceAudioProgressTimer = null;
    }
  }

  removeVoiceMessage(): void {
    this.cleanupAudioPlayback();
    if (this.voicePreviewUrl) {
      URL.revokeObjectURL(this.voicePreviewUrl);
      this.voicePreviewUrl = null;
      this.voiceBlob = null;
      this.voiceDurationSeconds = 0;
      this.formattedVoiceDuration = '';
    }
    // Force change detection to update the template immediately
    this.cdr.detectChanges();
  }

  private resetVoicePlaybackState(): void {
    this.isVoicePlaying = false;
    this.voicePlaybackPercent = 0;
    this.formattedVoicePlayTime = '0:00';
    if (this.voiceAudioProgressTimer) {
      clearInterval(this.voiceAudioProgressTimer);
      this.voiceAudioProgressTimer = null;
    }
  }

  private cleanupAudioPlayback(): void {
    this.resetVoicePlaybackState();
    if (this.voiceAudio) {
      this.voiceAudio.pause();
      this.voiceAudio = null;
    }
  }

  formatTime(totalSeconds: number): string {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    const secsStr = secs < 10 ? '0' + secs : secs.toString();
    return `${mins}:${secsStr}`;
  }

  onSubmit(): void {
    const hasDescription = !!this.description.trim();
    const hasVoice = !!this.voiceBlob;
    const hasLocation = !!this.location.trim();
    // Require a location AND at least one of description or voice note
    if (!hasLocation || (!hasDescription && !hasVoice) || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;

    const title = '';

    const uploads$: Observable<any>[] = [];

    // 1. Upload voice message if any
    let voiceUploadIndex = -1;
    if (this.voiceBlob) {
      voiceUploadIndex = uploads$.length;
      uploads$.push(this.citizenService.uploadFile(this.voiceBlob, 'voice_message.wav').pipe(
        catchError(err => {
          console.error('Voice upload failed:', err);
          return of({ url: '' });
        })
      ));
    }

    // 2. Upload images/videos
    const mediaStartIdx = uploads$.length;
    for (let i = 0; i < this.selectedMedia.length; i++) {
      const media = this.selectedMedia[i];
      const ext = media.isVideo ? 'mp4' : 'jpg';
      uploads$.push(this.citizenService.uploadFile(media.file, `media_${i}.${ext}`).pipe(
        catchError(err => {
          console.error(`Media file ${i} upload failed:`, err);
          return of({ url: '' });
        })
      ));
    }

    const performSubmission = (voiceUrl: string, mediaUrls: string[]) => {
      const primaryImageUrl = mediaUrls.length > 0 ? mediaUrls[0] : '';
      const descToSubmit = this.description.trim() || 'Voice report submission';
      
      this.citizenService.submitTicket(
        title,
        descToSubmit,
        this.location,
        this.latitude,
        this.longitude,
        primaryImageUrl,
        voiceUrl,
        this.formattedVoiceDuration,
        mediaUrls,
        this.voiceBlob ? this.selectedSpeechCode : undefined
      ).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/app/track']);
        },
        error: (err) => {
          this.isSubmitting = false;
          console.error('Failed to create task:', err);
        }
      });
    };

    if (uploads$.length > 0) {
      forkJoin(uploads$).subscribe({
        next: (results: any[]) => {
          let voiceUrl = '';
          if (voiceUploadIndex !== -1) {
            voiceUrl = results[voiceUploadIndex]?.url || '';
          }
          const mediaUrls: string[] = [];
          for (let i = mediaStartIdx; i < results.length; i++) {
            const url = results[i]?.url;
            if (url) {
              mediaUrls.push(url);
            }
          }
          performSubmission(voiceUrl, mediaUrls);
        },
        error: (err) => {
          console.error('Parallel uploads encountered error:', err);
          performSubmission('', []);
        }
      });
    } else {
      performSubmission('', []);
    }
  }
}
